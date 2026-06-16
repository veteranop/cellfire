"""
setup_deps.py — Install server-side Python dependencies for Cellfire.
Run once via the admin tool Scripts tab after deploying to the server.
"""
import subprocess
import sys

PACKAGES = [
    "firebase-admin",
    "requests",
]

print(f"Python: {sys.executable}  ({sys.version})")
print(f"Installing: {', '.join(PACKAGES)}\n")

result = subprocess.run(
    [sys.executable, "-m", "pip", "install", "--user", "--upgrade"] + PACKAGES,
    capture_output=True,
    text=True,
)

print(result.stdout)
if result.stderr:
    print("STDERR:", result.stderr)

if result.returncode == 0:
    print("\nDone. Verifying imports...")
    for pkg in ["firebase_admin", "requests"]:
        try:
            __import__(pkg)
            print(f"  OK  {pkg}")
        except ImportError as e:
            print(f"  FAIL {pkg}: {e}")
else:
    print(f"\nPip exited with code {result.returncode}")
