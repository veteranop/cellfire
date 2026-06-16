#!/usr/bin/env python3
"""
run_rules.py — Re-apply carrier resolution rules to all existing tile records.

Use this after updating band_carrier_lookup.json or band_license_map.json to
propagate new rules to data that was already written to tiles.

Primary resolution chain (runs on every record):
  1. exclusive_band  — spectrum-exclusive bands (B13→Verizon, B14→FirstNet, B71→T-Mobile…)
  2. fcc_band        — FCC state license map: state + band → exactly 1 carrier
  3. mnc_lookup      — PLMN MNC → carrier (well-documented US assignments only)

Unknown-carrier fallback chain (runs ONLY on records that remain carrier=Unknown):
  4. freq_license    — relaxed FCC: picks best-priority carrier when multiple are licensed
                       for this state + band combination (conf 30–42)
  5. pci_shared      — cross-tile PCI index: if PCI N is strongly mapped to carrier Y
                       across the rest of the dataset, infer the same (conf 40–52)
  6. pci_range       — industry PCI heuristic ranges as last resort (conf 22)

Records with source="alpha" or source="plmn" are modem-confirmed and are NEVER
modified regardless of what the rules say.
"""
# Resolution chain:
#   1. exclusive_band  — spectrum-exclusive bands (B13→Verizon, etc.)
#   2. fcc_band        — FCC license map: state + band → licensed carrier(s)
#   3. mnc_lookup      — PLMN MNC → carrier (well-documented US assignments only)
#   4. freq_license    — relaxed FCC best-guess for shared bands
#   5. pci_shared      — cross-tile PCI consensus index
#   6. pci_range       — hardcoded heuristic PCI ranges

import gzip
import json
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("run_rules")

# ── Config ────────────────────────────────────────────────────────────────────

SERVER_FILES_PATH  = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"
SERVER_ASSETS_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"

LOCAL_TILES_PATH   = "cellfire-tiles"
LOCAL_ASSETS_PATH  = "app/src/main/assets"

# False → run in-place on the server (normal use via Scripts tab / cron)
# True  → read/write local files (for testing on dev machine)
LOCAL_MODE = False

CELLFIRE_FILES_PATH = LOCAL_TILES_PATH   if LOCAL_MODE else SERVER_FILES_PATH
ASSETS_PATH         = LOCAL_ASSETS_PATH  if LOCAL_MODE else SERVER_ASSETS_PATH

# Sources that are modem-confirmed — never override these
PROTECTED_SOURCES = {"alpha", "plmn"}

# Minimum conf required before a rule will overwrite the existing carrier.
# Records already at or above this threshold keep their carrier unless
# the resolved carrier is different AND we're more confident.
OVERRIDE_MIN_CONF = 50

# US MNC → carrier name (MCC 310/311 assumed; well-documented assignments only).
# Stored in records as a bare string without leading zeros (e.g. "20", not "020").
MNC_CARRIER_MAP = {
    # T-Mobile (including former Sprint and Shentel spectrum)
    "20":  "T-Mobile", "160": "T-Mobile", "200": "T-Mobile", "210": "T-Mobile",
    "220": "T-Mobile", "230": "T-Mobile", "240": "T-Mobile", "250": "T-Mobile",
    "260": "T-Mobile", "490": "T-Mobile", "530": "T-Mobile", "660": "T-Mobile",
    "800": "T-Mobile",
    "120": "T-Mobile",        # former Sprint
    "588": "T-Mobile",        # former Shentel (acquired 2021)
    "589": "T-Mobile",        # former Shentel (acquired 2021)
    # AT&T
    "70":  "AT&T", "90":  "AT&T", "150": "AT&T", "170": "AT&T",
    "280": "AT&T", "380": "AT&T", "410": "AT&T", "560": "AT&T",
    "670": "AT&T", "680": "AT&T", "780": "AT&T",
    # Verizon
    "4":   "Verizon", "10":  "Verizon", "12":  "Verizon", "13":  "Verizon",
    "350": "Verizon", "480": "Verizon", "590": "Verizon", "820": "Verizon",
    # US Cellular
    "580": "US Cellular",
    # FirstNet (AT&T Band 14)
    "740": "FirstNet/AT&T",
    # Dish Wireless / Boost
    "980": "Dish Wireless",
}


# ── Rule tables ───────────────────────────────────────────────────────────────

def load_exclusive_bands():
    """band_carrier_lookup.json → {band_int: "Carrier Name"}"""
    p = Path(ASSETS_PATH) / "band_carrier_lookup.json"
    with open(p) as f:
        data = json.load(f)
    raw = data.get("carrier_exclusive_bands", {})
    result = {int(k): v for k, v in raw.items()}
    log.info(f"Loaded {len(result)} exclusive band rules: "
             f"{', '.join(f'B{k}→{v}' for k, v in sorted(result.items()))}")
    return result


def load_band_license_map():
    """
    band_license_map.json is {state: {carrier: [bands]}}.
    Invert to {state: {band: [carriers]}} for fast lookup.
    """
    p = Path(ASSETS_PATH) / "band_license_map.json"
    with open(p) as f:
        raw = json.load(f)
    inverted = {}
    for state, carriers in raw.items():
        band_map = {}
        for carrier, bands in carriers.items():
            for b in bands:
                band_map.setdefault(b, []).append(carrier)
        inverted[state] = band_map
    total = sum(len(v) for v in inverted.values())
    log.info(f"Loaded band_license_map: {len(inverted)} states, {total} state/band pairs")
    return inverted


# ── Unknown-carrier helpers ───────────────────────────────────────────────────

# When a state+band has multiple licensed carriers, prefer in this order.
# Reflects national LTE deployment density / market share as a tiebreaker.
_CARRIER_PRIORITY = [
    "T-Mobile", "AT&T", "Verizon", "FirstNet/AT&T", "US Cellular", "Dish Wireless",
]

def _carrier_priority(carrier):
    try:
        return _CARRIER_PRIORITY.index(carrier)
    except ValueError:
        return len(_CARRIER_PRIORITY)


# Industry-standard PCI heuristic ranges (matches app's pciToCarrier).
# PCI is 0-503, reused per-band, so this is always a coarse guess.
_PCI_RANGES = [
    (0,   179, "T-Mobile"),
    (180, 287, "Verizon"),
    (288, 359, "AT&T"),
    (360, 395, "FirstNet/AT&T"),
    (396, 431, "Dish Wireless"),
    (432, 503, "US Cellular"),
]

def _pci_range_carrier(pci):
    """Return carrier name for PCI using standard industry heuristic ranges."""
    if pci < 0:
        return None
    for lo, hi, carrier in _PCI_RANGES:
        if lo <= pci <= hi:
            return carrier
    return None


def build_pci_index(tile_files):
    """
    Two-pass helper: scan every tile and build a {pci: {carrier: weighted_votes}}
    index from high-confidence records (conf >= 65, known carrier).

    Used by rule 5 (pci_shared) to resolve unknowns that share a PCI with a
    confidently-identified cell anywhere else in the dataset.
    """
    index = {}
    for fp in tile_files:
        records = load_tile(fp)
        if not records:
            continue
        for rec in records:
            pci     = rec.get("pci") or -1
            carrier = rec.get("carrier") or ""
            conf    = rec.get("conf") or 0
            samples = max(rec.get("samples") or 1, 1)
            if pci < 0 or not carrier or carrier in ("Unknown", "Regional/Unknown") or conf < 65:
                continue
            index.setdefault(pci, {})
            index[pci][carrier] = index[pci].get(carrier, 0) + conf * samples
    log.info(f"PCI index: {len(index)} distinct PCIs with confident carrier assignments")
    return index


_UNKNOWN_CARRIERS = {"Unknown", "Regional/Unknown", "", None}

def resolve_unknown(rec, pci_index, license_map):
    """
    Best-guess resolution for records that remain carrier=Unknown after the
    primary chain.  Only called on records whose carrier is in _UNKNOWN_CARRIERS.

    Returns (carrier, source_label, conf) or (None, None, None).
    """
    bands = rec.get("possible_bands") or []
    pci   = rec.get("pci") if rec.get("pci") is not None else -1

    # ── Rule 4: Frequency licensing (relaxed FCC) ─────────────────────────────
    # Aggregate all carriers licensed in this state for ANY of this record's
    # bands.  Pick the top-licensed carrier; confidence drops with ambiguity.
    if bands and license_map:
        lat   = rec.get("lat", 0.0)
        lon   = rec.get("lon", 0.0)
        state = lat_lon_to_state(lat, lon)
        if state:
            state_bands = license_map.get(state, {})
            candidate_hits = {}  # carrier → count of bands it's licensed for
            for b in bands:
                for carrier in state_bands.get(b, []):
                    candidate_hits[carrier] = candidate_hits.get(carrier, 0) + 1
            if candidate_hits:
                ranked = sorted(
                    candidate_hits.items(),
                    key=lambda x: (-x[1], _carrier_priority(x[0]))
                )
                best_carrier, _ = ranked[0]
                n = len(candidate_hits)
                if n == 1:
                    conf = 42   # only one candidate — solid guess
                elif n == 2:
                    conf = 38   # narrow field
                elif n <= 4:
                    conf = 30   # plausible
                else:
                    conf = 0    # too ambiguous — skip
                if conf > 0:
                    return best_carrier, "freq_license", conf

    # ── Rule 5: PCI shared with a known carrier ───────────────────────────────
    # If this PCI is consistently mapped to one carrier across the dataset,
    # inherit that assignment.  Confidence scales with dominance.
    if pci >= 0 and pci in pci_index:
        votes = pci_index[pci]
        total = sum(votes.values())
        if total > 0:
            best     = max(votes, key=votes.get)
            dominance = votes[best] / total
            if dominance >= 0.80:
                return best, "pci_shared", 52
            elif dominance >= 0.60:
                return best, "pci_shared", 40

    # ── Rule 6: PCI range (last resort) ──────────────────────────────────────
    carrier = _pci_range_carrier(pci)
    if carrier:
        return carrier, "pci_range", 22

    return None, None, None


# ── Tile I/O ──────────────────────────────────────────────────────────────────

def load_tile(filepath):
    try:
        with gzip.open(filepath, "rt", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None  # corrupt or empty


def save_tile(filepath, records):
    with gzip.open(filepath, "wt", encoding="utf-8") as f:
        json.dump(records, f, separators=(",", ":"))


# ── State from tile filename ──────────────────────────────────────────────────
# Tile filenames: grid_p43.5_n112.0.json.gz → lat 43.5, lon -112.0
# We use a coarse lat/lon → US state lookup to drive the FCC band rule.

# Coarse bounding-box state lookup (lower-48 only, good enough for FCC band map)
_STATE_BOXES = [
    ("AK",  51.0,  71.5, -168.0, -130.0),
    ("AL",  30.0,  35.0,  -88.5,  -84.9),
    ("AR",  33.0,  36.5,  -94.6,  -89.6),
    ("AZ",  31.3,  37.0, -114.8, -109.0),
    ("CA",  32.5,  42.0, -124.5, -114.1),
    ("CO",  37.0,  41.0, -109.1, -102.0),
    ("CT",  40.9,  42.1,  -73.7,  -71.8),
    ("DC",  38.8,  39.0,  -77.1,  -76.9),
    ("DE",  38.4,  39.8,  -75.8,  -75.0),
    ("FL",  24.5,  31.0,  -87.6,  -80.0),
    ("GA",  30.4,  35.0,  -85.6,  -80.8),
    ("HI",  18.9,  22.2, -160.2, -154.8),
    ("IA",  40.4,  43.5,  -96.6,  -90.1),
    ("ID",  42.0,  49.0, -117.2, -111.0),
    ("IL",  36.9,  42.5,  -91.5,  -87.0),
    ("IN",  37.8,  41.8,  -88.1,  -84.8),
    ("KS",  37.0,  40.0,  -102.1,  -94.6),
    ("KY",  36.5,  39.1,  -89.5,  -81.9),
    ("LA",  28.9,  33.0,  -94.0,  -88.8),
    ("MA",  41.2,  42.9,  -73.5,  -69.9),
    ("MD",  37.9,  39.7,  -79.5,  -75.0),
    ("ME",  43.1,  47.5,  -71.1,  -66.9),
    ("MI",  41.7,  48.3,  -90.4,  -82.4),
    ("MN",  43.5,  49.4,  -97.2,  -89.5),
    ("MO",  36.0,  40.6,  -95.8,  -89.1),
    ("MS",  30.2,  35.0,  -91.7,  -88.1),
    ("MT",  44.4,  49.0, -116.0, -104.0),
    ("NC",  33.8,  36.6,  -84.3,  -75.5),
    ("ND",  45.9,  49.0, -104.1,  -96.5),
    ("NE",  40.0,  43.0, -104.1,  -95.3),
    ("NH",  42.7,  45.3,  -72.6,  -70.6),
    ("NJ",  38.9,  41.4,  -75.6,  -73.9),
    ("NM",  31.3,  37.0, -109.1, -103.0),
    ("NV",  35.0,  42.0, -120.0, -114.0),
    ("NY",  40.5,  45.0,  -79.8,  -71.9),
    ("OH",  38.4,  42.3,  -84.8,  -80.5),
    ("OK",  33.6,  37.0,  -103.0,  -94.4),
    ("OR",  42.0,  46.2, -124.6, -116.5),
    ("PA",  39.7,  42.3,  -80.5,  -74.7),
    ("RI",  41.1,  42.0,  -71.9,  -71.1),
    ("SC",  32.0,  35.2,  -83.4,  -78.5),
    ("SD",  42.5,  45.9, -104.1,  -96.4),
    ("TN",  35.0,  36.7,  -90.3,  -81.6),
    ("TX",  25.8,  36.5, -106.7,  -93.5),
    ("UT",  37.0,  42.0, -114.1, -109.0),
    ("VA",  36.5,  39.5,  -83.7,  -75.2),
    ("VT",  42.7,  45.0,  -73.4,  -71.5),
    ("WA",  45.5,  49.0, -124.7, -116.9),
    ("WI",  42.5,  47.1,  -92.9,  -86.8),
    ("WV",  37.2,  40.6,  -82.6,  -77.7),
    ("WY",  41.0,  45.0, -111.1, -104.0),
]

def lat_lon_to_state(lat, lon):
    """Return two-letter state code for a lat/lon, or None if not matched."""
    for abbr, lat_min, lat_max, lon_min, lon_max in _STATE_BOXES:
        if lat_min <= lat <= lat_max and lon_min <= lon <= lon_max:
            return abbr
    return None


# ── Rule application ──────────────────────────────────────────────────────────

def resolve_record(rec, exclusive_map, license_map, mnc_map):
    """
    Return (resolved_carrier, source_label, conf) or (None, None, None) if no rule fires.
    """
    bands = rec.get("possible_bands", [])
    if not bands:
        return None, None, None

    # Rule 1: exclusive band
    for b in bands:
        carrier = exclusive_map.get(b)
        if carrier:
            return carrier, "exclusive_band", 90

    # Rule 2: FCC band license map (state-specific)
    lat = rec.get("lat", 0.0)
    lon = rec.get("lon", 0.0)
    state = lat_lon_to_state(lat, lon)
    if state and license_map:
        state_bands = license_map.get(state, {})
        for b in bands:
            carriers = state_bands.get(b, [])
            if len(carriers) == 1:
                return carriers[0], "fcc_band", 70

    # Rule 3: MNC lookup — PLMN mobile network code uniquely identifies the operator
    mnc = str(rec.get("mnc", "")).lstrip("0") or str(rec.get("mnc", ""))
    if mnc and mnc_map:
        carrier = mnc_map.get(mnc)
        if carrier:
            return carrier, "mnc_lookup", 75

    return None, None, None


def apply_rules_to_tile(records, exclusive_map, license_map, mnc_map, pci_index=None):
    """Apply resolution rules to every record in a tile. Returns (records, change_count)."""
    changes = 0
    for rec in records:
        if rec.get("source") in PROTECTED_SOURCES:
            continue  # modem-confirmed — never touch

        resolved, source, conf = resolve_record(rec, exclusive_map, license_map, mnc_map)

        if resolved is None and rec.get("carrier") in _UNKNOWN_CARRIERS and pci_index is not None:
            # Primary chain found nothing and carrier is still Unknown — try fallback rules.
            resolved, source, conf = resolve_unknown(rec, pci_index, license_map)

        if resolved is None:
            continue

        if rec.get("carrier") == resolved:
            continue  # already correct

        # exclusive_band always wins over any non-modem source — it's a spectrum license fact.
        # All other rules only overwrite low-confidence or Unknown records.
        if source != "exclusive_band":
            if rec.get("conf", 0) >= OVERRIDE_MIN_CONF and rec.get("carrier") not in _UNKNOWN_CARRIERS:
                log.debug(f"  Skipping pci={rec.get('pci')} conf={rec.get('conf')} "
                          f"carrier={rec.get('carrier')} — already resolved with confidence")
                continue

        old_carrier = rec.get("carrier", "Unknown")
        rec["carrier"] = resolved
        rec["source"]  = source
        rec["conf"]    = max(rec.get("conf", 0), conf)
        log.debug(f"  pci={rec.get('pci')} bands={rec.get('possible_bands')}: "
                  f"{old_carrier} → {resolved} [{source}]")
        changes += 1

    return records, changes


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    tiles_path = Path(CELLFIRE_FILES_PATH)
    if not tiles_path.exists():
        log.error(f"Tiles directory not found: {tiles_path}")
        return

    exclusive_map = load_exclusive_bands()
    license_map   = load_band_license_map()
    mnc_map       = MNC_CARRIER_MAP

    tile_files = sorted(tiles_path.glob("grid_*.json.gz"))
    log.info(f"=== run_rules: {len(tile_files)} tiles in {tiles_path} ===")

    # Pass 1 — build cross-tile PCI index for the pci_shared fallback rule.
    # This scans all tiles read-only to collect PCI→carrier votes from
    # high-confidence records, before any writes happen.
    log.info("Pass 1: building PCI index from high-confidence records…")
    pci_index = build_pci_index(tile_files)

    # Pass 2 — apply all resolution rules (primary + unknown fallbacks).
    log.info("Pass 2: applying resolution rules to all tiles…")
    tiles_changed = 0
    total_changes = 0
    source_counts = {}

    for fp in tile_files:
        records = load_tile(fp)
        if records is None or not records:
            continue

        updated, changes = apply_rules_to_tile(
            records, exclusive_map, license_map, mnc_map, pci_index
        )

        if changes > 0:
            # Tally which sources fired for the summary log
            for rec in updated:
                src = rec.get("source", "")
                if src in ("freq_license", "pci_shared", "pci_range"):
                    source_counts[src] = source_counts.get(src, 0) + 1
            save_tile(fp, updated)
            tiles_changed += 1
            total_changes += changes

    log.info(
        f"=== Done: {tiles_changed} tiles updated, "
        f"{total_changes} records re-assigned ==="
    )
    if source_counts:
        log.info(
            "Unknown-carrier fallback breakdown: " +
            ", ".join(f"{src}={n}" for src, n in sorted(source_counts.items()))
        )


if __name__ == "__main__":
    main()
