#!/usr/bin/env python3
"""
Cellfire DB Merge Script
========================
Reads crowd-sourced observations from Firebase Realtime Database and merges
them into the cellfire-db tile .json.gz files on cellfire.io.

Confidence scoring (stored as "conf" field, 0-100 on every tile record):
    100  alpha / plmn — registered device, modem-confirmed carrier
     90  fcc_band     — exclusive band, only one carrier possible
     70  db           — confirmed by crowd but source unknown / older entry
     20  pci_range    — hardcoded bucket guess, very low trust
     40  (seed)       — OCID-seeded with no source info

  Modifiers applied on top of base score:
    +0..+15  sample count boost  (log2 scale, caps at 20 samples)
    -20      carrier conflict    (new alpha/plmn disagrees with stored carrier)
    -10      PCI collision       (same PCI seen with 2+ carriers in this tile)
    Score is always clamped 0–100.

Requirements:
    pip install firebase-admin requests

Setup:
    1. Download your Firebase service account key from:
       Firebase Console → Project Settings → Service Accounts → Generate new private key
    2. Save it as serviceAccountKey.json in the same directory as this script
    3. Set CELLFIRE_FILES_PATH to a local working directory for tile files
    4. Run: python3 merge_observations.py

Run continuously while drive-testing:
    python3 merge_observations.py --loop 30    (re-runs every 30 seconds)
"""

import json
import gzip
import os
import math
import re
import logging
import time
import argparse
import urllib.request
from collections import defaultdict
from pathlib import Path
from datetime import datetime

import urllib3
import firebase_admin
import requests
from firebase_admin import credentials, db as firebase_db

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ── Config ──────────────────────────────────────────────────────────────────

SERVICE_ACCOUNT_KEY = "serviceAccountKey.json"
FIREBASE_DATABASE_URL = "https://veteranopcom-default-rtdb.firebaseio.com"

# LOCAL_MODE = True  → tiles saved to LOCAL_TILES_PATH, no upload to server
# LOCAL_MODE = False → tiles uploaded to server via cPanel after merge
LOCAL_MODE = False

# Local working directory for tiles (used in LOCAL_MODE and as a download cache)
LOCAL_TILES_PATH = "cellfire-tiles"

# Absolute path on the server (used when running ON the server, LOCAL_MODE=False)
SERVER_FILES_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"

# Resolved at runtime: local dir when LOCAL_MODE, server path otherwise
CELLFIRE_FILES_PATH = LOCAL_TILES_PATH if LOCAL_MODE else SERVER_FILES_PATH

# Base URL to fetch existing tiles from if not present locally
CELLFIRE_BASE_URL = "https://cellfire.io/files/cellfire-db/"

# cPanel UAPI — used for direct file upload to the server (LOCAL_MODE=False only)
CPANEL_HOST       = "just2029.justhost.com:2083"
CPANEL_USER       = "veterap2"
CPANEL_TOKEN      = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"
CPANEL_UPLOAD_DIR = "/public_html/website_9695cd55/files/cellfire-db"  # corrected path

TILE_SIZE = 0.5  # degrees

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("cellfire_merge")

# ── Confidence scoring ───────────────────────────────────────────────────────

# Base score per source tag (from CrowdsourceReporter / CellScanService)
SOURCE_BASE_SCORE = {
    "alpha":           100,  # operatorAlphaLong — registered modem confirmation
    "plmn":            100,  # MCC+MNC table match on registered cell
    "fcc_band":         90,  # exclusive band (B13/B14/B71) — no ambiguity possible
    "db":               70,  # confirmed by a prior crowd observation, source lost
    "neighbor_infer":   55,  # inferred from nearby confirmed cells (PCI/TAC/geo proximity)
    "pci_range":        20,  # hardcoded PCI bucket — educated guess only
}
SOURCE_BASE_DEFAULT = 40  # OCID-seeded records with no source field


def _sample_boost(samples: int) -> int:
    """
    +0..+15 bonus based on sample count, log2 scale.
    0 samples → 0, 1 → 5, 4 → 10, 20+ → 15
    """
    if samples <= 0:
        return 0
    return min(15, int(math.log2(samples + 1) * 5))


def compute_conf(source: str, samples: int) -> int:
    """Compute confidence score for a NEW record being inserted into a tile."""
    base = SOURCE_BASE_SCORE.get(source, SOURCE_BASE_DEFAULT)
    score = base + _sample_boost(samples)
    return max(0, min(100, score))


def update_conf(existing_rec: dict, obs_source: str, obs_carrier: str) -> int:
    """
    Update confidence for an EXISTING record given a new incoming observation.

    Rules:
    - New alpha/plmn always wins and raises conf toward 100
    - Lower-quality sources only raise conf by a small sample boost
    - Carrier conflict with a trusted obs → conf penalty
    """
    current_conf = existing_rec.get("conf", SOURCE_BASE_DEFAULT)
    stored_carrier = existing_rec.get("carrier", "")
    samples = existing_rec.get("samples", 1)
    obs_base = SOURCE_BASE_SCORE.get(obs_source, SOURCE_BASE_DEFAULT)

    if obs_source in ("alpha", "plmn"):
        # Registered phone — this is ground truth
        if stored_carrier and stored_carrier != obs_carrier and stored_carrier != "Unknown":
            # Carrier conflict: the stored label was wrong, penalize before reset
            new_conf = max(obs_base, current_conf - 20)
        else:
            # Agreement or first real confirmation
            new_conf = max(obs_base + _sample_boost(samples), current_conf)
        # Pull toward 100 for each additional alpha/plmn confirmation
        new_conf = min(100, new_conf + 2)
    elif obs_source == "fcc_band":
        new_conf = max(obs_base + _sample_boost(samples), current_conf)
    else:
        # db / pci_range / unknown — only a small sample boost, never lower conf
        new_conf = max(current_conf, current_conf + 1)

    return max(0, min(100, new_conf))


def backfill_conf(records: list) -> list:
    """
    Assign an initial 'conf' score to OCID-seeded records that don't have one yet.
    Also applies a PCI-collision penalty when multiple carriers share the same PCI
    in this tile.
    """
    # Detect PCI collisions within the tile
    pci_carriers: dict = defaultdict(set)
    for rec in records:
        pci = rec.get("pci", 0)
        carrier = rec.get("carrier", "")
        if pci > 0 and carrier and carrier != "Unknown":
            pci_carriers[pci].add(carrier)

    collision_pcis = {pci for pci, carriers in pci_carriers.items() if len(carriers) > 1}

    for rec in records:
        if "conf" not in rec:
            source = rec.get("source", "")
            samples = rec.get("samples", 1)
            base = compute_conf(source, samples)
            # Collision penalty
            if rec.get("pci", 0) in collision_pcis:
                base -= 10
            rec["conf"] = max(0, min(100, base))

    return records


# ── Neighbor-inference scoring ───────────────────────────────────────────────
#
# When a tile contains confirmed cells (alpha/plmn/fcc_band, conf ≥ 70) we use
# them as "anchors" to guess the carrier of unresolved cells in the same tile.
#
# Signals used (cumulative vote tally per carrier):
#   +35  Same TAC   — TAC is carrier-assigned, not site-assigned; very strong
#   +20  PCI delta ≤ 3   — typical same-site 3-sector cluster (N, N+1, N+2)
#   +12  PCI delta ≤ 10  — common local market planning group
#   + 5  PCI delta ≤ 30  — weak regional hint
#   + 8  mod-3 mismatch  — orthogonal sectors: cand_pci%3 ≠ anchor_pci%3
#   - 5  mod-3 match + delta ≤ 3  — CRS collision risk; suspicious in real nets
#   +10  geo < 1 km / +6 < 3 km / +3 < 10 km
#   + 5  band overlap (possible_bands intersection)
#
# Result: inferred conf capped at 74 → always MED_CONF (yellow dot), never
# HIGH_CONF, so the user sees it as "probably right, not confirmed."

NEIGHBOR_MIN_ANCHOR_CONF  = 70   # Only trust anchors at this confidence or above
NEIGHBOR_APPLY_THRESHOLD  = 30   # Minimum raw vote score to assign a carrier


def _haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Approximate great-circle distance in km."""
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (math.sin(dlat / 2) ** 2
         + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2))
         * math.sin(dlon / 2) ** 2)
    return R * 2 * math.asin(math.sqrt(min(1.0, a)))


def _score_candidate_vs_anchor(cand: dict, anchor: dict) -> int:
    """
    Raw vote score for how likely cand belongs to the same carrier as anchor.
    Returns 0–100+ (unnormalized); 0 means "no signal".
    """
    cand_pci = cand.get("pci", 0)
    anc_pci  = anchor.get("pci", 0)
    if cand_pci <= 0 or anc_pci <= 0 or cand_pci == anc_pci:
        return 0

    score = 0

    # 1. Same TAC — strongest non-RF signal
    cand_tac = cand.get("tac")
    anc_tac  = anchor.get("tac")
    if cand_tac and anc_tac and cand_tac == anc_tac:
        score += 35

    # 2. PCI delta
    delta = abs(cand_pci - anc_pci)
    if delta <= 3:
        score += 20
    elif delta <= 10:
        score += 12
    elif delta <= 30:
        score += 5

    # 3. mod-3 sector compatibility
    #    3-sector sites typically assign PCIs with distinct mod-3 values (0,1,2).
    #    Different mod-3 → orthogonal sectors → good sign they co-exist.
    #    Same mod-3 AND very close delta → potential CRS collision, suspicious.
    if cand_pci % 3 != anc_pci % 3:
        score += 8
    elif delta <= 3:
        score -= 5  # same mod-3, too close — unlikely in real RF planning

    # 4. Geographic proximity
    try:
        dist_km = _haversine_km(
            cand.get("lat", 0.0), cand.get("lon", 0.0),
            anchor.get("lat", 0.0), anchor.get("lon", 0.0),
        )
        if dist_km < 1.0:
            score += 10
        elif dist_km < 3.0:
            score += 6
        elif dist_km < 10.0:
            score += 3
    except Exception:
        pass

    # 5. Band overlap
    cand_bands = set(cand.get("possible_bands", []))
    anc_bands  = set(anchor.get("possible_bands", []))
    if cand_bands and anc_bands and cand_bands & anc_bands:
        score += 5

    return max(0, score)


def infer_neighbor_carriers(records: list) -> list:
    """
    For unresolved / low-confidence records in a tile, vote on carrier using
    nearby confirmed cells (anchors). Updates carrier, source, and conf
    in-place when a carrier wins with enough votes.

    Inferred conf is capped at 74 so these cells always land in MED_CONF
    (yellow dot) — never mistaken for a crowd-confirmed record.
    """
    anchors = [
        r for r in records
        if r.get("conf", 0) >= NEIGHBOR_MIN_ANCHOR_CONF
        and r.get("carrier", "Unknown") not in ("Unknown", "")
    ]
    if not anchors:
        return records

    candidates = [
        r for r in records
        if r.get("carrier", "Unknown") in ("Unknown", "")
        or r.get("conf", 0) < NEIGHBOR_MIN_ANCHOR_CONF
    ]

    updated = 0
    for cand in candidates:
        carrier_scores: dict = defaultdict(int)

        for anchor in anchors:
            s = _score_candidate_vs_anchor(cand, anchor)
            if s > 0:
                carrier_scores[anchor["carrier"]] += s

        if not carrier_scores:
            continue

        best_carrier = max(carrier_scores, key=carrier_scores.__getitem__)
        best_score   = carrier_scores[best_carrier]

        if best_score < NEIGHBOR_APPLY_THRESHOLD:
            continue

        # Convert raw vote score to conf: floor 30, slope 0.5, cap 74
        # (74 = just below HIGH_CONF threshold of 75)
        inferred_conf = min(74, 30 + int(best_score * 0.5))
        current_conf  = cand.get("conf", 0)

        if inferred_conf > current_conf:
            old = f"carrier={cand.get('carrier','?')} conf={current_conf}"
            cand["carrier"] = best_carrier
            cand["source"]  = "neighbor_infer"
            cand["conf"]    = inferred_conf
            log.info(f"  neighbor_infer pci={cand.get('pci')} tac={cand.get('tac')} "
                     f"{old} → {best_carrier} conf={inferred_conf} (votes={best_score})")
            updated += 1

    if updated:
        log.info(f"  neighbor_infer: resolved {updated} records from {len(anchors)} anchors")

    return records


# ── Helpers ──────────────────────────────────────────────────────────────────

def tile_filename(lat: float, lon: float) -> str:
    t_lat = math.floor(lat / TILE_SIZE) * TILE_SIZE
    t_lon = math.floor(lon / TILE_SIZE) * TILE_SIZE
    return f"grid_{coord_str(t_lat)}_{coord_str(t_lon)}.json.gz"


def coord_str(v: float) -> str:
    prefix = "p" if v >= 0 else "n"
    return f"{prefix}{abs(v):.1f}"


def firebase_key_to_filename(key: str) -> str:
    """Convert Firebase tile key to .json.gz filename.

    The app stores tile keys without periods (Firebase forbids them):
      grid_p435_n1120  ->  grid_p43.5_n112.0.json.gz

    Older keys with periods pass through unchanged (backwards-compat).
    """
    if "." in key:
        return f"{key}.json.gz"
    def expand(m):
        val = int(m.group(2)) / 10
        return f"{m.group(1)}{val:.1f}"
    return re.sub(r'([pn])(\d+)', expand, key) + ".json.gz"


def load_tile(filepath: Path) -> list:
    if not filepath.exists():
        url = CELLFIRE_BASE_URL + filepath.name
        try:
            req = urllib.request.Request(url, headers={"Accept": "*/*", "User-Agent": "cellfire-merge/1.0"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = resp.read()
            filepath.parent.mkdir(parents=True, exist_ok=True)
            filepath.write_bytes(data)
            log.info(f"Seeded {filepath.name} from server ({len(data)} bytes)")
        except Exception as e:
            log.warning(f"Could not seed {filepath.name} from server: {e}")
            return []
    with gzip.open(filepath, "rt", encoding="utf-8") as f:
        return json.load(f)


def save_tile(filepath: Path, records: list):
    filepath.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(filepath, "wt", encoding="utf-8") as f:
        json.dump(records, f, separators=(",", ":"))
    log.info(f"Saved {filepath.name} ({len(records)} records, {filepath.stat().st_size} bytes)")


def _band_to_int(band: str) -> int:
    """Convert band label like 'B4', 'n41' to integer (4, 41)."""
    m = re.search(r'\d+', str(band))
    return int(m.group()) if m else 0


def merge_observation(records: list, obs: dict) -> list:
    """
    Merge a single observation into the records list.
    Matches on (pci, tac). Updates carrier/conf/mnc if obs has better source,
    or inserts as a new record if not found.
    """
    pci = obs.get("pci")
    tac = obs.get("tac")
    if pci is None or tac is None:
        return records

    obs_source  = obs.get("source", "")
    obs_carrier = obs.get("carrier", "Unknown")
    obs_base    = SOURCE_BASE_SCORE.get(obs_source, SOURCE_BASE_DEFAULT)

    for rec in records:
        if rec.get("pci") == pci and rec.get("tac") == tac:
            # ── Existing record ──────────────────────────────────────────────
            rec["samples"] = rec.get("samples", 1) + 1

            # Update carrier only when incoming source is more trusted
            stored_base = SOURCE_BASE_SCORE.get(rec.get("source", ""), SOURCE_BASE_DEFAULT)
            if obs_base >= stored_base and obs_carrier != "Unknown":
                rec["carrier"] = obs_carrier
                rec["source"]  = obs_source
                if obs.get("mnc"):
                    rec["mnc"] = obs["mnc"]

            # Update location as running average weighted by samples
            n = rec["samples"]
            rec["lat"] = ((rec.get("lat", obs["lat"]) * (n - 1)) + obs["lat"]) / n
            rec["lon"] = ((rec.get("lon", obs["lon"]) * (n - 1)) + obs["lon"]) / n

            # Recompute confidence
            rec["conf"] = update_conf(rec, obs_source, obs_carrier)

            log.debug(f"  Updated pci={pci} tac={tac} carrier={rec['carrier']} "
                      f"conf={rec['conf']} samples={rec['samples']}")
            return records

    # ── New record ───────────────────────────────────────────────────────────
    new_rec = {
        "pci":            pci,
        "mnc":            obs.get("mnc", ""),
        "carrier":        obs_carrier,
        "lat":            obs.get("lat", 0.0),
        "lon":            obs.get("lon", 0.0),
        "cellid":         0,
        "tac":            tac,
        "range":          0,
        "samples":        1,
        "source":         obs_source,
        "possible_bands": [_band_to_int(obs["band"])] if obs.get("band") else [],
        "conf":           compute_conf(obs_source, 1),
    }
    log.debug(f"  New record pci={pci} tac={tac} carrier={obs_carrier} "
              f"source={obs_source} conf={new_rec['conf']}")
    records.append(new_rec)
    return records


# ── Joomla upload ────────────────────────────────────────────────────────────

def upload_tile(filepath: Path):
    """Upload a tile file directly to the server via cPanel UAPI."""
    try:
        with open(filepath, "rb") as f:
            resp = requests.post(
                f"https://{CPANEL_HOST}/execute/Fileman/upload_files",
                headers={
                    "Authorization": f"cpanel {CPANEL_USER}:{CPANEL_TOKEN}",
                },
                data={"dir": CPANEL_UPLOAD_DIR, "overwrite": "1"},
                files={"file-1": (filepath.name, f, "application/gzip")},
                timeout=30,
                verify=False,
            )
        result = resp.json()
        if resp.status_code == 200 and not result.get("errors"):
            log.info(f"Uploaded {filepath.name} → cellfire.io")
        else:
            log.warning(f"Upload {filepath.name} failed: {resp.status_code} — {result.get('errors') or resp.text[:200]}")
    except Exception as e:
        log.warning(f"Upload {filepath.name} error: {e}")


# ── Firebase init ────────────────────────────────────────────────────────────

def init_firebase():
    """Initialize Firebase once. Safe to call multiple times in loop mode."""
    if not firebase_admin._apps:
        key_path = Path(SERVICE_ACCOUNT_KEY)
        if not key_path.exists():
            raise FileNotFoundError(
                f"Firebase service account key not found: {key_path.resolve()}\n"
                "Download it from Firebase Console → Project Settings → Service Accounts."
            )
        cred = credentials.Certificate(str(key_path))
        firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_DATABASE_URL})
        log.info("Firebase initialized.")


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    init_firebase()
    log.info("=== Cellfire merge started ===")
    obs_ref = firebase_db.reference("observations")
    all_tiles = obs_ref.get()

    if not all_tiles:
        log.info("No observations in Firebase.")
        return

    processed_keys = []  # (tile_key, node_key) pairs to delete after merge

    for tile_key, pushes in all_tiles.items():
        if not isinstance(pushes, dict):
            continue

        # Unprocessed = processed flag is absent or False
        unprocessed = {nid: obs for nid, obs in pushes.items()
                       if isinstance(obs, dict) and not obs.get("processed", False)}

        if not unprocessed:
            continue

        log.info(f"Tile {tile_key}: {len(unprocessed)} new observations")

        # Load existing tile records and backfill any missing conf scores
        filepath = Path(CELLFIRE_FILES_PATH) / firebase_key_to_filename(tile_key)
        records = load_tile(filepath)
        records = backfill_conf(records)

        for node_key, obs in unprocessed.items():
            records = merge_observation(records, obs)
            processed_keys.append((tile_key, node_key))

        # Infer carrier for unresolved cells using confirmed neighbors in tile
        records = infer_neighbor_carriers(records)

        save_tile(filepath, records)
        if LOCAL_MODE:
            # Running locally — push the merged tile to the server via cPanel
            upload_tile(filepath)
        # When LOCAL_MODE=False: already written directly to the server filesystem, no upload needed

    # Delete processed entries from Firebase — keeps the DB clean, no accumulation
    for tile_key, node_key in processed_keys:
        firebase_db.reference(f"observations/{tile_key}/{node_key}").delete()

    log.info(f"=== Done. Merged and deleted {len(processed_keys)} observations across "
             f"{len(set(k for k,_ in processed_keys))} tiles ===")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Cellfire observation merge")
    parser.add_argument("--loop", type=int, metavar="SECONDS",
                        help="Re-run every N seconds (drive-test mode)")
    args = parser.parse_args()

    if args.loop:
        log.info(f"Loop mode: running every {args.loop}s — Ctrl+C to stop")
        while True:
            try:
                main()
            except Exception as e:
                log.error(f"Run failed: {e}")
            time.sleep(args.loop)
    else:
        main()
