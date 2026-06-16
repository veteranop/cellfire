#!/usr/bin/env python3
"""
push_rules.py — Upload cellfire JSON rule files to cellfire.io via cPanel UAPI.

Usage:
    python push_rules.py                  # push all files
    python push_rules.py band_carrier     # push band_carrier_lookup.json only

Setup (one time):
    1. Log into cPanel → Security → Manage API Tokens → Create token named "cellfire-push"
    2. Copy the token and paste it into push_rules_config.py (see below)
"""

import json
import sys
import urllib.request
import urllib.parse
import ssl
import os

# ---------------------------------------------------------------------------
# Config — edit these or put them in push_rules_config.py next to this file
# ---------------------------------------------------------------------------
CPANEL_HOST   = "just2029.justhost.com"
CPANEL_PORT   = 2083
CPANEL_USER   = "veterap2"
SERVER_DIR    = "/home1/veterap2/public_html/website_9695cd55/files/cellfire-db"
API_TOKEN     = ""   # paste your cPanel API token here, or set in push_rules_config.py

# Local asset files → server filename
ASSETS_DIR = os.path.join(os.path.dirname(__file__),
                          "app", "src", "main", "assets")
FILES = {
    "band_carrier":  "band_carrier_lookup.json",
    "pci_carrier":   "pci_carrier_data.json",
    "earfcn":        "carrier_earfcn_map.json",
}

# ---------------------------------------------------------------------------
# Load config override if it exists
# ---------------------------------------------------------------------------
_config_path = os.path.join(os.path.dirname(__file__), "push_rules_config.py")
if os.path.exists(_config_path):
    _cfg = {}
    with open(_config_path) as f:
        exec(f.read(), _cfg)
    API_TOKEN = _cfg.get("API_TOKEN", API_TOKEN)
    CPANEL_USER = _cfg.get("CPANEL_USER", CPANEL_USER)
    CPANEL_HOST = _cfg.get("CPANEL_HOST", CPANEL_HOST)

# ---------------------------------------------------------------------------

def push_file(filename: str) -> bool:
    local_path = os.path.join(ASSETS_DIR, filename)
    if not os.path.exists(local_path):
        print(f"  SKIP  {filename} — not found at {local_path}")
        return False

    with open(local_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Quick JSON validation
    try:
        parsed = json.loads(content)
        version = parsed.get("version", "?")
    except json.JSONDecodeError as e:
        print(f"  ERROR {filename} — invalid JSON: {e}")
        return False

    url = f"https://{CPANEL_HOST}:{CPANEL_PORT}/execute/Fileman/save_file_content"
    body = urllib.parse.urlencode({
        "dir":     SERVER_DIR,
        "file":    filename,
        "content": content,
    }).encode("utf-8")

    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Authorization", f"cpanel {CPANEL_USER}:{API_TOKEN}")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")

    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    try:
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            result = json.loads(resp.read())
    except Exception as e:
        print(f"  ERROR {filename} — request failed: {e}")
        return False

    if result.get("status") == 1:
        print(f"  OK    {filename}  (version: {version})")
        return True
    else:
        errors = result.get("errors") or result.get("messages") or result
        print(f"  FAIL  {filename} — {errors}")
        return False


def main():
    if not API_TOKEN:
        print("ERROR: API_TOKEN is not set.")
        print("       Create a cPanel API token, then either:")
        print("       1. Paste it into push_rules.py (API_TOKEN = '...')")
        print("       2. Or create push_rules_config.py with: API_TOKEN = '...'")
        sys.exit(1)

    # Which files to push
    if len(sys.argv) > 1:
        keys = sys.argv[1:]
        targets = {}
        for k in keys:
            if k not in FILES:
                print(f"Unknown key '{k}'. Valid keys: {', '.join(FILES)}")
                sys.exit(1)
            targets[k] = FILES[k]
    else:
        targets = FILES

    print(f"Pushing {len(targets)} file(s) to {CPANEL_HOST}:{SERVER_DIR}\n")
    ok = 0
    for key, fname in targets.items():
        if push_file(fname):
            ok += 1

    print(f"\n{ok}/{len(targets)} files pushed successfully.")
    if ok < len(targets):
        sys.exit(1)


if __name__ == "__main__":
    main()
