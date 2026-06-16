#!/usr/bin/env python3
"""
import_opencellid.py — OpenCellID CSV → Cellfire tile importer
==============================================================
Reads an OpenCellID CSV (gzipped, no header row) and merges records into
the local Cellfire tile .json.gz files.

OpenCellID column order (no header in file):
  radio, mcc, net, area, cell, unit, lon, lat, range, samples,
  changeable, created, updated, averageSignal

Per-radio mapping to our tile fields:
  LTE  → unit = PCI (0-503),   area = TAC, cell = eNB Cell ID (direct)
  NR   → unit = PCI (0-1007),  area = TAC, cell = NR Cell Identity (direct)
  UMTS → unit = PSC (0-511),   area = LAC, cell = (RNC_ID << 16) | LCID
           *** PSC ≠ LTE PCI — stored separately, skip for tile merge by default
  GSM  → no PCI equivalent — skip

Usage
-----
  # dry run — shows tile stats without writing anything
  python import_opencellid.py --dry-run cell_towers.csv.gz

  # standard PCI-keyed import (non-US data that has real PCI)
  python import_opencellid.py cell_towers.csv.gz --out C:/path/cellfire_db

  # US-only CellID-keyed import (US carriers don't report PCI to OCID)
  # Writes grid_ci_*.json.gz tiles, keyed by (cellid, tac) instead of (pci, tac)
  python import_opencellid.py cell_towers.csv.gz --us-only --ci-mode

  # also import UMTS
  python import_opencellid.py cell_towers.csv.gz --include-umts

  # skip NR (5G), LTE only
  python import_opencellid.py cell_towers.csv.gz --lte-only
"""

import argparse
import csv
import gzip
import json
import math
import sys
from collections import defaultdict
from pathlib import Path

# ── Default output dir ────────────────────────────────────────────────────────
DEFAULT_OUT = Path.home() / "Desktop" / "cellfire_db"

TILE_SIZE = 0.5  # degrees — must match the rest of the codebase

# Confidence for OCID-seeded records (matches SOURCE_BASE_DEFAULT in merge_observations.py)
CONF_OCID = 40

# US MCC values (310-316)
US_MCCS = {"310", "311", "312", "313", "314", "315", "316"}

# ── OpenCellID CSV column names (file has NO header row) ─────────────────────
OCID_COLS = [
    "radio", "mcc", "net", "area", "cell", "unit",
    "lon", "lat", "range", "samples",
    "changeable", "created", "updated", "averageSignal",
]

# ── US MNC → carrier name ─────────────────────────────────────────────────────
# Sources: ITU / FCC / GSMA; MCC omitted here (all US, MCC 310/311/312/313)
MNC_CARRIER: dict[str, str] = {
    # AT&T / FirstNet
    "030": "AT&T", "070": "AT&T", "080": "AT&T", "090": "AT&T",
    "150": "AT&T", "170": "AT&T", "380": "AT&T", "980": "AT&T",
    "410": "AT&T",   # 310-410 main AT&T
    "180": "AT&T",   # 311-180 AT&T
    "190": "AT&T",   # 311-190 AT&T
    "200": "AT&T",   # 311-200 AT&T
    "210": "AT&T",   # 311-210 AT&T
    # FirstNet (AT&T subsidiary)
    "100": "FirstNet/AT&T",  # 313-100
    # T-Mobile (main 310-260 + all sub-band MCIs)
    "160": "T-Mobile", "200": "T-Mobile", "210": "T-Mobile",
    "220": "T-Mobile", "230": "T-Mobile", "240": "T-Mobile",
    "250": "T-Mobile", "260": "T-Mobile", "270": "T-Mobile",
    "280": "T-Mobile", "290": "T-Mobile", "300": "T-Mobile",
    "310": "T-Mobile", "320": "T-Mobile", "330": "T-Mobile",
    "400": "T-Mobile",
    "040": "T-Mobile",   # 311-040
    # Sprint (now T-Mobile)
    "120": "Sprint/T-Mobile",  # 310-120
    "490": "Sprint/T-Mobile",  # 311-490
    # Verizon
    "010": "Verizon", "020": "Verizon", "030": "Verizon",
    "060": "Verizon", "070": "Verizon", "100": "Verizon",
    "110": "Verizon", "120": "Verizon", "130": "Verizon",
    "140": "Verizon", "160": "Verizon", "350": "Verizon",
    "360": "Verizon", "390": "Verizon", "480": "Verizon",
    "450": "Verizon", "510": "Verizon", "530": "Verizon",
    "590": "Verizon", "880": "Verizon", "890": "Verizon",
    "950": "Verizon", "990": "Verizon",
    # US Cellular
    "220": "US Cellular",  # 311-220
    "370": "US Cellular",  # 311-370
    "580": "US Cellular",  # 311-580
    # Dish Wireless
    "020": "Dish Wireless",  # 311-020 (also Dish)
    # C Spire
    "230": "C Spire",        # 311-230
}

# MCC+MNC combined key overrides (higher priority, handles ambiguous MNCs)
MCC_MNC_CARRIER: dict[tuple[str, str], str] = {
    ("311", "480"): "Verizon",
    ("311", "580"): "US Cellular",
    ("311", "370"): "US Cellular",
    ("311", "220"): "US Cellular",
    ("311", "040"): "T-Mobile",
    ("311", "490"): "Sprint/T-Mobile",
    ("311", "320"): "Sprint/T-Mobile",
    ("311", "080"): "AT&T",
    ("311", "450"): "Verizon",
    ("311", "530"): "Verizon",
    ("310", "410"): "AT&T",
    ("310", "260"): "T-Mobile",
    ("310", "480"): "Verizon",
    ("310", "120"): "Sprint/T-Mobile",
    ("313", "100"): "FirstNet/AT&T",
}


def mnc_to_carrier(mcc: str, mnc: str) -> str:
    """Resolve MCC+MNC → human carrier name. Falls back to MNC-only, then 'Unknown'."""
    # Zero-pad MNC to 3 digits for lookup
    mnc_padded = mnc.zfill(3)
    key = (mcc, mnc_padded)
    if key in MCC_MNC_CARRIER:
        return MCC_MNC_CARRIER[key]
    return MNC_CARRIER.get(mnc_padded, "Unknown")


# ── Tile helpers ──────────────────────────────────────────────────────────────

def coord_str(v: float) -> str:
    prefix = "p" if v >= 0 else "n"
    return f"{prefix}{abs(v):.1f}"


def tile_filename(lat: float, lon: float, ci_mode: bool = False) -> str:
    t_lat = math.floor(lat / TILE_SIZE) * TILE_SIZE
    t_lon = math.floor(lon / TILE_SIZE) * TILE_SIZE
    prefix = "grid_ci" if ci_mode else "grid"
    return f"{prefix}_{coord_str(t_lat)}_{coord_str(t_lon)}.json.gz"


def load_tile(fp: Path) -> list:
    if not fp.exists():
        return []
    try:
        with gzip.open(fp, "rt", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"  [WARN] corrupt tile {fp.name}: {e}")
        return []


def save_tile(fp: Path, records: list):
    fp.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(fp, "wt", encoding="utf-8") as f:
        json.dump(records, f, separators=(",", ":"))


def upsert(records: list, new: dict, ci_mode: bool = False) -> tuple[list, bool]:
    """
    Merge new record into records list.
    - ci_mode=False: match key is (pci, tac)
    - ci_mode=True:  match key is (cellid, tac)
    Higher conf wins; higher samples always bumped.
    Returns (updated_records, was_changed).
    """
    new_conf = new.get("conf", 0)

    if ci_mode:
        cellid = new["cellid"]
        tac    = new["tac"]
        for i, rec in enumerate(records):
            if rec.get("cellid") == cellid and rec.get("tac") == tac:
                if new_conf > rec.get("conf", 0):
                    records[i] = {**rec, **new}
                    return records, True
                if new.get("samples", 0) > rec.get("samples", 0):
                    records[i]["samples"] = new["samples"]
                    return records, True
                return records, False
    else:
        pci = new["pci"]
        tac = new["tac"]
        for i, rec in enumerate(records):
            if rec.get("pci") == pci and rec.get("tac") == tac:
                if new_conf > rec.get("conf", 0):
                    records[i] = {**rec, **new}
                    return records, True
                if new.get("samples", 0) > rec.get("samples", 0):
                    records[i]["samples"] = new["samples"]
                    return records, True
                return records, False

    records.append(new)
    return records, True


# ── Main ─────────────────────────────────────────────────────────────────────

def run(csv_path: str, out_dir: Path, dry_run: bool,
        include_umts: bool, lte_only: bool,
        us_only: bool, ci_mode: bool):

    out_dir.mkdir(parents=True, exist_ok=True)
    mode_label = "CellID-keyed (grid_ci_*)" if ci_mode else "PCI-keyed (grid_*)"
    print(f"Output dir : {out_dir}")
    print(f"Input file : {csv_path}")
    print(f"Dry run    : {dry_run}")
    print(f"US only    : {us_only}")
    print(f"Mode       : {mode_label}")
    print()

    # ── Pass 1: read CSV and group records by tile ──────────────────────────
    tile_records: dict[str, list] = defaultdict(list)

    counters = {
        "total": 0, "skipped_bad": 0, "skipped_gps": 0, "skipped_country": 0,
        "lte": 0, "nr": 0, "umts": 0, "gsm": 0, "other": 0,
    }

    opener = gzip.open if csv_path.endswith(".gz") else open

    with opener(csv_path, "rt", encoding="utf-8") as f:
        reader = csv.DictReader(f, fieldnames=OCID_COLS)
        for row in reader:
            counters["total"] += 1
            if counters["total"] % 1_000_000 == 0:
                print(f"  ... {counters['total']:,} rows read, {len(tile_records)} tiles so far")
            try:
                radio   = row["radio"].strip().upper()
                mcc     = row["mcc"].strip()
                mnc     = row["net"].strip()
                lat     = float(row["lat"])
                lon     = float(row["lon"])
                samples = int(row["samples"])
                rng     = int(row["range"])
            except (ValueError, KeyError):
                counters["skipped_bad"] += 1
                continue

            # Filter to US only
            if us_only and mcc not in US_MCCS:
                counters["skipped_country"] += 1
                continue

            # Skip null-island GPS
            if abs(lat) < 0.001 and abs(lon) < 0.001:
                counters["skipped_gps"] += 1
                continue

            carrier = mnc_to_carrier(mcc, mnc)

            # ── LTE ──────────────────────────────────────────────────────────
            if radio == "LTE":
                counters["lte"] += 1
                try:
                    pci    = int(row["unit"])   # Physical Cell ID 0-503 (0 = not recorded)
                    tac    = int(row["area"])   # Tracking Area Code
                    cellid = int(row["cell"])   # eNB Cell ID (direct)
                except ValueError:
                    counters["skipped_bad"] += 1
                    continue

                if ci_mode:
                    # CellID-keyed mode: accept records even without PCI
                    if cellid <= 0 or tac <= 0:
                        counters["skipped_bad"] += 1
                        continue
                    rec = {
                        "cellid":  cellid,
                        "pci":     pci if pci > 0 else None,
                        "mnc":     mnc.zfill(3),
                        "carrier": carrier,
                        "lat":     round(lat, 5),
                        "lon":     round(lon, 5),
                        "tac":     tac,
                        "range":   rng,
                        "samples": samples,
                        "conf":    CONF_OCID,
                        "source":  "ocid",
                        "radio":   "LTE",
                    }
                    tile_records[tile_filename(lat, lon, ci_mode=True)].append(rec)
                else:
                    if pci <= 0:
                        counters.setdefault("lte_no_pci", 0)
                        counters["lte_no_pci"] += 1
                        continue
                    if pci > 503:
                        counters["skipped_bad"] += 1
                        continue
                    rec = {
                        "pci":     pci,
                        "mnc":     mnc.zfill(3),
                        "carrier": carrier,
                        "lat":     round(lat, 5),
                        "lon":     round(lon, 5),
                        "cellid":  cellid,
                        "tac":     tac,
                        "range":   rng,
                        "samples": samples,
                        "conf":    CONF_OCID,
                        "source":  "ocid",
                        "radio":   "LTE",
                    }
                    tile_records[tile_filename(lat, lon)].append(rec)

            # ── NR (5G) ──────────────────────────────────────────────────────
            elif radio == "NR" and not lte_only:
                counters["nr"] += 1
                try:
                    pci    = int(row["unit"])   # NR PCI 0-1007
                    tac    = int(row["area"])   # TAC
                    cellid = int(row["cell"])   # NR Cell Identity
                except ValueError:
                    counters["skipped_bad"] += 1
                    continue

                if ci_mode:
                    if cellid <= 0 or tac <= 0:
                        counters["skipped_bad"] += 1
                        continue
                    rec = {
                        "cellid":  cellid,
                        "pci":     pci if pci > 0 else None,
                        "mnc":     mnc.zfill(3),
                        "carrier": carrier,
                        "lat":     round(lat, 5),
                        "lon":     round(lon, 5),
                        "tac":     tac,
                        "range":   rng,
                        "samples": samples,
                        "conf":    CONF_OCID,
                        "source":  "ocid",
                        "radio":   "NR",
                    }
                    tile_records[tile_filename(lat, lon, ci_mode=True)].append(rec)
                else:
                    if pci <= 0:
                        counters.setdefault("nr_no_pci", 0)
                        counters["nr_no_pci"] += 1
                        continue
                    if pci > 1007:
                        counters["skipped_bad"] += 1
                        continue
                    rec = {
                        "pci":     pci,
                        "mnc":     mnc.zfill(3),
                        "carrier": carrier,
                        "lat":     round(lat, 5),
                        "lon":     round(lon, 5),
                        "cellid":  cellid,
                        "tac":     tac,
                        "range":   rng,
                        "samples": samples,
                        "conf":    CONF_OCID,
                        "source":  "ocid",
                        "radio":   "NR",
                    }
                    tile_records[tile_filename(lat, lon)].append(rec)

            # ── UMTS ─────────────────────────────────────────────────────────
            elif radio == "UMTS" and include_umts:
                counters["umts"] += 1
                try:
                    psc      = int(row["unit"])    # Primary Scrambling Code 0-511
                    lac      = int(row["area"])    # Location Area Code
                    full_cid = int(row["cell"])    # (RNC_ID << 16) | LCID
                    rnc_id   = full_cid >> 16
                    lcid     = full_cid & 0xFFFF
                except ValueError:
                    counters["skipped_bad"] += 1
                    continue

                # Store PSC in pci field so the app can look it up by PSC
                # tac=-1 distinguishes UMTS from LTE in tile queries
                # cellid encodes rnc<<16|lcid so we preserve both
                rec = {
                    "pci":     psc,
                    "mnc":     mnc.zfill(3),
                    "carrier": carrier,
                    "lat":     round(lat, 5),
                    "lon":     round(lon, 5),
                    "cellid":  full_cid,   # full UTRAN Cell ID; use >> 16 / & 0xFFFF to split
                    "rnc":     rnc_id,
                    "lcid":    lcid,
                    "tac":     lac,        # LAC stored in tac field
                    "range":   rng,
                    "samples": samples,
                    "conf":    CONF_OCID,
                    "source":  "ocid",
                    "radio":   "UMTS",
                }
                tile_records[tile_filename(lat, lon)].append(rec)

            elif radio == "UMTS":
                counters["umts"] += 1  # counted but not imported
            elif radio == "GSM":
                counters["gsm"] += 1   # always skip
            else:
                counters["other"] += 1

    imported_radio = counters["lte"]
    if not lte_only:
        imported_radio += counters["nr"]
    if include_umts:
        imported_radio += counters["umts"]

    print(f"CSV stats:")
    print(f"  Total rows read : {counters['total']:>8,}")
    print(f"  Skipped (non-US): {counters.get('skipped_country', 0):>8,}")
    print(f"  LTE             : {counters['lte']:>8,}  (importing)")
    print(f"  NR (5G)         : {counters['nr']:>8,}  {'(importing)' if not lte_only else '(skipped --lte-only)'}")
    print(f"  UMTS            : {counters['umts']:>8,}  {'(importing)' if include_umts else '(skipped — use --include-umts)'}")
    print(f"  GSM             : {counters['gsm']:>8,}  (skipped — no PCI equivalent)")
    if not ci_mode:
        print(f"  LTE (no PCI)    : {counters.get('lte_no_pci', 0):>8,}  (unit=0/-1, skipped in PCI mode)")
        print(f"  NR  (no PCI)    : {counters.get('nr_no_pci',  0):>8,}  (unit=0/-1, skipped in PCI mode)")
    print(f"  Bad/skipped     : {counters['skipped_bad'] + counters['skipped_gps']:>8,}")
    print(f"  Unique tiles    : {len(tile_records):>8,}")
    print()

    if dry_run:
        print("DRY RUN — showing tile breakdown, nothing written:\n")
        for fname in sorted(tile_records):
            recs = tile_records[fname]
            carriers = set(r["carrier"] for r in recs)
            print(f"  {fname:<40}  {len(recs):>5} records  carriers={sorted(carriers)}")
        print(f"\nTotal: {sum(len(v) for v in tile_records.values()):,} records across {len(tile_records)} tiles")
        return

    # ── Pass 2: merge into tile files ────────────────────────────────────────
    print(f"Merging into {len(tile_records)} tiles …\n")

    total_inserted = 0
    total_updated  = 0
    total_unchanged = 0
    tiles_written  = 0

    for fname in sorted(tile_records):
        fp = out_dir / fname
        existing = load_tile(fp)
        before_len = len(existing)

        inserted = 0
        updated  = 0

        for new_rec in tile_records[fname]:
            if ci_mode:
                ci_before  = new_rec.get("cellid")
                tac_before = new_rec.get("tac")
                existed = any(
                    r.get("cellid") == ci_before and r.get("tac") == tac_before
                    for r in existing
                )
            else:
                pci_before = new_rec.get("pci")
                tac_before = new_rec.get("tac")
                existed = any(
                    r.get("pci") == pci_before and r.get("tac") == tac_before
                    for r in existing
                )
            existing, changed = upsert(existing, new_rec, ci_mode=ci_mode)
            if changed:
                if existed:
                    updated += 1
                else:
                    inserted += 1

        after_len = len(existing)
        unchanged = len(tile_records[fname]) - inserted - updated

        total_inserted  += inserted
        total_updated   += updated
        total_unchanged += unchanged

        if inserted > 0 or updated > 0:
            save_tile(fp, existing)
            tiles_written += 1
            print(f"  {fname:<40}  +{inserted} new  ~{updated} updated  "
                  f"={unchanged} unchanged  total={after_len}")

    print()
    print("=" * 60)
    print(f"Done.")
    print(f"  Tiles written   : {tiles_written}")
    print(f"  Records inserted: {total_inserted:,}")
    print(f"  Records updated : {total_updated:,}")
    print(f"  Records unchanged:{total_unchanged:,}")
    print(f"  Output dir      : {out_dir}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Import OpenCellID CSV into Cellfire tile files"
    )
    parser.add_argument("csv", help="OpenCellID CSV or CSV.GZ file path")
    parser.add_argument(
        "--out", default=str(DEFAULT_OUT),
        help=f"Output directory (default: {DEFAULT_OUT})"
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Show what would be imported without writing any files"
    )
    parser.add_argument(
        "--include-umts", action="store_true",
        help="Also import UMTS records (PSC stored in pci field, RNC/LCID preserved)"
    )
    parser.add_argument(
        "--lte-only", action="store_true",
        help="Import LTE only; skip NR (5G) records"
    )
    parser.add_argument(
        "--us-only", action="store_true",
        help="Only import US records (MCC 310-316)"
    )
    parser.add_argument(
        "--ci-mode", action="store_true",
        help="Key tiles by (cellid, tac) instead of (pci, tac). "
             "Writes grid_ci_*.json.gz files. Use with --us-only since US has no PCI data."
    )
    args = parser.parse_args()

    run(
        csv_path=args.csv,
        out_dir=Path(args.out),
        dry_run=args.dry_run,
        include_umts=args.include_umts,
        lte_only=args.lte_only,
        us_only=args.us_only,
        ci_mode=args.ci_mode,
    )
