# Cellfire Project — Claude Instructions

## Ecosystem Overview
Cellfire is a **unified product ecosystem** consisting of three components that share infrastructure and will eventually interoperate:
1. **Cellfire App** — Android app (`com.veteranop.cellfire`), this repo
2. **Cellfire RF Studio** — Desktop app, separate repo
3. **Cellfire Web Viewer** — Browser-based viewer, separate repo

Change control, versioning, and release logging should be tracked across all three as one ecosystem, even when working on only one component at a time.

---

## Android App

### Package / Architecture
- Package: `com.veteranop.cellfire`
- Scans cellular towers via `TelephonyManager`
- Key files:
  - `CellScanService.kt` — foreground service, 5s poll + 35s deep scan
  - `CarrierResolver.kt` — EARFCN/band lookup + UnwiredLabs API fallback
  - `CellfireDbManager.kt` — grid tile downloader + PCI+TAC lookup
  - `CellRepository.kt` — Room DB, state flows, CSV export
  - `DataModels.kt` — LteCell, NrCell, WcdmaCell, GsmCell
  - Assets: `band_carrier_lookup.json`, `pci_carrier_data.json`, `earfcn_frequencies.json`

### Carrier Resolution Chain (LTE priority order)
1. `operatorAlphaLong/Short` — direct from TelephonyManager
2. `plmnToCarrier(mcc, mnc)` — hardcoded PLMN table
3. `CellfireDbManager.lookupByPciTac(pci, tac)` — tile DB lookup
4. `pciToCarrier(pci)` — PCI range fallback (least reliable)

### Grid Tile System (CellfireDbManager)
- Server: `https://cellfire.io/files/cellfire-db/`
- Format: `grid_p{lat}_{lon}.json.gz` — gzip compressed JSON arrays
- Tile size: 0.5° lat/lon; download radius: 80 miles (~49 tiles max)
- Lookup key: `(pci, tac)` packed into Long; higher sample count wins on conflict
- Data sourced from OpenCellID

### Crowd-Sourcing Pipeline
- App → Firebase: `CrowdsourceReporter.kt`
  - Path: `/observations/{tileKey}/{pci}_{tac}`
  - Requires valid GPS + pci > 0; TAC=0 and Unknown carrier allowed
  - Source quality: alpha(5) > plmn(4) > exclusive_band(3) = fcc_band(3) > db(2) > pci_range(1)
- Firebase → tiles: `merge_observations.py` (project root)
  - Run via cron when SSH is available; needs `serviceAccountKey.json` + `FIREBASE_DATABASE_URL`

### Signal Rules
- Asset: `app/src/main/assets/band_carrier_lookup.json`
- Live URL: `https://cellfire.io/files/cellfire-db/band_carrier_lookup.json`
- Exclusive bands: 13→VZW, 14→FirstNet, 41→TMo, 71→TMo, 260→VZW, 261→TMo, 29→Dish, 70→Dish
- Resolution order: exclusive_band → fcc_band → db → pciOnly → pci_range

### In-App Updater
- `AppUpdater.kt` checks GitHub Releases API → downloads APK → fires system installer
- GitHub API: `https://api.github.com/repos/veteranop/Cellfire_app_public/releases/latest`
- Compares `tag_name` (e.g. "v1.0.1.7") to `BuildConfig.VERSION_NAME`

---

## Release Process — ALWAYS FOLLOW THIS

Use the **Release tab** in the Admin Tool (`localhost:5050`) or run `release.py` directly. Never manually edit versions or push APKs outside this pipeline.

### What `release.py` does automatically:
1. Reads `versionCode` + `versionName` from `app/build.gradle.kts`
2. Bumps the version (`--patch` default, or `--minor` / `--major` / `--version X.X.X.X`)
3. Writes new version back to `build.gradle.kts`
4. Builds signed APK via `gradlew.bat assembleRelease` (keystore at `C:\cellfire_ks\keystore.properties`)
5. Copies APK → `cellfire-release.apk` in project root
6. Git commits version bump + pushes to current branch on `veteranop/cellfire`
7. Creates GitHub Release on `veteranop/Cellfire_app_public` with tag `vX.X.X.X`
8. Uploads `cellfire-release.apk` to the release

### What must be done AFTER release.py completes:
9. **Update website** — push new version string to cellfire.io via the Admin Tool (Code tab or API)
10. **Post Slack recap** — use Admin Tool → `POST /api/push_slack_report` or the release tab's Slack button

### Current version: `1.0.1.22` (versionCode 32)
### APK filename: `cellfire-release.apk` (Admin Tool Release tab monitors `cellfire-app.apk` — minor naming gap)
### Repos: source = `veteranop/cellfire`, public releases = `veteranop/Cellfire_app_public`

Version must stay in sync across: app binary, GitHub Releases, and cellfire.io website.

---

## cellfire.io Server Management — USE THE ADMIN TOOL

All website changes, PHP edits, script execution, user management, and file operations on cellfire.io **must go through the Cellfire Admin Tool**. This is the primary interface for all server interaction — do not attempt manual file edits or direct cPanel calls outside of it.

### What the Admin Tool is
- **File**: `cellfire_admin.py` at project root
- **Launch**: Run from conda prompt — `python cellfire_admin.py` → `http://localhost:5050` (use conda, not `run_admin.bat`, to avoid Windows cp1252 emoji encoding errors)
- **Config**: `push_rules_config.py` (API_TOKEN, RUNNER_SECRET, SLACK_WEBHOOK_URL, etc.)
- **Stack**: Flask local server + cPanel UAPI (file ops) + PHP runner on server (script/cron exec) + cf_users_api.php (user management)
- **Direct cPanel push**: When deploying arbitrary files outside the Code tab's allowlist, use `_cpanel_save()` pattern directly via Python — see `push_rules_config.py` for credentials

### Admin Tool Tabs & Capabilities

| Tab | What it does |
|-----|-------------|
| **Rules** | Edit signal rule JSON files locally (`band_carrier_lookup.json`, etc.) + push to server via cPanel |
| **Database** | Leaflet map of local tile cache, PCI browser, Firebase pending obs stats, sync tiles from server |
| **Scripts** | Deploy `.py` scripts to server, run them via PHP runner, view execution logs; **GitHub Actions** section with "▶ Run FCC Band Update" button + live status/link to run |
| **Jobs** | List/add/remove server cron jobs via PHP runner (`crontab` on the server) |
| **Users** | List/search/edit Cellfire users via `cf_users_api.php` (toggle_active, set_plan, extend_trial, block, create, clear_mac); shows Last Login (`lastvisitDate`) column |
| **Media** | Browse/upload/delete files in `cellfire.io/images/` via cPanel Fileman |
| **Code** | Fetch + edit + push site files: `template.css`, `template.js`, `index.php`, `cf_app.php`, `cf_studio.php`, `cf_viewer.php`, `cf_users_api.php` |
| **Settings** | Configure Slack signup webhook |
| **Release** | Show current vs GitHub version, APK status, trigger `release.py` with bump level + release notes, streamed output |

### Key API endpoints (called from Claude via the running local server)
- `POST /api/push` — push a signal rules JSON to server
- `POST /api/run_script` — run a whitelisted Python script on the server
- `POST /api/deploy_script` — upload a local .py to server scripts dir
- `GET/POST /api/cron_*` — manage server cron jobs
- `GET/POST /api/users*` — user management
- `POST /api/site/push` — push a site code file (CSS/JS/PHP) to server
- `POST /api/push_slack_report` — post status report to #cellfire Slack
- `GET /api/release/status` — current version, GitHub release, APK info
- `POST /api/release/build` — trigger release.py (SSE stream)
- `POST /api/fcc/trigger` — dispatch `fcc-band-update.yml` workflow via `gh` CLI
- `GET /api/fcc/status` — latest run status/conclusion/URL from GitHub Actions

### Server topology
- cPanel host: `just2029.justhost.com:2083`
- Server root: `/home1/veterap2/public_html/website_9695cd55/`
- DB files: `.../files/cellfire-db/`
- PHP runner: `cellfire_runner.php` deployed to site root
- Users API: `https://cellfire.io/cf_users_api.php`
- CMS: Joomla 6.0.3 — `com_cellfireapi` = Studio license manager (**do not touch**)

### Page content architecture — IMPORTANT

All site page content lives in **Joomla article layout override PHP files**, NOT in the Joomla DB (`josbf_content.introtext` is empty for every page except Pricing).

- Override files location: `templates/cellfire/html/com_content/article/`
- `default.php` is a **router** that detects the active menu item alias and auto-loads the matching `cf_*.php` layout — no manual layout selection needed in the Joomla article editor
- To add a new page: create `cf_{alias}.php` in the overrides directory

Every page file is editable via the Admin Tool Code tab **and** has a local mirror in `site_template/`.

| File | URL | Notes |
|------|-----|-------|
| `default.php` | (router) | Auto-loads layout by alias — edit carefully |
| `cf_app.php` | `/app` | App marketing page |
| `cf_studio.php` | `/studio` | RF Studio marketing page |
| `cf_viewer.php` | `/viewer` | Viewer marketing page |
| `cf_pricing.php` | `/pricing` | Pricing page (35 KB — largest) |
| `cf_about.php` | `/about` | About page |
| `cf_downloads.php` | `/downloads` | Downloads page |
| `cf_account.php` | `/account` | Account/login page |
| `cf_register.php` | `/register` | Registration page |
| `cf_privacy.php` | `/privacy` | Privacy Policy |
| `cf_terms.php` | `/terms` | Terms of Service |
| `template.css` | — | Main stylesheet |
| `template.js` | — | Main JS (injected behaviors) |
| `index.php` | — | Joomla template shell |
| `cf_users_api.php` | (site root) | User/license management API |
| `verify_email.php` | (site root) | Email verification landing page |

### Local site_template folder
- `site_template/` mirrors **all** deployed page files for local editing — every file in the table above has a local copy here
- Use `POST /api/site/push_local` to push from local folder, or `POST /api/site/push` with content

### template.js version / cache busting
- Joomla WAM generates a static hash (`?95f60e` style) from the file path — does **not** change on file update
- To force browser cache bust: increment the `'version'` option in `index.php` line 20:
  `$wa->registerAndUseScript('template.cellfire', ..., ['defer' => true, 'version' => '1.1']);`
- Current version string: `'1.1'`

### Homepage map (cellfire.io)
- The coverage map JS (`cfmSelectTile`, `cfmRenderDetail`, tile loading, panel rendering) is embedded in `index.php` (the Joomla template shell), inside the `<?php if ($isHome) :?>` block — NOT in a separate article or DB field
- The panel search filter (PCI / TAC / carrier / band) is injected via `template.js` using a MutationObserver on `#cfm-panel-body`

---

## Release Notifications — Slack

After every release (any component — app, RF Studio, or web viewer), post a recap to the Cellfire Slack channel including:
- Component name and new version number
- Summary of what changed (features, fixes, known issues)
- Link to GitHub Release or relevant artifact

This is the single source of truth for tracking what changed and when across the ecosystem.

---

## Change Control — All Three Products

Even when only one component is being updated, treat version and change tracking as ecosystem-wide:
- Log changes to the Slack channel for all three products
- GitHub Release notes should reference cross-component implications where relevant
- If a change in one product will require a matching change in another, flag it explicitly

---

## FCC Band License Map Automation

`band_license_map.json` is regenerated weekly from FCC ULS bulk data and pushed to both the repo and cellfire.io server automatically.

### Pipeline
```
Server cron (Mon 6 AM UTC)
  → run_fcc_update.py           dispatches workflow_dispatch via GitHub REST API
  → fcc-band-update.yml         runs on GitHub Actions (ubuntu-latest)
      ├─ Downloads l_cell.zip + l_market.zip from data.fcc.gov
      ├─ Downloads auction CSVs (107/108/110) from auctiondata.fcc.gov
      ├─ Runs build_band_license_map.py
      ├─ Diffs vs. previous JSON
      ├─ [if changed] Commits to repo + pushes to cellfire.io via cPanel API
      └─ [always] Posts Slack notification with added/removed/updated state summary
```

### Key files
- `build_band_license_map.py` — multi-source builder (Part 22/27 + auction CSVs)
- `run_fcc_update.py` — dispatch script deployed to server; called by cron
- `.github/workflows/fcc-band-update.yml` — GitHub Actions workflow
- `.fcc_cache/` — local cache dir for auction CSVs (gitignored)

### GitHub Actions secrets required
- `CPANEL_TOKEN` — cPanel API token for pushing updated JSON to cellfire.io
- `SLACK_WEBHOOK` — incoming webhook URL for `#cellfire` notifications

### Server cron entry
```
0 6 * * 1  cd /home1/veterap2 && /usr/bin/env python3 run_fcc_update.py >> /home1/veterap2/runner_logs/run_fcc_update.py.log 2>&1
```
Managed via Admin Tool → Jobs tab. Trigger manually via Admin Tool → Scripts tab → "▶ Run FCC Band Update".

---

## Infrastructure Notes
- Firebase Realtime DB: rules require anonymous auth for writes, reads are blocked — reasonable for public launch
- `merge_observations.py` runs via server cron every 19 minutes at `/home1/veterap2/cellfire/` using its own venv
- No FTP/SSH access — all server ops go through the Admin Tool (cPanel API + PHP runner)

## Active TODOs
- [x] FCC band license map automation (GitHub Actions + server cron + Slack reporting) ✓
- [x] Firebase Crashlytics — confirmed working (100% crash-free, data flowing) ✓
- [x] Deploy `merge_observations.py` via cron ✓ (running every 19 min)
- [ ] Play Store submission — privacy policy updated, Data Safety form still needs to be filled in Play Console
- [x] Push `README_public.md` to `github.com/veteranop/Cellfire_app_public` ✓ (also added `CHANGELOG.md`)
- [ ] Complete `com_cellfireapp` Signal Rules admin module (Joomla backend)

See `FUTURE.md` for non-urgent backlog items.
