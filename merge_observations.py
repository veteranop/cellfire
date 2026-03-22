#!/usr/bin/env python3
"""
Cellfire DB Merge Script
========================
Reads crowd-sourced observations from Firebase Realtime Database and merges
them into the cellfire-db tile .json.gz files on cellfire.io.

Requirements:
    pip install firebase-admin requests

Setup:
    1. Download your Firebase service account key from:
       Firebase Console → Project Settings → Service Accounts → Generate new private key
    2. Save it as serviceAccountKey.json in the same directory as this script
    3. Set CELLFIRE_FILES_PATH to a local working directory for tile files
    4. Generate a Joomla API token:
         Joomla Admin → Users → Manage → click your user → API Token tab → copy token
    5. Set JOOMLA_API_TOKEN below
    6. Run: python3 merge_observations.py

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

# Absolute path to the cellfire-db directory on the server
CELLFIRE_FILES_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"

# Base URL to fetch existing tiles from if not present locally
CELLFIRE_BASE_URL = "https://cellfire.io/files/cellfire-db/"

# cPanel UAPI — used for direct file upload to the server
CPANEL_HOST      = "just2029.justhost.com:2083"
CPANEL_USER      = "veterap2"
CPANEL_TOKEN     = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"
CPANEL_UPLOAD_DIR = "/public_html/files/cellfire-db"

TILE_SIZE = 0.5  # degrees
MIN_SAMPLES_TO_TRUST = 1  # lower = more permissive crowd-sourcing

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("cellfire_merge")

# ── Firebase init ────────────────────────────────────────────────────────────

cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_DATABASE_URL})


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
    Matches on (pci, tac). Updates carrier/mnc if obs has more samples,
    or inserts as a new record if not found.
    """
    pci = obs.get("pci")
    tac = obs.get("tac")
    if pci is None or tac is None:
        return records

    for rec in records:
        if rec.get("pci") == pci and rec.get("tac") == tac:
            # Existing record — increment samples and update carrier if confirmed
            rec["samples"] = rec.get("samples", 1) + 1
            if obs.get("source") in ("alpha", "plmn", "fcc_band"):
                rec["carrier"] = obs["carrier"]
                if obs.get("mnc"):
                    rec["mnc"] = obs["mnc"]
            # Update location as running average
            n = rec["samples"]
            rec["lat"] = ((rec.get("lat", obs["lat"]) * (n - 1)) + obs["lat"]) / n
            rec["lon"] = ((rec.get("lon", obs["lon"]) * (n - 1)) + obs["lon"]) / n
            return records

    # New record not in existing tile
    new_rec = {
        "pci":            pci,
        "mnc":            obs.get("mnc", ""),
        "carrier":        obs.get("carrier", "Unknown"),
        "lat":            obs.get("lat", 0.0),
        "lon":            obs.get("lon", 0.0),
        "cellid":         0,
        "tac":            tac,
        "range":          0,
        "samples":        1,
        "possible_bands": [_band_to_int(obs["band"])] if obs.get("band") else []
    }
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


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    log.info("=== Cellfire merge started ===")
    obs_ref = firebase_db.reference("observations")
    all_tiles = obs_ref.get()

    if not all_tiles:
        log.info("No observations in Firebase.")
        return

    processed_keys = []  # (tile_key, push_id) pairs to mark processed

    for tile_key, pushes in all_tiles.items():
        if not isinstance(pushes, dict):
            continue

        unprocessed = {pid: obs for pid, obs in pushes.items()
                       if isinstance(obs, dict) and not obs.get("processed", False)}

        if not unprocessed:
            continue

        log.info(f"Tile {tile_key}: {len(unprocessed)} new observations")

        # Load existing tile records
        filepath = Path(CELLFIRE_FILES_PATH) / firebase_key_to_filename(tile_key)
        records = load_tile(filepath)

        for push_id, obs in unprocessed.items():
            records = merge_observation(records, obs)
            processed_keys.append((tile_key, push_id))

        save_tile(filepath, records)
        upload_tile(filepath)

    # Mark processed in Firebase
    for tile_key, push_id in processed_keys:
        firebase_db.reference(f"observations/{tile_key}/{push_id}/processed").set(True)

    log.info(f"=== Done. Processed {len(processed_keys)} observations across "
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
