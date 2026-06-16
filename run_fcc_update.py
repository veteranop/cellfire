#!/usr/bin/env python3
"""
run_fcc_update.py
Dispatches the FCC band license map update workflow on GitHub Actions.
Deployed to cellfire.io and called from server cron every Monday.

Requires fcc_github_token.txt alongside this script on the server —
a single line containing a GitHub personal access token with
workflow dispatch permission on veteranop/cellfire.
"""
import json
import os
import sys
import urllib.request

_here        = os.path.dirname(os.path.abspath(__file__))
_token_file  = os.path.join(_here, "fcc_github_token.txt")

try:
    GITHUB_TOKEN = open(_token_file).read().strip()
except FileNotFoundError:
    print(f"Error: {_token_file} not found. Create it with your GitHub token.", file=sys.stderr)
    sys.exit(1)

REPO     = "veteranop/cellfire"
WORKFLOW = "fcc-band-update.yml"
BRANCH   = "master"

url  = f"https://api.github.com/repos/{REPO}/actions/workflows/{WORKFLOW}/dispatches"
data = json.dumps({"ref": BRANCH}).encode()

req = urllib.request.Request(url, data=data, method="POST")
req.add_header("Authorization",        f"Bearer {GITHUB_TOKEN}")
req.add_header("Accept",               "application/vnd.github+json")
req.add_header("X-GitHub-Api-Version", "2022-11-28")
req.add_header("Content-Type",         "application/json")

try:
    with urllib.request.urlopen(req) as r:
        # 204 No Content = success
        print(f"FCC update workflow dispatched (HTTP {r.status})")
except urllib.error.HTTPError as e:
    body = e.read().decode(errors="replace")
    print(f"Dispatch failed: HTTP {e.code} — {body}", file=sys.stderr)
    sys.exit(1)
except Exception as e:
    print(f"Dispatch error: {e}", file=sys.stderr)
    sys.exit(1)
