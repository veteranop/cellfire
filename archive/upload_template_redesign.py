#!/usr/bin/env python3
"""Upload redesigned template CSS, JS, and article pages to the server."""
import urllib.request, urllib.parse, json, ssl, base64
from pathlib import Path

CPANEL_HOST  = "just2029.justhost.com:2083"
CPANEL_USER  = "veterap2"
CPANEL_TOKEN = "ZSKZZ5Q31MSH1BHC9TDUN3ZVDZNIDFUH"

# Server paths
TEMPLATE_ROOT = "/home1/veterap2/public_html/website_9695cd55/templates/cellfire"
ARTICLES_ROOT = "/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article"

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def cpanel_request(action, params):
    url = f"https://{CPANEL_HOST}/execute/{action}"
    data = urllib.parse.urlencode(params).encode()
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Authorization", f"cpanel {CPANEL_USER}:{CPANEL_TOKEN}")
    with urllib.request.urlopen(req, context=ctx, timeout=30) as r:
        return json.loads(r.read())

def upload_file(local_path: Path, remote_dir: str, remote_name: str = None):
    content = local_path.read_text(encoding="utf-8")
    name    = remote_name or local_path.name
    res = cpanel_request("Fileman/save_file_content", {
        "dir":      remote_dir,
        "file":     name,
        "content":  content,
    })
    ok = res.get("status") == 1 or res.get("result", {}).get("status") == 1
    print(f"  {'OK ' if ok else 'ERR'} {remote_dir}/{name}")
    if not ok:
        print(f"       {res}")
    return ok

BASE = Path(r"C:\Users\markd\StudioProjects\cellfire\site_template")

files = [
    # (local path, remote dir, remote filename)
    (BASE / "css"  / "template.css", f"{TEMPLATE_ROOT}/css",    "template.css"),
    (BASE / "js"   / "template.js",  f"{TEMPLATE_ROOT}/js",     "template.js"),
    (BASE / "articles" / "cf_app.php",    ARTICLES_ROOT, "cf_app.php"),
    (BASE / "articles" / "cf_viewer.php", ARTICLES_ROOT, "cf_viewer.php"),
    (BASE / "articles" / "cf_studio.php", ARTICLES_ROOT, "cf_studio.php"),
]

print("Uploading redesigned template files...")
all_ok = True
for local, remote_dir, remote_name in files:
    if not local.exists():
        print(f"  MISS {local}")
        all_ok = False
        continue
    ok = upload_file(local, remote_dir, remote_name)
    all_ok = all_ok and ok

print()
print("Done." if all_ok else "Some uploads failed — check output above.")
