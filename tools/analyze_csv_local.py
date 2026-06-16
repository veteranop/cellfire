#!/usr/bin/env python3
"""
Local CSV analysis — mirrors what seed_from_csv.py would inject.
No server imports needed; tile logic is replicated inline.
"""

import csv
import math
from collections import defaultdict

CSV_PATH = r"C:\Users\markd\Downloads\CellFire_ALL_1774207762125.csv"
TILE_SIZE = 0.5
MIN_OBS_CANDIDATE = 3
MIN_OBS_CONFIRMED = 1

CARRIER_MNC = {
    "T-Mobile":   "260",
    "AT&T":       "410",
    "Verizon":    "004",
    "US Cellular":"220",
    "UScellular": "220",
    "FirstNet":   "410",
    "Dish":       "020",
    "Dish Wireless": "020",
}

def carrier_to_mnc(carrier):
    return CARRIER_MNC.get(carrier, "000")

def coord_str(v):
    sign = "p" if v >= 0 else "n"
    return f"{sign}{abs(v):.1f}"

def tile_key(lat, lon):
    t_lat = math.floor(lat / TILE_SIZE) * TILE_SIZE
    t_lon = math.floor(lon / TILE_SIZE) * TILE_SIZE
    return f"grid_{coord_str(t_lat)}_{coord_str(t_lon)}.json.gz"

def centroid(lats, lons):
    return sum(lats)/len(lats), sum(lons)/len(lons)

# ── Parse CSV ─────────────────────────────────────────────────────────────────

confirmed  = {}   # (pci, tac)   -> info
candidates = {}   # (pci, earfcn) -> info
total_rows = 0
skipped_gps = 0

with open(CSV_PATH, newline="", encoding="utf-8") as f:
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

        if abs(lat) < 0.1 and abs(lon) < 0.1:
            skipped_gps += 1
            continue

        if registered and tac > 0 and carrier not in ("Unknown", "", "null"):
            key = (pci, tac)
            if key not in confirmed:
                confirmed[key] = {
                    "carrier": carrier, "earfcn": earfcn, "band": band,
                    "mnc": carrier_to_mnc(carrier), "samples": 0,
                    "lats": [], "lons": []
                }
            info = confirmed[key]
            info["samples"] += 1
            info["lats"].append(lat)
            info["lons"].append(lon)
        elif carrier in ("Unknown", "", "null") and tac == 0:
            key = (pci, earfcn)
            if key not in candidates:
                candidates[key] = {
                    "carrier": "Unknown", "band": band, "samples": 0,
                    "lats": [], "lons": []
                }
            info = candidates[key]
            info["samples"] += 1
            info["lats"].append(lat)
            info["lons"].append(lon)

print(f"Parsed {total_rows} rows — {len(confirmed)} confirmed cells, "
      f"{len(candidates)} unique unknown-carrier (pci,earfcn) pairs "
      f"({skipped_gps} rows skipped GPS=0,0)\n")

# ── Section 1: Confirmed cells ────────────────────────────────────────────────

print("=" * 70)
print("SECTION 1 — CONFIRMED CELLS (Registered=true, TAC>0, carrier!=Unknown)")
print("=" * 70)

# Group by tile
tile_confirmed = defaultdict(list)
for (pci, tac), info in confirmed.items():
    if info["samples"] < MIN_OBS_CONFIRMED:
        continue
    lat, lon = centroid(info["lats"], info["lons"])
    tk = tile_key(lat, lon)
    tile_confirmed[tk].append({
        "pci": pci, "tac": tac,
        "carrier": info["carrier"], "mnc": info["mnc"],
        "band": info["band"], "earfcn": info["earfcn"],
        "samples": info["samples"],
        "lat": round(lat, 5), "lon": round(lon, 5),
        "tile": tk,
    })

for tk in sorted(tile_confirmed.keys()):
    recs = sorted(tile_confirmed[tk], key=lambda r: (-r["samples"], r["pci"]))
    print(f"\nTile: {tk}  ({len(recs)} confirmed records)")
    print(f"  {'PCI':>4}  {'TAC':>6}  {'Band':>5}  {'EARFCN':>7}  {'Samples':>7}  "
          f"{'Carrier':<14}  {'MNC':>3}  {'Lat':>10}  {'Lon':>11}")
    print(f"  {'-'*4}  {'-'*6}  {'-'*5}  {'-'*7}  {'-'*7}  {'-'*14}  "
          f"{'-'*3}  {'-'*10}  {'-'*11}")
    for r in recs:
        print(f"  {r['pci']:>4}  {r['tac']:>6}  {r['band']:>5}  {r['earfcn']:>7}  "
              f"{r['samples']:>7}  {r['carrier']:<14}  {r['mnc']:>3}  "
              f"{r['lat']:>10.5f}  {r['lon']:>11.5f}")

# ── Section 2: Unknown-carrier candidates ────────────────────────────────────

print("\n\n" + "=" * 70)
print("SECTION 2 — UNKNOWN-CARRIER CANDIDATES (carrier=Unknown, TAC=0, >=3 obs)")
print("=" * 70)

tile_candidates = defaultdict(list)
for (pci, earfcn), info in candidates.items():
    if info["samples"] < MIN_OBS_CANDIDATE:
        continue
    lat, lon = centroid(info["lats"], info["lons"])
    tk = tile_key(lat, lon)
    tile_candidates[tk].append({
        "pci": pci, "earfcn": earfcn, "band": info["band"],
        "samples": info["samples"],
        "lat": round(lat, 5), "lon": round(lon, 5),
        "tile": tk,
    })

for tk in sorted(tile_candidates.keys()):
    recs = sorted(tile_candidates[tk], key=lambda r: (-r["samples"], r["pci"]))
    print(f"\nTile: {tk}  ({len(recs)} candidates)")
    print(f"  {'PCI':>4}  {'EARFCN':>7}  {'Band':>5}  {'Samples':>7}  "
          f"{'Lat':>10}  {'Lon':>11}")
    print(f"  {'-'*4}  {'-'*7}  {'-'*5}  {'-'*7}  {'-'*10}  {'-'*11}")
    for r in recs:
        print(f"  {r['pci']:>4}  {r['earfcn']:>7}  {r['band']:>5}  {r['samples']:>7}  "
              f"{r['lat']:>10.5f}  {r['lon']:>11.5f}")

# ── Section 3: Anchor scoring — which confirmed cells share a tile ─────────────

print("\n\n" + "=" * 70)
print("SECTION 3 — ANCHOR SCORING: confirmed anchors co-located with each candidate")
print("(These are the cells infer_neighbor_carriers() will use to score unknowns)")
print("=" * 70)

all_tile_keys = set(tile_confirmed) | set(tile_candidates)

for tk in sorted(all_tile_keys):
    cands = tile_candidates.get(tk, [])
    anchors = tile_confirmed.get(tk, [])
    if not cands:
        continue

    cands_sorted = sorted(cands, key=lambda r: (-r["samples"], r["pci"]))
    anchors_sorted = sorted(anchors, key=lambda r: (-r["samples"], r["pci"]))

    print(f"\nTile: {tk}")
    print(f"  Confirmed anchors ({len(anchors)}):")
    if anchors_sorted:
        for a in anchors_sorted:
            print(f"    PCI {a['pci']:>4}  TAC {a['tac']:>6}  {a['band']:>5} "
                  f" EARFCN {a['earfcn']:>7}  {a['carrier']:<14} x{a['samples']}")
    else:
        print("    (none — no confirmed anchors in this tile)")

    print(f"  Unknown candidates ({len(cands_sorted)}):")
    for c in cands_sorted:
        print(f"    PCI {c['pci']:>4}  {c['band']:>5}  EARFCN {c['earfcn']:>7}  "
              f"x{c['samples']}  centroid=({c['lat']:.5f}, {c['lon']:.5f})")

# ── Summary ───────────────────────────────────────────────────────────────────

eligible_candidates = sum(
    1 for info in candidates.values() if info["samples"] >= MIN_OBS_CANDIDATE
)

print("\n\n" + "=" * 70)
print("SUMMARY")
print("=" * 70)
print(f"  Total CSV rows:            {total_rows}")
print(f"  Skipped (GPS 0,0):         {skipped_gps}")
print(f"  Confirmed cells (unique):  {len(confirmed)}")
print(f"  Tiles with confirmed:      {len(tile_confirmed)}")
print(f"  Unknown candidates total:  {len(candidates)}")
print(f"  Candidates >=3 obs:        {eligible_candidates}")
print(f"  Tiles with candidates:     {len(tile_candidates)}")
print(f"  Total tiles to touch:      {len(all_tile_keys)}")
