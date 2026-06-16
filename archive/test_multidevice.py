#!/usr/bin/env python3
"""
Test that list_devices works for markdj's account.
Also verifies the devices table was created on first enterprise login.
"""
import sys, os, json, urllib.request, urllib.parse

sys.path.insert(0, os.path.dirname(__file__))
import push_rules_config as cfg

USERS_API = "https://cellfire.io/cf_users_api.php"
SECRET    = cfg.RUNNER_SECRET

def api(action, **kwargs):
    params = {"secret": SECRET, "action": action, **kwargs}
    data   = urllib.parse.urlencode(params).encode()
    req    = urllib.request.Request(USERS_API, data=data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read())

# Get markdj's user_id
print("=== Finding markdj ===")
result = api("list", search="markdj", limit=5)
markdj = next((u for u in result["users"] if u["username"] == "markdj"), None)
if not markdj:
    print("User markdj not found!")
    sys.exit(1)

uid = markdj["id"]
print(f"user_id: {uid}, plan: {markdj['plan_type']}, active: {markdj['is_active']}")
print(f"mac_address: {markdj.get('mac_address', '(none)')}")

print("\n=== Checking devices (list_devices) ===")
result = api("list_devices", user_id=uid)
print(f"Response: {json.dumps(result, indent=2)}")
