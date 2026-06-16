#!/usr/bin/env python3
"""Prepend mod_security exclusions to server .htaccess for Cellfire API."""
import json, urllib.request, urllib.parse, ssl, os, base64
import importlib.util

spec = importlib.util.spec_from_file_location("cfg", "push_rules_config.py")
cfg = importlib.util.module_from_spec(spec); spec.loader.exec_module(cfg)
API_TOKEN = cfg.API_TOKEN

ctx = ssl.create_default_context()
ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE

CPANEL_HOST = "just2029.justhost.com"
CPANEL_PORT = 2083
CPANEL_USER = "veterap2"
WEB_ROOT    = "/home1/veterap2/public_html/website_9695cd55"

def cpanel_get(module, func, params):
    qs  = urllib.parse.urlencode(params)
    url = f"https://{CPANEL_HOST}:{CPANEL_PORT}/execute/{module}/{func}?{qs}"
    req = urllib.request.Request(url, headers={"Authorization": f"cpanel {CPANEL_USER}:{API_TOKEN}"})
    with urllib.request.urlopen(req, context=ctx) as r:
        return json.loads(r.read())

def cpanel_post(module, func, params):
    body = urllib.parse.urlencode(params).encode()
    url  = f"https://{CPANEL_HOST}:{CPANEL_PORT}/execute/{module}/{func}"
    req  = urllib.request.Request(url, data=body, method="POST",
           headers={"Authorization": f"cpanel {CPANEL_USER}:{API_TOKEN}",
                    "Content-Type":  "application/x-www-form-urlencoded"})
    with urllib.request.urlopen(req, context=ctx) as r:
        return json.loads(r.read())

# --- Read current .htaccess ---
result  = cpanel_get("Fileman", "get_file_content", {"dir": WEB_ROOT, "file": ".htaccess"})
current = result["data"]["content"]
print(f"Read .htaccess — {len(current)} chars")

# --- Build new content ---
MODSEC_BLOCK = (
    "## -- Mod_Security exclusions for Cellfire API (Stripe + auth endpoints) --\n"
    "<IfModule mod_security.c>\n"
    "  <If \"%{QUERY_STRING} =~ /com_cellfireapi/\">\n"
    "    SecRuleEngine Off\n"
    "  </If>\n"
    "</IfModule>\n"
    "\n"
    "<IfModule mod_security2.c>\n"
    "  <If \"%{QUERY_STRING} =~ /com_cellfireapi/\">\n"
    "    SecRuleEngine Off\n"
    "  </If>\n"
    "</IfModule>\n"
    "\n"
)

if "com_cellfireapi" in current:
    print("Exclusion already present — skipping.")
else:
    new_content = MODSEC_BLOCK + current
    encoded = base64.b64encode(new_content.encode("utf-8")).decode("ascii")
    res = cpanel_post("Fileman", "save_file_content", {
        "dir":      WEB_ROOT,
        "file":     ".htaccess",
        "content":  new_content,
        "encoding": "utf-8"
    })
    print("Upload result:", json.dumps(res))
    print(f"New .htaccess: {len(new_content)} chars")
