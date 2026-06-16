#!/usr/bin/env python3
"""
rebuild_tile_index.py — Cellfire server-side tile index rebuilder
Scans the cellfire-db directory, reads every grid_*.json.gz tile,
and regenerates tile_index.json in place.

Run via cron:  0 3 * * * cd /home1/veterap2 && python3 rebuild_tile_index.py
"""
import gzip
import json
import os
import re
import sys
from pathlib import Path

TILES_DIR = Path("/home1/veterap2/public_html/website_9695cd55/files/cellfire-db")
OUTPUT    = TILES_DIR / "tile_index.json"

_TILE_RE = re.compile(r"grid_([pn])([\d.]+)_([pn])([\d.]+)\.json\.gz$")


def parse_coords(name):
    m = _TILE_RE.match(name)
    if not m:
        return None
    lat_sign = 1 if m.group(1) == "p" else -1
    lon_sign = 1 if m.group(3) == "p" else -1
    return lat_sign * float(m.group(2)), lon_sign * float(m.group(4))


def main():
    if not TILES_DIR.exists():
        print(f"ERROR: tiles directory not found: {TILES_DIR}", file=sys.stderr)
        sys.exit(1)

    result = []
    for fp in sorted(TILES_DIR.glob("grid_*.json.gz")):
        coords = parse_coords(fp.name)
        if not coords:
            continue
        lat, lon = coords
        try:
            with gzip.open(fp, "rt", encoding="utf-8") as f:
                records = json.load(f)
        except Exception as e:
            print(f"WARN: skipping {fp.name}: {e}", file=sys.stderr)
            records = []

        carriers: dict = {}
        conf_sum = 0
        for rec in records:
            c = rec.get("carrier") or "Unknown"
            carriers[c] = carriers.get(c, 0) + 1
            conf_sum += rec.get("conf", 0)

        result.append({
            "key":        fp.stem.replace(".json", ""),   # strip .json.gz → key
            "lat":        lat,
            "lon":        lon,
            "cell_count": len(records),
            "carriers":   carriers,
            "avg_conf":   round(conf_sum / len(records)) if records else 0,
        })

    total = sum(t["cell_count"] for t in result)
    index = {"tiles": result, "total_cells": total, "total_tiles": len(result)}
    content = json.dumps(index, separators=(",", ":"))

    OUTPUT.write_text(content, encoding="utf-8")
    print(f"OK: wrote {OUTPUT} — {len(result):,} tiles, {total:,} cells")


if __name__ == "__main__":
    main()
