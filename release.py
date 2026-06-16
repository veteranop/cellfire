#!/usr/bin/env python3
"""
release.py — Cellfire automated release pipeline
=================================================
1. Reads current version from build.gradle.kts
2. Bumps the version (default: last digit)
3. Writes new versionCode + versionName back to build.gradle.kts
4. Commits + pushes version bump to veteranop/cellfire
5. Builds the APK via Gradle
6. Renames APK to cellfire-app.apk
7. Creates a new GitHub release on veteranop/Cellfire_app_public
8. Uploads cellfire-app.apk to the release

Usage
-----
  python release.py              # bump last digit:  1.0.1.0 → 1.0.1.1
  python release.py --minor      # bump minor digit: 1.0.1.0 → 1.0.2.0
  python release.py --major      # bump major digit: 1.0.1.0 → 1.1.0.0
  python release.py --version 1.2.0.0   # explicit version
  python release.py --dry-run    # show what would happen, nothing executed

Requirements
------------
  gh CLI authenticated (gh auth status)
  Android SDK + Gradle wrapper (gradlew.bat) in project root
"""

import argparse
import base64
import datetime
import io
import json
import re
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path

# Force UTF-8 output so box-drawing characters don't crash on Windows cp1252
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
if sys.stderr.encoding and sys.stderr.encoding.lower() != "utf-8":
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# ── Config ────────────────────────────────────────────────────────────────────
PROJECT_ROOT   = Path(__file__).parent
GRADLE_FILE    = PROJECT_ROOT / "app" / "build.gradle.kts"
GRADLEW        = PROJECT_ROOT / "gradlew.bat"
APK_RENAME     = "cellfire-release.apk"
AAB_RENAME     = "cellfire-release.aab"
SOURCE_REPO    = "veteranop/cellfire"
RELEASE_REPO   = "veteranop/Cellfire_app_public"
GIT_BRANCH     = "1.0.0.7_Stable"

# Where Gradle puts the built artifacts
APK_DEBUG_PATH   = PROJECT_ROOT / "app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE_PATH = PROJECT_ROOT / "app/build/outputs/apk/release/app-release.apk"
AAB_RELEASE_PATH = PROJECT_ROOT / "app/build/outputs/bundle/release/app-release.aab"

# ── Java / JAVA_HOME resolution ───────────────────────────────────────────────
# Candidates in priority order: env var → Android Studio bundled JBR → IDEA JBR
_JBR_CANDIDATES = [
    Path(r"C:\Program Files\Android\Android Studio\jbr"),
    Path(r"C:\Program Files\Android\Android Studio\jre"),
    Path(r"C:\Program Files\JetBrains\IntelliJ IDEA\jbr"),
]

def _resolve_java_home() -> str | None:
    if jh := os.environ.get("JAVA_HOME"):
        return jh
    for p in _JBR_CANDIDATES:
        if (p / "bin" / "java.exe").exists():
            return str(p)
    return None

import os
_JAVA_HOME = _resolve_java_home()
if _JAVA_HOME:
    print(f"  JAVA_HOME -> {_JAVA_HOME}")
else:
    print("  WARNING: JAVA_HOME not found — Gradle may fail", file=sys.stderr)

# ── Helpers ───────────────────────────────────────────────────────────────────

def run(cmd: list[str], check=True, capture=False) -> subprocess.CompletedProcess:
    print(f"  $ {' '.join(str(c) for c in cmd)}")
    env = os.environ.copy()
    if _JAVA_HOME:
        env["JAVA_HOME"] = _JAVA_HOME
        env["PATH"] = str(Path(_JAVA_HOME) / "bin") + os.pathsep + env.get("PATH", "")
    return subprocess.run(
        cmd, check=check,
        capture_output=capture, text=True,
        cwd=PROJECT_ROOT, env=env
    )


def read_version() -> tuple[int, str]:
    """Returns (versionCode, versionName) from build.gradle.kts."""
    text = GRADLE_FILE.read_text(encoding="utf-8")
    code = re.search(r'versionCode\s*=\s*(\d+)', text)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    if not code or not name:
        sys.exit("ERROR: Could not parse versionCode/versionName from build.gradle.kts")
    return int(code.group(1)), name.group(1)


def bump_version(version_name: str, bump: str) -> tuple[int, str]:
    """
    Given a version like '1.0.1.0' and a bump type, returns the new
    (versionCode, versionName).
    bump: 'build' | 'patch' | 'minor' | 'major'
    """
    parts = [int(x) for x in version_name.split(".")]
    while len(parts) < 4:
        parts.append(0)

    if bump == "major":
        parts = [parts[0] + 1, 0, 0, 0]
    elif bump == "minor":
        parts = [parts[0], parts[1], parts[2] + 1, 0]
    elif bump == "patch":
        parts = [parts[0], parts[1], parts[2], parts[3] + 1]
    else:  # build (last digit)
        parts[3] += 1

    new_name = ".".join(str(p) for p in parts)
    # versionCode = all digits concatenated: 1.0.1.1 → 10011? No — just increment by 1
    current_code, _ = read_version()
    return current_code + 1, new_name


def write_version(new_code: int, new_name: str):
    text = GRADLE_FILE.read_text(encoding="utf-8")
    text = re.sub(r'(versionCode\s*=\s*)\d+',     f'\\g<1>{new_code}', text)
    text = re.sub(r'(versionName\s*=\s*)"[^"]+"', f'\\g<1>"{new_name}"', text)
    GRADLE_FILE.write_text(text, encoding="utf-8")


def find_apk() -> Path:
    """Returns path to the built APK — prefers release, falls back to debug."""
    if APK_RELEASE_PATH.exists():
        print(f"  Using release APK: {APK_RELEASE_PATH}")
        return APK_RELEASE_PATH
    if APK_DEBUG_PATH.exists():
        print(f"  Using debug APK: {APK_DEBUG_PATH}")
        return APK_DEBUG_PATH
    sys.exit("ERROR: No APK found. Gradle build may have failed.")


# ── GitHub doc helpers ───────────────────────────────────────────────────────

def _gh_get_file(repo: str, path: str) -> tuple[str, str]:
    """Fetch a file from GitHub. Returns (decoded_content, sha)."""
    result = run(["gh", "api", f"repos/{repo}/contents/{path}"], capture=True)
    data = json.loads(result.stdout)
    content = base64.b64decode(data["content"]).decode("utf-8")
    return content, data["sha"]


def _gh_put_file(repo: str, path: str, content: str, sha: str, message: str):
    """Push an updated file to GitHub via the Contents API."""
    encoded = base64.b64encode(content.encode("utf-8")).decode()
    run([
        "gh", "api", f"repos/{repo}/contents/{path}",
        "--method", "PUT",
        "-f", f"message={message}",
        "-f", f"content={encoded}",
        "-f", f"sha={sha}",
        "-f", "branch=main",
    ])


def _build_changelog_entry(version: str, notes: str) -> str:
    """Format a single CHANGELOG.md entry from version + raw notes string."""
    today = datetime.date.today().isoformat()
    lines = [l.strip() for l in notes.splitlines() if l.strip()]
    if not lines:
        lines = ["Bug fixes and improvements"]
    items = "\n".join(f"- {re.sub(r'^[-*•]\\s*', '', l)}" for l in lines)
    return f"## v{version} — {today}\n{items}\n"


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Cellfire automated release pipeline")
    group  = parser.add_mutually_exclusive_group()
    group.add_argument("--major",   action="store_true", help="Bump major digit")
    group.add_argument("--minor",   action="store_true", help="Bump minor digit")
    group.add_argument("--patch",   action="store_true", help="Bump patch digit")
    group.add_argument("--version", metavar="X.X.X.X",  help="Set explicit version")
    parser.add_argument("--dry-run", action="store_true", help="Show steps, do nothing")
    parser.add_argument("--debug-build", action="store_true",
                        help="Force debug build even if release signing is configured")
    parser.add_argument("--notes", default="", help="Release notes (optional)")
    args = parser.parse_args()

    dry = args.dry_run
    if dry:
        print("DRY RUN — nothing will be executed\n")

    # ── Step 1: Determine new version ─────────────────────────────────────────
    current_code, current_name = read_version()
    print(f"Current version: {current_name}  (code {current_code})")

    if args.version:
        parts = args.version.split(".")
        if len(parts) != 4 or not all(p.isdigit() for p in parts):
            sys.exit("ERROR: --version must be in X.X.X.X format (e.g. 1.2.0.0)")
        new_name = args.version
        new_code = current_code + 1
    else:
        bump = "major" if args.major else "minor" if args.minor else "patch" if args.patch else "build"
        new_code, new_name = bump_version(current_name, bump)

    tag = f"v{new_name}"
    print(f"New version    : {new_name}  (code {new_code})  tag={tag}\n")

    if dry:
        _dry_branch = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True, text=True, cwd=PROJECT_ROOT
        ).stdout.strip() or GIT_BRANCH
        print(f"Would update   : {GRADLE_FILE}")
        print(f"Would build    : {'debug' if args.debug_build else 'release (signed)'}")
        print(f"Would rename   : -> {APK_RENAME}")
        print(f"Would commit   : [{_dry_branch}] chore: release {new_name}")
        print(f"Would release  : {RELEASE_REPO} {tag}")
        print(f"Would upload   : {APK_RENAME}")
        return

    # ── Detect current branch (used later for push) ───────────────────────────
    _branch_result = subprocess.run(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"],
        capture_output=True, text=True, cwd=PROJECT_ROOT
    )
    current_branch = _branch_result.stdout.strip() or GIT_BRANCH

    # ── Step 1: Write new version ──────────────────────────────────────────────
    print("── Step 1: Updating version ──────────────────────────────────")
    write_version(new_code, new_name)
    print(f"  build.gradle.kts → versionCode={new_code}, versionName={new_name}")

    # ── Step 2: Build APK + AAB (before committing — don't litter git on failure) ───
    build_task = "assembleDebug" if args.debug_build else "assembleRelease bundleRelease"
    print(f"\n── Step 2: Building APK + AAB ────────────────────────────────")
    print(f"  (this takes a few minutes — output streams below)\n")

    build_result = run([str(GRADLEW)] + build_task.split() + ["--stacktrace"], check=False)
    if build_result.returncode != 0:
        # Revert build.gradle.kts so next run doesn't skip a version number
        write_version(current_code, current_name)
        sys.exit(f"\nBuild FAILED (exit {build_result.returncode}) — version reverted to {current_name}")

    # ── Step 3: Find + rename APK and AAB ────────────────────────────────────
    print("\n── Step 3: Renaming APK + AAB ────────────────────────────────")
    built_apk  = find_apk()
    output_apk = PROJECT_ROOT / APK_RENAME
    shutil.copy2(built_apk, output_apk)
    size_mb = output_apk.stat().st_size / 1024 / 1024
    print(f"  {built_apk.name} → {output_apk.name}  ({size_mb:.1f} MB)")

    output_aab = PROJECT_ROOT / AAB_RENAME
    if AAB_RELEASE_PATH.exists():
        shutil.copy2(AAB_RELEASE_PATH, output_aab)
        aab_mb = output_aab.stat().st_size / 1024 / 1024
        print(f"  {AAB_RELEASE_PATH.name} → {output_aab.name}  ({aab_mb:.1f} MB)")
    else:
        output_aab = None
        print(f"  WARNING: AAB not found at {AAB_RELEASE_PATH}", file=sys.stderr)

    # ── Step 4: Git commit + push version bump ────────────────────────────────
    print(f"\n── Step 4: Committing version bump → {current_branch} ────────────")
    run(["git", "add", "app/build.gradle.kts"])
    # --allow-empty guards against re-running after a partial success
    run(["git", "commit", "--allow-empty", "-m", f"chore: release {new_name}"])
    run(["git", "push", "origin", current_branch])

    # ── Step 5: Create GitHub release ────────────────────────────────────────
    print(f"\n── Step 5: Creating GitHub release {tag} ─────────────────────")

    # Check if this tag already exists (e.g. from a previous partial run)
    tag_check = run(["gh", "release", "view", tag, "--repo", RELEASE_REPO],
                     check=False, capture=True)
    if tag_check.returncode == 0:
        print(f"  Tag {tag} already exists — deleting stale release first")
        run(["gh", "release", "delete", tag, "--repo", RELEASE_REPO, "--yes"], check=False)
        # Also delete the git tag on the remote so we can recreate it
        run(["git", "push", "--delete", f"https://github.com/{RELEASE_REPO}.git", tag], check=False)

    notes = args.notes or f"Cellfire {new_name}"
    run([
        "gh", "release", "create", tag,
        "--repo",  RELEASE_REPO,
        "--title", new_name,
        "--notes", notes,
    ])

    # ── Step 6: Upload APK + AAB ─────────────────────────────────────────────
    print(f"\n── Step 6: Uploading APK + AAB ───────────────────────────────")
    assets = [str(output_apk)]
    if output_aab:
        assets.append(str(output_aab))
    run([
        "gh", "release", "upload", tag,
        *assets,
        "--repo", RELEASE_REPO,
    ])
    print(f"  Uploaded: {', '.join(Path(a).name for a in assets)}")

    # ── Step 7: Update website version strings ───────────────────────────────
    print(f"\n── Step 7: Updating website version strings ──────────────────────")
    _admin_url = "http://localhost:5050/api/site/update_app_version"
    try:
        _payload = {"version": new_name}
        if args.notes:
            _payload["notes"] = args.notes
        _req = urllib.request.Request(
            _admin_url,
            data=json.dumps(_payload).encode(),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(_req, timeout=20) as _r:
            _result = json.loads(_r.read())
        if _result.get("ok"):
            for _f, _r in _result.get("results", {}).items():
                print(f"  {_f}: {_r.get('message','ok')}")
        else:
            print(f"  WARNING: Some files failed:")
            for _f, _r in _result.get("results", {}).items():
                status = "ok" if _r.get("ok") else f"FAILED — {_r.get('error')}"
                print(f"    {_f}: {status}", file=sys.stderr if not _r.get("ok") else sys.stdout)
    except Exception as _e:
        print(f"  WARNING: Could not reach Admin Tool at {_admin_url}")
        print(f"  Is the Admin Tool running? Start it with: python cellfire_admin.py")
        print(f"  Then manually run: POST {_admin_url} {{\"version\": \"{new_name}\"}}")
        print(f"  Error: {_e}", file=sys.stderr)

    # ── Step 8: Update public repo docs (CHANGELOG + README badge) ──────────
    print(f"\n── Step 8: Updating public repo docs ────────────────────────────")
    try:
        # 8a — CHANGELOG.md: prepend new entry after the header separator
        cl_content, cl_sha = _gh_get_file(RELEASE_REPO, "CHANGELOG.md")
        new_entry = _build_changelog_entry(new_name, notes)
        sep = "\n---\n"
        idx = cl_content.find(sep)
        if idx != -1:
            updated_cl = cl_content[:idx + len(sep)] + "\n" + new_entry + "\n" + cl_content[idx + len(sep):]
        else:
            updated_cl = new_entry + "\n" + cl_content
        _gh_put_file(RELEASE_REPO, "CHANGELOG.md", updated_cl, cl_sha,
                     f"docs: changelog for {new_name}")
        print(f"  CHANGELOG.md — prepended v{new_name} entry")
    except Exception as e:
        print(f"  WARNING: CHANGELOG.md update failed: {e}", file=sys.stderr)

    try:
        # 8b — README.md: update version badge
        rm_content, rm_sha = _gh_get_file(RELEASE_REPO, "README.md")
        updated_rm = re.sub(
            r"(Download%20APK-v)[^-]+-",
            f"\\g<1>{new_name}-",
            rm_content,
        )
        if updated_rm != rm_content:
            _gh_put_file(RELEASE_REPO, "README.md", updated_rm, rm_sha,
                         f"docs: update README badge to v{new_name}")
            print(f"  README.md — badge updated to v{new_name}")
        else:
            print(f"  README.md — badge pattern not found, skipped")
    except Exception as e:
        print(f"  WARNING: README.md update failed: {e}", file=sys.stderr)

    print(f"""
══════════════════════════════════════════════════════
  Release complete!
  Version : {new_name}  (code {new_code})
  Tag     : {tag}
  Repo    : https://github.com/{RELEASE_REPO}/releases/tag/{tag}
══════════════════════════════════════════════════════
""")


if __name__ == "__main__":
    main()
