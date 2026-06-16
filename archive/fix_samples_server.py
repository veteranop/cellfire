#!/usr/bin/env python3
import sys, json, gzip, logging, ssl, urllib.request
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s", stream=sys.stdout)
log = logging.getLogger("fix_samples")
log.info("=== fix_sample_counts starting ===")

CELLFIRE_FILES_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"
CPANEL_HOST = "just2029.justhost.com:2083"
CPANEL_USER = "veterap2"
CPANEL_TOKEN = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"
CPANEL_UPLOAD_DIR = "/public_html/website_9695cd55/files/cellfire-db"

UPDATES = [
    ("grid_p43.0_n112.5.json.gz",  309, 10860, 54, "alpha", "T-Mobile", "260", 43.48904, -112.02256),
    ("grid_p43.5_n112.0.json.gz",   56, 10860, 90, "alpha", "T-Mobile", "260", 43.55479, -111.98759),
    ("grid_p43.5_n112.0.json.gz",  360, 10860, 57, "alpha", "T-Mobile", "260", 43.61007, -111.95545),
    ("grid_p43.5_n112.0.json.gz",   63, 10860, 40, "alpha", "T-Mobile", "260", 43.52286, -111.98443),
    ("grid_p43.5_n112.0.json.gz",  265, 10860, 24, "alpha", "T-Mobile", "260", 43.58763, -111.97205),
    ("grid_p43.5_n112.0.json.gz",   42, 10860,  6, "alpha", "T-Mobile", "260", 43.62517, -111.94444),
    ("grid_p43.5_n112.5.json.gz",  124, 10860, 78, "alpha", "T-Mobile", "260", 43.50407, -112.02380),
    ("grid_p43.5_n112.5.json.gz",  115, 10860, 58, "alpha", "T-Mobile", "260", 43.51124, -112.00825),
    ("grid_p43.5_n112.5.json.gz",   81, 10860, 40, "alpha", "T-Mobile", "260", 43.52290, -112.02063),
]

def upload_tile(filepath):
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    gz_bytes = filepath.read_bytes()
    boundary = "----CellfireSeedBoundary"
    part1 = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"dir\"\r\n\r\n" + CPANEL_UPLOAD_DIR + "\r\n"
    part2 = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file-1\"; filename=\"" + filepath.name + "\"\r\nContent-Type: application/octet-stream\r\n\r\n"
    part3 = "\r\n--" + boundary + "--\r\n"
    body = part1.encode() + part2.encode() + gz_bytes + part3.encode()
    req = urllib.request.Request(
        "https://" + CPANEL_HOST + "/execute/Fileman/upload_files",
        data=body, method="POST",
        headers={
            "Authorization": "cpanel " + CPANEL_USER + ":" + CPANEL_TOKEN,
            "Content-Type": "multipart/form-data; boundary=" + boundary,
        },
    )
    with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
        r = json.loads(resp.read())
        if r.get("status") == 1:
            log.info("Uploaded " + filepath.name)
        else:
            uploads = r.get("data", {}).get("uploads", [{}])
            reason = uploads[0].get("reason", "?") if uploads else "?"
            log.warning("Upload issue (" + filepath.name + "): " + reason)

by_tile = {}
for tile, pci, tac, samples, source, carrier, mnc, lat, lon in UPDATES:
    by_tile.setdefault(tile, []).append((pci, tac, samples, source, carrier, mnc, lat, lon))

tiles_path = Path(CELLFIRE_FILES_PATH)
for tile_name, updates in by_tile.items():
    fp = tiles_path / tile_name
    if not fp.exists():
        log.warning(tile_name + " not found, skipping")
        continue
    with gzip.open(fp, "rt", encoding="utf-8") as f:
        records = json.load(f)
    before = json.dumps(records, sort_keys=True)
    for pci, tac, new_samples, source, carrier, mnc, lat, lon in updates:
        found = False
        for i, r in enumerate(records):
            if r.get("pci") == pci and r.get("tac") == tac:
                found = True
                old_s = r.get("samples", 0)
                if new_samples > old_s:
                    records[i]["samples"] = new_samples
                    records[i]["source"] = source
                    records[i]["carrier"] = carrier
                    records[i]["conf"] = 100
                    records[i]["lat"] = lat
                    records[i]["lon"] = lon
                    log.info("  [" + tile_name + "] pci=" + str(pci) + " tac=" + str(tac) + ": samples " + str(old_s) + "->" + str(new_samples))
                else:
                    log.info("  [" + tile_name + "] pci=" + str(pci) + " tac=" + str(tac) + ": already " + str(old_s) + ">=" + str(new_samples))
                break
        if not found:
            log.warning("  [" + tile_name + "] pci=" + str(pci) + " tac=" + str(tac) + " NOT FOUND")
    after = json.dumps(records, sort_keys=True)
    if after != before:
        with gzip.open(fp, "wt", encoding="utf-8") as f:
            json.dump(records, f, separators=(",", ":"))
        log.info("  Saved " + fp.name + " (" + str(len(records)) + " records, " + str(fp.stat().st_size) + " bytes)")
        upload_tile(fp)
    else:
        log.info("  [" + tile_name + "] no change needed")

log.info("=== fix_sample_counts complete ===")
