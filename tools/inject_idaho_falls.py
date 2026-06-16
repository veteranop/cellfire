#!/usr/bin/env python3
"""
One-shot injector for Idaho Falls drive-test data (2026-03-22).
Injects 14 confirmed T-Mobile cells (conf=100) and 4 Unknown candidates
into the appropriate Idaho Falls tiles, then re-runs neighbour inference
so PCI 471 and the B12 candidates get scored.
"""
import sys
sys.path.insert(0, "/home1/veterap2/cellfire")
from merge_observations import (
    load_tile, save_tile, infer_neighbor_carriers, backfill_conf,
    CELLFIRE_FILES_PATH,
)
import json, logging, ssl, urllib.request
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("inject_idaho")

CPANEL_HOST = "just2029.justhost.com:2083"
CPANEL_USER = "veterap2"
CPANEL_TOKEN = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"
CPANEL_UPLOAD_DIR = "/public_html/website_9695cd55/files/cellfire-db"

CONFIRMED = [
    {"tile":"grid_p43.0_n112.5.json.gz","pci":309,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48904,"lon":-112.02256,"samples":54,"earfcn":5035},
    {"tile":"grid_p43.0_n112.5.json.gz","pci":203,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48899,"lon":-112.02264,"samples":26,"earfcn":66786},
    {"tile":"grid_p43.0_n112.5.json.gz","pci":362,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48988,"lon":-112.02372,"samples":13,"earfcn":66786},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":56, "tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.55479,"lon":-111.98759,"samples":90,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":360,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.61007,"lon":-111.95545,"samples":57,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":63, "tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.52286,"lon":-111.98443,"samples":40,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":265,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.58763,"lon":-111.97205,"samples":24,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":42, "tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.62517,"lon":-111.94444,"samples":6, "earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":16, "tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.59100,"lon":-111.96383,"samples":2, "earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":205,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.54772,"lon":-111.99933,"samples":1, "earfcn":66786},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":124,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.50407,"lon":-112.02380,"samples":78,"earfcn":66936},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":115,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.51124,"lon":-112.00825,"samples":58,"earfcn":66936},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":81, "tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.52290,"lon":-112.02063,"samples":40,"earfcn":66786},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":320,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.53616,"lon":-112.01077,"samples":1, "earfcn":66936},
]

CANDIDATES = [
    {"tile":"grid_p43.5_n112.0.json.gz","pci":471,"tac":0,"lat":43.61039,"lon":-111.95534,"samples":67,"earfcn":66786},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":282,"tac":0,"lat":43.60658,"lon":-111.95743,"samples":27,"earfcn":5035},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":272,"tac":0,"lat":43.55114,"lon":-111.99812,"samples":10,"earfcn":5035},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":185,"tac":0,"lat":43.62005,"lon":-111.94791,"samples":8, "earfcn":5035},
]


def upsert(records, new_rec):
    pci, tac = new_rec["pci"], new_rec["tac"]
    new_conf = new_rec.get("conf", 0)
    for i, r in enumerate(records):
        if r.get("pci") == pci and r.get("tac") == tac:
            if new_conf > r.get("conf", 0):
                records[i] = {**r, **new_rec}
                return records, True
            return records, False
    records.append(new_rec)
    return records, True


def upload(filepath):
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    gz_bytes = filepath.read_bytes()
    boundary = "----CellfireSeedBoundary"
    body = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"dir\"\r\n\r\n{CPANEL_UPLOAD_DIR}\r\n"
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file-1\"; filename=\"{filepath.name}\"\r\nContent-Type: application/octet-stream\r\n\r\n"
    ).encode() + gz_bytes + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(
        f"https://{CPANEL_HOST}/execute/Fileman/upload_files",
        data=body, method="POST",
        headers={
            "Authorization": f"cpanel {CPANEL_USER}:{CPANEL_TOKEN}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
        r = json.loads(resp.read())
        if r.get("status") == 1:
            log.info(f"  Uploaded {filepath.name}")
        else:
            log.warning(f"  Upload: {r}")


tiles_path = Path(CELLFIRE_FILES_PATH)
by_tile = {}
for rec in CONFIRMED:
    t = rec["tile"]
    by_tile.setdefault(t, {"confirmed": [], "candidates": []})["confirmed"].append({
        "pci": rec["pci"], "mnc": rec["mnc"], "carrier": rec["carrier"],
        "lat": rec["lat"], "lon": rec["lon"], "cellid": 0,
        "tac": rec["tac"], "range": 500, "samples": rec["samples"],
        "conf": 100, "source": "alpha",
    })
for rec in CANDIDATES:
    t = rec["tile"]
    by_tile.setdefault(t, {"confirmed": [], "candidates": []})["candidates"].append({
        "pci": rec["pci"], "mnc": "000", "carrier": "Unknown",
        "lat": rec["lat"], "lon": rec["lon"], "cellid": 0,
        "tac": rec["tac"], "range": 500, "samples": rec["samples"],
        "conf": 0, "source": "",
    })

for tile_name, groups in by_tile.items():
    fp = tiles_path / tile_name
    records = load_tile(fp)
    before = json.dumps(records, sort_keys=True)

    for rec in groups["confirmed"]:
        records, changed = upsert(records, rec)
        tag = "updated" if changed else "unchanged"
        log.info(f"  [{tile_name}] confirmed pci={rec['pci']} tac={rec['tac']} carrier={rec['carrier']} -> {tag}")

    cands_added = 0
    for rec in groups["candidates"]:
        records, changed = upsert(records, rec)
        if changed:
            cands_added += 1
    if cands_added:
        log.info(f"  [{tile_name}] +{cands_added} unknown candidates injected")

    records = backfill_conf(records)
    records = infer_neighbor_carriers(records)

    after = json.dumps(records, sort_keys=True)
    if after != before:
        save_tile(fp, records)
        upload(fp)
        log.info(f"  [{tile_name}] saved and uploaded ({len(records)} records)")
    else:
        log.info(f"  [{tile_name}] no net change")

log.info("=== inject_idaho_falls.py complete ===")
