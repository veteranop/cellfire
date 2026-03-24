# Cellfire — The Wireless Warlord Scanner
**Real-time, all-carrier LTE & 5G NR cell tower scanner for Android**
See every tower around you — T-Mobile, Verizon, AT&T, FirstNet, Dish, US Cellular — with accurate band, RSRP, SINR, PCI, and carrier name. No root required.

**Cellfire does what no other free app on the Play Store does in 2025.**

| Feature                                      | Cellfire    | CellMapper  | Network Cell Info | SignalCheck |
|----------------------------------------------|-------------|-------------|-------------------|-------------|
| Real-time all-carrier neighbor cell display  | ✅ Yes       | ✅ Yes*      | ✅ Yes*            | ✅ Yes*      |
| Accurate band (n71, n41, B66, B13, etc.)     | ✅ Yes       | ✅ Yes       | ✅ Yes             | ✅ Yes       |
| Carrier name without uploading your data     | ✅ Yes       | ❌ No        | ❌ No              | ❌ No        |
| Confidence scoring per tower                 | ✅ Yes       | ❌ No        | ❌ No              | ❌ No        |
| Drive test mode + CSV export                 | ✅ Yes       | ❌ No        | 💲 Paid            | 💲 Paid      |
| Free                                         | ✅ Yes       | ✅ Yes       | 💲 Paid features  | 💲 Paid      |

\* Requires data upload or paid version to see full carrier/neighbor detail

<img width="1080" height="2424" alt="image" src="https://github.com/user-attachments/assets/676cda2f-fae3-4ddf-b1f4-e5939b30d4da" />
<img width="1080" height="2424" alt="image" src="https://github.com/user-attachments/assets/5e52eb97-cd60-4228-9ce4-76664df3e263" />
<img width="1080" height="2424" alt="image" src="https://github.com/user-attachments/assets/ec5a098c-b1a5-4a5c-923c-2c213dcccbda" />
<img width="1080" height="2424" alt="image" src="https://github.com/user-attachments/assets/653a0480-71f4-4829-8d1c-4a67f3acf77b" />

---

## Why Cellfire Exists

Every other app either:
- Hides neighbor cells behind a paywall or upload requirement
- Requires you to submit your data before you can see anything useful
- Can't tell you the carrier name without a live internet lookup
- Costs money for the features that matter

**Cellfire does none of that.**
Exclusive spectrum bands (B13 → Verizon, B71 → T-Mobile, B14 → FirstNet) resolve instantly with no lookup at all. A crowd-sourced tile database cached on your device handles everything else — downloaded once, works offline. You own the spectrum. You see everything.

---

## Download

<p align="center">
  <a href="https://github.com/veteranop/Cellfire_app_public/releases/latest/download/cellfire-release.apk">
    <img src="https://img.shields.io/badge/Download%20APK-Android-green?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://cellfire.io">
    <img src="https://img.shields.io/badge/More%20at-cellfire.io-orange?style=for-the-badge" alt="cellfire.io"/>
  </a>
</p>

> **Sideload instructions:** Go to **Settings → Apps → Special app access → Install unknown apps**, enable your browser or file manager, then open the downloaded APK.
> Updates can be checked from within the app: **Settings → App Update**.

---

## Features

### Real-Time Cell Scanning
- Scans all visible LTE and 5G NR towers simultaneously — not just the one you're connected to
- Reports PCI, EARFCN/NR-ARFCN, band, RSRP, RSRQ, SINR, channel bandwidth, and TAC
- **Deep scan mode** — extended 35-second cycle for thorough neighbor cell detection
- Background foreground service with automatic 5-second polling
- Supports LTE, NR (5G), WCDMA, and GSM

### Carrier Resolution — No Upload Required
Cellfire resolves the carrier for every visible tower through a layered chain:

1. **Exclusive band** — B13 is always Verizon. B71/B41/B26 are always T-Mobile. B14 is always FirstNet. Resolves instantly, no data needed.
2. **Community tile database** — a crowd-sourced database cached on your device identifies towers by PCI + TAC fingerprint, scored by how many users have confirmed them.
3. **PCI inference** — statistical fallback for towers with no database record.

Signal rules are fetched live from cellfire.io — carrier detection improves without an app update.

### Community Cell Database
- Downloads crowd-sourced tower tiles for your area (~80-mile radius, ~49 tiles)
- Each tile covers a ~30 × 30 mile grid square, gzip-compressed and cached on-device
- Tiles refresh as you move; unchanged tiles use conditional HTTP requests to save data
- Each record carries a **confidence score (0–100)** based on source quality and observation count
- Opt-in: confirmed observations are anonymously contributed back to improve detection for everyone

### Drive Test Mode
- Records all visible cells with GPS coordinates and full signal history over time
- Export to CSV for offline analysis or import into mapping tools
- Visualize per-tower RSRP/RSRQ/SINR trends within the app

### PCI Discovery Tracker
- Tracks every unique Physical Cell ID seen during a session
- Shows total discovered count on the main screen
- Bulk upload discovered cells from Settings to seed new coverage areas

---

## Understanding the Dot Colors

Each cell row shows a colored dot indicating carrier identification confidence:

| Dot | Color | Confidence | Meaning |
|-----|-------|-----------|---------|
| ● | **Blue (gold border)** | — | **Your registered serving cell** — modem confirmed, actively camped. |
| ● | **Dark green** | 100 | **Verified** — a registered user was actually camped on this cell. Highest trust. |
| ● | **Green** | 75–99 | **High confidence** — strong crowd-sourced match or exclusive band. Reliable. |
| ● | **Yellow** | 40–74 | **Medium confidence** — limited observations. Probably correct. |
| ● | **Red** | < 40 | **Low confidence** — PCI range heuristic only. Approximate. |
| ● | **Grey** | — | **No record** — tower not in any loaded tile. May be new or in sparse area. |

> The gold border marks your serving cell regardless of dot color — a serving cell can be dark green (verified) or grey (not yet in the database).

---

## Signal Measurements

| Field | Description |
|-------|-------------|
| **RSRP** | Reference Signal Received Power (dBm). Raw signal strength. ≥ −85 good · −85 to −100 fair · < −100 weak |
| **RSRQ** | Reference Signal Received Quality (dB). Signal quality relative to interference. ≥ −10 good |
| **SINR** | Signal-to-Interference-plus-Noise Ratio (dB). Best indicator of throughput potential |
| **Band** | LTE or NR frequency band (e.g. B66 = AWS 1700/2100 MHz · B12 = 700 MHz · n71 = 600 MHz NR) |
| **EARFCN** | Absolute Radio Frequency Channel Number — the exact frequency channel within a band |
| **BW** | Channel bandwidth in MHz. Wider = higher theoretical peak data rates |
| **PCI** | Physical Cell Identity (0–503). The radio-layer ID broadcast by every tower |
| **TAC** | Tracking Area Code. Carrier-assigned regional identifier |

---

## Carrier Color Coding

| Carrier | Row Color |
|---------|-----------|
| T-Mobile | Magenta |
| AT&T | Blue |
| Verizon | Red |
| US Cellular | Purple |
| FirstNet / AT&T | Dark |
| Dish Wireless (Boost) | Orange |

---

## Privacy

- No account required
- Crowdsourcing is **opt-in** — disable at any time: **Settings → Upload cell data**
- Uploaded observations contain only radio metadata (PCI, TAC, band, approximate location) — never IMEI, IMSI, phone number, or personal identifiers
- Location is approximate (tile-level, ~30 × 30 mile grid) — not precise GPS

---

## Permissions

| Permission | Why it's needed |
|-----------|----------------|
| `ACCESS_FINE_LOCATION` | Android requires precise location to access radio/cell info via TelephonyManager |
| `READ_PHONE_STATE` | Required to call `getAllCellInfo()` and read signal measurements |
| `FOREGROUND_SERVICE` | Keeps the scan running while the app is in the background |
| `INTERNET` | Downloads tile database, fetches signal rule updates, uploads observations (opt-in) |
| `REQUEST_INSTALL_PACKAGES` | Required for the in-app update installer |

---

## Requirements

- Android 10+ (Android 12+ recommended for best neighbor cell data)
- Location services enabled
- A device with Qualcomm or similar modem for full neighbor cell EARFCN reporting

---

## Installation

Download the latest APK from [Releases](https://github.com/veteranop/Cellfire_app_public/releases/latest).

This is the public release repository. Source is not included.

---

## Settings Reference

| Setting | Description |
|---------|-------------|
| **Upload cell data** | Enables anonymous crowd-sourcing of observed cells to improve the database |
| **Signal Rules** | View the active signal rule version and force a refresh from the server |
| **App Update** | Check for a newer APK on GitHub Releases and install it |
| **Export CSV** | Export all drive test data and discovered PCIs to a file |
| **Clear All Data** | Wipe local scan history and tile cache |

---

<p align="center">
  <a href="https://cellfire.io">cellfire.io</a> &nbsp;·&nbsp;
  <a href="https://cellfire.io/privacy-policy">Privacy Policy</a>
</p>

<p align="center">© 2026 Cellfire. All rights reserved.</p>
