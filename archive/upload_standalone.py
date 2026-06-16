#!/usr/bin/env python3
"""Upload standalone inject script to server."""
import urllib.request, urllib.parse, ssl, json

STANDALONE = r'''#!/usr/bin/env python3
import sys, json, gzip, logging, ssl, math, re, urllib.request
from pathlib import Path
from collections import defaultdict

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s", stream=sys.stdout)
log = logging.getLogger("inject_idaho")
log.info("=== inject_idaho_falls_standalone starting ===")

CELLFIRE_FILES_PATH = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"
CELLFIRE_BASE_URL = "https://cellfire.io/files/cellfire-db/"
CPANEL_HOST = "just2029.justhost.com:2083"
CPANEL_USER = "veterap2"
CPANEL_TOKEN = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"
CPANEL_UPLOAD_DIR = "/public_html/website_9695cd55/files/cellfire-db"

SOURCE_BASE_SCORE = {
    "alpha": 100, "plmn": 100, "fcc_band": 90,
    "db": 70, "neighbor_infer": 55, "pci_range": 20,
}

def _sample_boost(s):
    if s <= 0: return 0
    return min(15, int(math.log2(s + 1) * 5))

def compute_conf(source, samples):
    base = SOURCE_BASE_SCORE.get(source, 40)
    return max(0, min(100, base + _sample_boost(samples)))

def backfill_conf(records):
    pci_carriers = defaultdict(set)
    for r in records:
        if r.get("pci", 0) > 0 and r.get("carrier", "") and r.get("carrier", "") != "Unknown":
            pci_carriers[r["pci"]].add(r["carrier"])
    collision_pcis = {p for p, cs in pci_carriers.items() if len(cs) > 1}
    for r in records:
        if "conf" not in r:
            base = compute_conf(r.get("source", ""), r.get("samples", 1))
            if r.get("pci", 0) in collision_pcis:
                base -= 10
            r["conf"] = max(0, min(100, base))
    return records

def _haversine_km(lat1, lon1, lat2, lon2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1))*math.cos(math.radians(lat2))*math.sin(dlon/2)**2
    return R * 2 * math.asin(math.sqrt(min(1.0, a)))

def _earfcn_to_band(earfcn):
    if not earfcn: return 0
    KNOWN = {5035:17, 66786:4, 66936:4, 2050:2, 2175:2, 1100:2, 500:2,
             38400:41, 39150:41, 2525:25, 2650:25, 5330:12, 6300:14,
             9820:29, 9210:26, 8690:26, 8040:17, 8190:13}
    if earfcn in KNOWN: return KNOWN[earfcn]
    if 0 < earfcn <= 599: return 1
    if 600 <= earfcn <= 1199: return 2
    if 1200 <= earfcn <= 1949: return 3
    if 1950 <= earfcn <= 2399: return 4
    if 2400 <= earfcn <= 2649: return 5
    if 4750 <= earfcn <= 5249: return 17
    if 5180 <= earfcn <= 5279: return 12
    if 8690 <= earfcn <= 9039: return 26
    if 66436 <= earfcn <= 67335: return 66
    return 0

def infer_neighbor_carriers(records):
    anchors = [r for r in records if r.get("conf", 0) >= 70 and r.get("carrier", "Unknown") != "Unknown"]
    candidates = [r for r in records if r.get("carrier", "Unknown") == "Unknown" or r.get("conf", 0) < 40]
    if not anchors or not candidates:
        return records
    by_key = {(r.get("pci"), r.get("tac")): i for i, r in enumerate(records)}
    for cand in candidates:
        votes = defaultdict(int)
        c_earfcn = cand.get("arfcn") or cand.get("earfcn", 0)
        c_band = _earfcn_to_band(c_earfcn)
        for anch in anchors:
            if anch.get("pci") == cand.get("pci") and anch.get("tac") == cand.get("tac"):
                continue
            a_earfcn = anch.get("arfcn") or anch.get("earfcn", 0)
            a_band = _earfcn_to_band(a_earfcn)
            score = 0
            if c_earfcn and a_earfcn:
                if c_earfcn == a_earfcn:
                    score += 40
                elif c_band and a_band and c_band == a_band and abs(c_earfcn - a_earfcn) <= 100:
                    score += 15
                elif c_band and a_band and c_band != a_band:
                    pass
                else:
                    score += 5
            elif c_band and a_band and c_band == a_band:
                score += 5
            same_band = not (c_band and a_band and c_band != a_band)
            if same_band:
                dpci = abs(cand.get("pci", 0) - anch.get("pci", 0))
                if dpci <= 3: score += 20
                elif dpci <= 10: score += 12
                elif dpci <= 30: score += 5
                if cand.get("pci", 0) % 3 != anch.get("pci", 0) % 3:
                    score += 8
                elif dpci <= 3:
                    score -= 5
            if cand.get("tac") and cand.get("tac") > 0 and cand.get("tac") == anch.get("tac"):
                score += 35
            clat = cand.get("lat", 0); clon = cand.get("lon", 0)
            alat = anch.get("lat", 0); alon = anch.get("lon", 0)
            if clat and alat:
                d = _haversine_km(clat, clon, alat, alon)
                if d < 1: score += 10
                elif d < 3: score += 6
                elif d < 10: score += 3
            votes[anch["carrier"]] += score
        if not votes:
            continue
        best = max(votes, key=votes.__getitem__)
        if votes[best] >= 30:
            idx = by_key.get((cand.get("pci"), cand.get("tac")))
            if idx is not None:
                old_conf = records[idx].get("conf", 0)
                inferred_conf = min(74, 40 + int(math.log2(votes[best] + 1) * 5))
                if inferred_conf > old_conf:
                    records[idx]["carrier"] = best
                    records[idx]["conf"] = inferred_conf
                    records[idx]["source"] = "neighbor_infer"
                    log.info(f"  Inferred pci={cand.get('pci')} -> {best} conf={inferred_conf} votes={votes[best]}")
    return records

def load_tile(fp):
    if not fp.exists():
        url = CELLFIRE_BASE_URL + fp.name
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "cellfire-inject/1.0"})
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
                data = resp.read()
            fp.parent.mkdir(parents=True, exist_ok=True)
            fp.write_bytes(data)
            log.info(f"Seeded {fp.name} ({len(data)} bytes)")
        except Exception as e:
            log.warning(f"Could not seed {fp.name}: {e}")
            return []
    try:
        with gzip.open(fp, "rt", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        log.warning(f"Corrupt tile {fp.name}: {e}")
        return []

def save_tile(fp, records):
    fp.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(fp, "wt", encoding="utf-8") as f:
        json.dump(records, f, separators=(",", ":"))
    log.info(f"Saved {fp.name} ({len(records)} records, {fp.stat().st_size} bytes)")

def upload_tile(filepath):
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
            log.info(f"Uploaded {filepath.name}")
        else:
            log.warning(f"Upload failed: {r}")

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

CONFIRMED = [
    {"tile":"grid_p43.0_n112.5.json.gz","pci":309,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48904,"lon":-112.02256,"samples":54,"earfcn":5035},
    {"tile":"grid_p43.0_n112.5.json.gz","pci":203,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48899,"lon":-112.02264,"samples":26,"earfcn":66786},
    {"tile":"grid_p43.0_n112.5.json.gz","pci":362,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.48988,"lon":-112.02372,"samples":13,"earfcn":66786},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":56,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.55479,"lon":-111.98759,"samples":90,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":360,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.61007,"lon":-111.95545,"samples":57,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":63,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.52286,"lon":-111.98443,"samples":40,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":265,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.58763,"lon":-111.97205,"samples":24,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":42,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.62517,"lon":-111.94444,"samples":6,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":16,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.59100,"lon":-111.96383,"samples":2,"earfcn":66936},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":205,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.54772,"lon":-111.99933,"samples":1,"earfcn":66786},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":124,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.50407,"lon":-112.02380,"samples":78,"earfcn":66936},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":115,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.51124,"lon":-112.00825,"samples":58,"earfcn":66936},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":81,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.52290,"lon":-112.02063,"samples":40,"earfcn":66786},
    {"tile":"grid_p43.5_n112.5.json.gz","pci":320,"tac":10860,"carrier":"T-Mobile","mnc":"260","lat":43.53616,"lon":-112.01077,"samples":1,"earfcn":66936},
]
CANDIDATES = [
    {"tile":"grid_p43.5_n112.0.json.gz","pci":471,"tac":0,"lat":43.61039,"lon":-111.95534,"samples":67,"earfcn":66786},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":282,"tac":0,"lat":43.60658,"lon":-111.95743,"samples":27,"earfcn":5035},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":272,"tac":0,"lat":43.55114,"lon":-111.99812,"samples":10,"earfcn":5035},
    {"tile":"grid_p43.5_n112.0.json.gz","pci":185,"tac":0,"lat":43.62005,"lon":-111.94791,"samples":8,"earfcn":5035},
]

tiles_path = Path(CELLFIRE_FILES_PATH)
by_tile = {}
for rec in CONFIRMED:
    t = rec["tile"]
    by_tile.setdefault(t, {"confirmed": [], "candidates": []})
    by_tile[t]["confirmed"].append({
        "pci": rec["pci"], "mnc": rec["mnc"], "carrier": rec["carrier"],
        "lat": rec["lat"], "lon": rec["lon"], "cellid": 0,
        "tac": rec["tac"], "range": 500, "samples": rec["samples"],
        "conf": 100, "source": "alpha", "arfcn": rec["earfcn"],
    })
for rec in CANDIDATES:
    t = rec["tile"]
    by_tile.setdefault(t, {"confirmed": [], "candidates": []})
    by_tile[t]["candidates"].append({
        "pci": rec["pci"], "mnc": "000", "carrier": "Unknown",
        "lat": rec["lat"], "lon": rec["lon"], "cellid": 0,
        "tac": rec["tac"], "range": 500, "samples": rec["samples"],
        "conf": 0, "source": "", "arfcn": rec["earfcn"],
    })

for tile_name, groups in by_tile.items():
    fp = tiles_path / tile_name
    records = load_tile(fp)
    before = json.dumps(records, sort_keys=True)
    for rec in groups["confirmed"]:
        records, changed = upsert(records, rec)
        log.info(f"  [{tile_name}] pci={rec['pci']} tac={rec['tac']} conf=100 -> {'updated' if changed else 'unchanged'}")
    cands_added = 0
    for rec in groups["candidates"]:
        records, changed = upsert(records, rec)
        if changed:
            cands_added += 1
    if cands_added:
        log.info(f"  [{tile_name}] +{cands_added} unknown candidates inserted")
    records = backfill_conf(records)
    records = infer_neighbor_carriers(records)
    after = json.dumps(records, sort_keys=True)
    if after != before:
        save_tile(fp, records)
        upload_tile(fp)
        log.info(f"  [{tile_name}] saved+uploaded ({len(records)} records)")
    else:
        log.info(f"  [{tile_name}] no net change")

log.info("=== inject_idaho_falls_standalone complete ===")
'''

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

data = urllib.parse.urlencode({
    'dir': '/home1/veterap2/cellfire',
    'file': 'inject_standalone.py',
    'content': STANDALONE,
}).encode()

req = urllib.request.Request(
    'https://cellfire.io:2083/execute/Fileman/save_file_content',
    data=data,
    method='POST',
    headers={
        'Authorization': 'cpanel veterap2:ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH',
        'Content-Type': 'application/x-www-form-urlencoded',
    },
)
with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
    r = json.loads(resp.read())
    print('Upload result:', json.dumps(r, indent=2))
