#!/usr/bin/env python3
"""
seed_from_csv.py — Cellfire drive test CSV → tile injector
===========================================================
Reads one or more Cellfire drive-test CSV exports, extracts:

  1. CONFIRMED cells (Registered=true, TAC>0, carrier != Unknown)
     → injected with conf=100, source="alpha"

  2. CANDIDATE cells (carrier="Unknown" OR low-confidence, TAC=0)
     → injected with carrier="Unknown", conf=0, source="" so the
       neighbour-inference engine in merge_observations.py can score
       them on the next run.

After injection, runs infer_neighbor_carriers() on every affected tile
so PCI 471 (B66 EARFCN 66786, 67 observations) and other unknowns can
be scored immediately against the confirmed T-Mobile anchors in the
same tile.

Usage
-----
  # dry run — shows what would be changed without writing anything
  python3 seed_from_csv.py --dry-run path/to/CellFire_ALL_*.csv

  # live run — updates tiles on the server
  python3 seed_from_csv.py path/to/CellFire_ALL_*.csv

  # only inject confirmed cells, skip unknown candidates
  python3 seed_from_csv.py --confirmed-only path/to/CellFire_ALL_*.csv

Requires merge_observations.py to be in the same directory (imports
its tile helpers: load_tile, save_tile, infer_neighbor_carriers,
tile_filename, coord_str, CELLFIRE_FILES_PATH, TILE_SIZE).
"""

import argparse
import csv
import gzip
import json
import logging
import math
import os
import sys
import urllib.request
from collections import defaultdict
from pathlib import Path

# ── import shared helpers from merge_observations ────────────────────────────
sys.path.insert(0, str(Path(__file__).parent))
from merge_observations import (
    load_tile, save_tile, infer_neighbor_carriers, backfill_conf,
    tile_filename, coord_str,
    CELLFIRE_FILES_PATH, CELLFIRE_BASE_URL, TILE_SIZE,
    CPANEL_HOST, CPANEL_USER, CPANEL_TOKEN, CPANEL_UPLOAD_DIR, LOCAL_MODE,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("seed_from_csv")

# Minimum observations for a candidate cell to be worth injecting
MIN_OBS_CANDIDATE = 3   # ignore cells seen < 3 times (likely noise)
MIN_OBS_CONFIRMED = 1   # all confirmed serving cells injected regardless of count


# ── MNC table ────────────────────────────────────────────────────────────────
CARRIER_MNC = {
    "T-Mobile":   "260",
    "AT&T":       "410",
    "Verizon":    "004",
    "US Cellular":"220",
    "UScellular": "220",
    "FirstNet":   "410",   # FirstNet runs on AT&T spectrum
    "Dish":       "020",
    "Dish Wireless": "020",
}

def carrier_to_mnc(carrier: str) -> str:
    return CARRIER_MNC.get(carrier, "000")


# ── CSV parsing ───────────────────────────────────────────────────────────────

def parse_csvs(csv_paths: list[str]) -> dict:
    """
    Parse one or more Cellfire CSV exports.
    Returns:
      {
        "confirmed": { (pci, tac): {"carrier", "earfcn", "band", "samples",
                                     "lats", "lons", "mnc"} },
        "candidates": { (pci, earfcn): {"carrier"="Unknown", "band", "samples",
                                         "lats", "lons"} }
      }
    """
    confirmed  = {}   # (pci, tac)  → cell info
    candidates = {}   # (pci, earfcn) → cell info  (TAC unavailable for neighbors)

    skipped_gps = 0
    total_rows  = 0

    for path in csv_paths:
        log.info(f"Parsing {path} …")
        with open(path, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                total_rows += 1
                try:
                    lat  = float(row["Latitude"])
                    lon  = float(row["Longitude"])
                    pci  = int(row["PCI"])
                    tac  = int(row["TAC"])
                    earfcn  = int(row["EARFCN"])
                    carrier = row["Carrier"].strip().strip('"')
                    band    = row["Band"].strip().strip('"')
                    registered = row["Registered"].strip().lower() == "true"
                except (KeyError, ValueError):
                    continue

                # Skip broken GPS (null-island)
                if abs(lat) < 0.1 and abs(lon) < 0.1:
                    skipped_gps += 1
                    continue

                if registered and tac > 0 and carrier not in ("Unknown", "", "null"):
                    # ── Confirmed serving cell ─────────────────────────────────
                    key = (pci, tac)
                    if key not in confirmed:
                        confirmed[key] = {
                            "carrier": carrier,
                            "earfcn":  earfcn,
                            "band":    band,
                            "mnc":     carrier_to_mnc(carrier),
                            "samples": 0,
                            "lats":    [],
                            "lons":    [],
                        }
                    info = confirmed[key]
                    info["samples"] += 1
                    info["lats"].append(lat)
                    info["lons"].append(lon)

                elif carrier in ("Unknown", "", "null") and tac == 0:
                    # ── Unknown-carrier neighbour cell ─────────────────────────
                    key = (pci, earfcn)
                    if key not in candidates:
                        candidates[key] = {
                            "carrier": "Unknown",
                            "band":    band,
                            "samples": 0,
                            "lats":    [],
                            "lons":    [],
                        }
                    info = candidates[key]
                    info["samples"] += 1
                    info["lats"].append(lat)
                    info["lons"].append(lon)

    log.info(f"Parsed {total_rows} rows: "
             f"{len(confirmed)} confirmed cells, "
             f"{len(candidates)} unknown-carrier candidates "
             f"({skipped_gps} rows skipped for GPS=0,0)")
    return {"confirmed": confirmed, "candidates": candidates}


def centroid(lats: list, lons: list) -> tuple[float, float]:
    return sum(lats) / len(lats), sum(lons) / len(lons)


# ── tile key ─────────────────────────────────────────────────────────────────

def tile_key(lat: float, lon: float) -> str:
    t_lat = math.floor(lat / TILE_SIZE) * TILE_SIZE
    t_lon = math.floor(lon / TILE_SIZE) * TILE_SIZE
    return f"grid_{coord_str(t_lat)}_{coord_str(t_lon)}.json.gz"


# ── record helpers ────────────────────────────────────────────────────────────

def make_confirmed_record(pci, tac, info: dict) -> dict:
    lat, lon = centroid(info["lats"], info["lons"])
    return {
        "pci":     pci,
        "mnc":     info["mnc"],
        "carrier": info["carrier"],
        "lat":     round(lat, 5),
        "lon":     round(lon, 5),
        "cellid":  0,
        "tac":     tac,
        "range":   500,
        "samples": info["samples"],
        "conf":    100,
        "source":  "alpha",
    }


def make_candidate_record(pci, earfcn, info: dict) -> dict:
    lat, lon = centroid(info["lats"], info["lons"])
    return {
        "pci":     pci,
        "mnc":     "000",
        "carrier": "Unknown",
        "lat":     round(lat, 5),
        "lon":     round(lon, 5),
        "cellid":  0,
        "tac":     0,
        "range":   500,
        "samples": info["samples"],
        "conf":    0,
        "source":  "",
    }


# ── merge into tile ───────────────────────────────────────────────────────────

def upsert_record(records: list, new_rec: dict) -> tuple[list, bool]:
    """
    Insert or update a record in the tile list.
    Match key: (pci, tac) for confirmed, (pci, tac=0) for candidates.
    Returns (updated_records, was_changed).
    """
    pci = new_rec["pci"]
    tac = new_rec["tac"]
    new_conf = new_rec.get("conf", 0)

    for i, rec in enumerate(records):
        if rec.get("pci") == pci and rec.get("tac") == tac:
            existing_conf = rec.get("conf", 0)
            if new_conf > existing_conf:
                records[i] = {**rec, **new_rec}   # merge, new wins on conflicts
                return records, True
            return records, False  # existing is better or equal

    records.append(new_rec)
    return records, True


# ── cPanel upload (same logic as merge_observations.py) ──────────────────────

def upload_tile(filepath: Path):
    if LOCAL_MODE:
        log.info(f"  [LOCAL_MODE] would upload {filepath.name}")
        return
    import ssl, base64, urllib.parse
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    gz_bytes = filepath.read_bytes()
    # multipart upload
    boundary = "----CellfireBoundary"
    rel_path = str(CPANEL_UPLOAD_DIR).lstrip("/") + "/" + filepath.name
    body_parts = [
        f"--{boundary}",
        f'Content-Disposition: form-data; name="dir"',
        "",
        str(CPANEL_UPLOAD_DIR),
        f"--{boundary}",
        f'Content-Disposition: form-data; name="file-1"; filename="{filepath.name}"',
        "Content-Type: application/octet-stream",
        "",
    ]
    body_prefix = "\r\n".join(body_parts).encode() + b"\r\n"
    body_suffix = f"\r\n--{boundary}--\r\n".encode()
    body = body_prefix + gz_bytes + body_suffix

    req = urllib.request.Request(
        f"https://{CPANEL_HOST}/execute/Fileman/upload_files",
        data=body,
        method="POST",
        headers={
            "Authorization": f"cpanel {CPANEL_USER}:{CPANEL_TOKEN}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
            result = json.loads(resp.read())
            if result.get("status") == 1:
                log.info(f"  ↑ Uploaded {filepath.name}")
            else:
                log.warning(f"  Upload response: {result}")
    except Exception as e:
        log.warning(f"  Upload failed for {filepath.name}: {e}")


# ── main ──────────────────────────────────────────────────────────────────────

def run(csv_paths: list[str], dry_run: bool, confirmed_only: bool):
    parsed = parse_csvs(csv_paths)
    confirmed  = parsed["confirmed"]
    candidates = parsed["candidates"]

    tiles_path = Path(CELLFIRE_FILES_PATH)

    # Group records by target tile
    tile_confirmed:  dict[str, list] = defaultdict(list)
    tile_candidates: dict[str, list] = defaultdict(list)

    for (pci, tac), info in confirmed.items():
        if info["samples"] < MIN_OBS_CONFIRMED:
            continue
        lat, lon = centroid(info["lats"], info["lons"])
        key = tile_key(lat, lon)
        tile_confirmed[key].append(make_confirmed_record(pci, tac, info))

    if not confirmed_only:
        for (pci, earfcn), info in candidates.items():
            if info["samples"] < MIN_OBS_CANDIDATE:
                continue
            lat, lon = centroid(info["lats"], info["lons"])
            key = tile_key(lat, lon)
            tile_candidates[key].append(make_candidate_record(pci, earfcn, info))

    all_tile_keys = set(tile_confirmed) | set(tile_candidates)
    log.info(f"\nWill touch {len(all_tile_keys)} tiles")

    tiles_changed = 0
    for tk in sorted(all_tile_keys):
        fp = tiles_path / tk
        records = load_tile(fp)
        before  = json.dumps(records, sort_keys=True)

        # Inject confirmed records first (highest quality)
        for rec in tile_confirmed.get(tk, []):
            records, changed = upsert_record(records, rec)
            if changed:
                log.info(f"  [{tk}] +confirmed pci={rec['pci']} tac={rec['tac']} "
                         f"carrier={rec['carrier']} samples={rec['samples']}")

        # Inject Unknown candidates
        injected_candidates = 0
        for rec in tile_candidates.get(tk, []):
            records, changed = upsert_record(records, rec)
            if changed:
                injected_candidates += 1
        if injected_candidates:
            log.info(f"  [{tk}] +{injected_candidates} unknown candidates")

        # Re-run inference so new candidates get scored against anchors immediately
        records = backfill_conf(records)
        records = infer_neighbor_carriers(records)

        after = json.dumps(records, sort_keys=True)
        if after == before:
            log.info(f"  [{tk}] no net change after inference")
            continue

        tiles_changed += 1
        if not dry_run:
            save_tile(fp, records)
            upload_tile(fp)
        else:
            log.info(f"  [{tk}] [DRY RUN] would save+upload ({len(records)} records)")

    log.info(f"\nDone — {tiles_changed}/{len(all_tile_keys)} tiles changed"
             + (" [DRY RUN — nothing written]" if dry_run else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed Cellfire tiles from drive-test CSV exports")
    parser.add_argument("csvs", nargs="+", metavar="CSV", help="Cellfire export CSV files")
    parser.add_argument("--dry-run", action="store_true",
                        help="Show what would change without writing anything")
    parser.add_argument("--confirmed-only", action="store_true",
                        help="Only inject confirmed (Registered=true) cells; skip Unknown candidates")
    args = parser.parse_args()

    run(args.csvs, dry_run=args.dry_run, confirmed_only=args.confirmed_only)
