#!/usr/bin/env python3
"""
Two fixes for the activation check:
1. Patch auth.php login() to only block on our 64-char hex tokens (not old Joomla activation values)
2. Run a migration clearing activation for all existing licensed users so they're unblocked
"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import push_component_files as pc
import urllib.request, urllib.parse, json

COMPONENT = pc.COMPONENT
SITE_ROOT = pc.SITE_ROOT

# ── Fix 1: auth.php ── narrow the activation check ────────────────────────────
auth_php = open(os.path.join(os.path.dirname(__file__), 'auth_updated.php'),
                encoding='utf-8').read()

old_check = (
    "        // Check email verification before anything else — gives a clear actionable message\n"
    "        if (!empty($user->activation)) {"
)
new_check = (
    "        // Check email verification before anything else — gives a clear actionable message\n"
    "        // Only block on OUR 64-char hex tokens — Joomla stores other values in this field\n"
    "        // that existing users may have from old registrations.\n"
    "        if (!empty($user->activation) && preg_match('/^[a-f0-9]{64}$/', $user->activation)) {"
)

patched_auth = auth_php.replace(old_check, new_check)
if patched_auth == auth_php:
    print("  [WARN] Could not find exact string in auth_updated.php — checking deployed version")
    # Fall back: write the patch inline
    patched_auth = auth_php.replace(
        "if (!empty($user->activation)) {",
        "if (!empty($user->activation) && preg_match('/^[a-f0-9]{64}$/', $user->activation)) {"
    )

print("1. Patching auth.php login check ...")
ok, msg = pc.cpanel_save(f"{COMPONENT}/controllers", "auth.php", patched_auth)
print(f"   [{'OK' if ok else 'FAIL'}] {msg}")
if ok:
    # Keep local copy in sync
    open(os.path.join(os.path.dirname(__file__), 'auth_updated.php'), 'w', encoding='utf-8').write(patched_auth)

# ── Fix 2: migration ── clear activation for existing licensed users ───────────
MIGRATION_SQL = """
UPDATE josbf_users u
INNER JOIN josbf_cellfire_licenses l ON l.user_id = u.id
SET u.activation = ''
WHERE u.activation != ''
  AND u.activation NOT REGEXP '^[a-f0-9]{64}$'
"""
# Deploy as a runner script
migration_script = f'''#!/usr/bin/env python3
"""Clear legacy Joomla activation values for existing Cellfire licensed users."""
import subprocess, sys

sql = """{MIGRATION_SQL.strip()}"""

# Use mysql CLI if available
try:
    result = subprocess.run(
        ["mysql", "-u", "veterap2_joom819", "-pB1S!wp4K)6", "veterap2_joom819", "-e", sql],
        capture_output=True, text=True, timeout=30
    )
    print("STDOUT:", result.stdout)
    print("STDERR:", result.stderr)
    print("Return code:", result.returncode)
    if result.returncode == 0:
        print("Migration complete.")
    else:
        print("Migration failed.")
except Exception as e:
    print(f"Error: {{e}}")
'''

mig_path = os.path.join(os.path.dirname(__file__), 'migrate_activation.py')
open(mig_path, 'w', encoding='utf-8').write(migration_script)

print("2. Deploying migration script ...")
ok2, msg2 = pc.cpanel_save('/home1/veterap2', 'migrate_activation.py', migration_script)
print(f"   [{'OK' if ok2 else 'FAIL'}] {msg2}")
