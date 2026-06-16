#!/usr/bin/env python3
"""
dedup_tiles.py — Deduplicate invalid-PCI records in cellfire tile files.

Records with PCI ≤ 0 (the modem returned 0 or -1 — no valid PCI) that share
the same (TAC, carrier) in a tile are redundant: they all describe the same
unknown cell on the same carrier. This script merges each such group into one
representative record:

  pci            = 0   (canonical "PCI unknown" sentinel)
  samples        = sum of all merged records
  lat / lon      = weighted average by sample count
  conf           = max conf across the group
  possible_bands = union of all bands seen
  arfcn, mnc, source, cellid, range = taken from the highest-conf record

Valid-PCI records (pci > 0) are never modified.
Records with pci ≤ 0 but missing TAC or carrier are left as-is.

Run once as a cleanup pass after importing legacy data, or periodically via
the Scripts tab in the admin tool.
"""

import gzip
import json
import logging
import os
from collections import defaultdict
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("dedup_tiles")

# ── Config ────────────────────────────────────────────────────────────────────

# Absolute path on the server (used when LOCAL_MODE = False)
SERVER_FILES_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"

# Local working directory (used when LOCAL_MODE = True, for testing)
LOCAL_TILES_PATH = "cellfire-tiles"

# False  → run in-place on the server filesystem (normal cron / Scripts-tab use)
# True   → read/write locally (for local testing)
LOCAL_MODE = False

CELLFIRE_FILES_PATH = LOCAL_TILES_PATH if LOCAL_MODE else SERVER_FILES_PATH


# ── Tile I/O ──────────────────────────────────────────────────────────────────

def load_tile(filepath):
    try:
        with gzip.open(filepath, "rt", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None  # None = corrupt/unreadable, distinct from [] = valid empty tile


def save_tile(filepath, records):
    with gzip.open(filepath, "wt", encoding="utf-8") as f:
        json.dump(records, f, separators=(",", ":"))
    log.info(f"  Saved {filepath.name} ({len(records)} records, "
             f"{filepath.stat().st_size} bytes)")


# ── Core dedup logic ──────────────────────────────────────────────────────────

def dedup_tile(records):
    """
    Merge invalid-PCI records (pci <= 0) that share (tac, carrier) into one.

    Returns (new_records_list, number_of_records_removed).
    Valid-PCI records and ungroupable invalid-PCI records pass through unchanged.
    """
    # Drop records with invalid TAC (0xFFFF modem sentinel or overflow ≥ 65535).
    # 65534 and below are valid 16-bit TAC values and are never touched.
    before  = len(records)
    records = [r for r in records if r.get("tac", 0) < 0xFFFF]
    purged  = before - len(records)

    valid   = [r for r in records if (r.get("pci") or 0) > 0]
    invalid = [r for r in records if (r.get("pci") or 0) <= 0]

    if not invalid:
        return records, 0, purged

    # Partition: groupable (real TAC + known carrier) vs pass-through
    groups      = defaultdict(list)   # (tac, carrier) -> [records]
    pass_through = []

    for rec in invalid:
        tac     = rec.get("tac", 0)
        carrier = rec.get("carrier", "")
        if tac and carrier and carrier not in ("Unknown", ""):
            groups[(tac, carrier)].append(rec)
        else:
            pass_through.append(rec)

    merged_out   = []
    total_removed = 0

    for (tac, carrier), group in groups.items():
        if len(group) == 1:
            # Only one record in this group — nothing to merge
            merged_out.append(group[0])
            continue

        # ── Build merged record ───────────────────────────────────────────────
        total_samples = sum(r.get("samples", 1) for r in group)

        # Best record by confidence (source of truth for scalar fields)
        best = max(group, key=lambda r: (r.get("conf", 0), r.get("samples", 0)))

        # Weighted-average location
        total_w = sum(r.get("samples", 1) for r in group)
        avg_lat = sum(r.get("lat", 0.0) * r.get("samples", 1) for r in group) / total_w
        avg_lon = sum(r.get("lon", 0.0) * r.get("samples", 1) for r in group) / total_w

        # Union of all bands ever seen for this (TAC, carrier) group
        all_bands = sorted(set(b for r in group for b in r.get("possible_bands", [])))

        merged_rec = {
            "pci":            0,
            "mnc":            best.get("mnc", ""),
            "carrier":        carrier,
            "lat":            round(avg_lat, 6),
            "lon":            round(avg_lon, 6),
            "cellid":         best.get("cellid", 0),
            "tac":            tac,
            "range":          best.get("range", 0),
            "samples":        total_samples,
            "source":         best.get("source", ""),
            "arfcn":          best.get("arfcn", 0),
            "possible_bands": all_bands,
            "conf":           best.get("conf", 0),
        }
        merged_out.append(merged_rec)

        removed = len(group) - 1
        total_removed += removed
        log.info(f"    tac={tac} {carrier}: {len(group)} records → 1  "
                 f"(removed {removed}, kept {total_samples} samples, "
                 f"conf={merged_rec['conf']})")

    return valid + merged_out + pass_through, total_removed, purged


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    tiles_path = Path(CELLFIRE_FILES_PATH)
    if not tiles_path.exists():
        log.error(f"Tiles directory not found: {tiles_path}")
        return

    tile_files = sorted(tiles_path.glob("grid_*.json.gz"))
    log.info(f"=== dedup_tiles: scanning {len(tile_files)} tiles in {tiles_path} ===")

    tiles_changed    = 0
    records_removed  = 0
    records_purged   = 0
    corrupt_deleted  = 0

    for fp in tile_files:
        records = load_tile(fp)

        if records is None:
            # Zero-byte or unreadable — delete it so it doesn't persist as junk
            try:
                fp.unlink()
                log.info(f"  Deleted corrupt tile: {fp.name}")
                corrupt_deleted += 1
            except Exception as e:
                log.warning(f"  Could not delete {fp.name}: {e}")
            continue

        if not records:
            continue  # valid but empty — leave alone

        new_records, removed, purged = dedup_tile(records)

        if removed > 0 or purged > 0:
            save_tile(fp, new_records)
            tiles_changed   += 1
            records_removed += removed
            records_purged  += purged

    log.info(
        f"=== Done: {tiles_changed} tiles updated, "
        f"{records_removed} duplicate records removed, "
        f"{records_purged} invalid-TAC records purged, "
        f"{corrupt_deleted} corrupt tiles deleted ==="
    )


if __name__ == "__main__":
    main()
