#!/usr/bin/env python3
"""Clear legacy Joomla activation values for existing Cellfire licensed users."""
import subprocess, sys

sql = """UPDATE josbf_users u
INNER JOIN josbf_cellfire_licenses l ON l.user_id = u.id
SET u.activation = ''
WHERE u.activation != ''
  AND u.activation NOT REGEXP '^[a-f0-9]{64}$'"""

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
    print(f"Error: {e}")
