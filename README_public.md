# Cellfire — The Wireless Warlord Scanner
**Real-time, all-carrier LTE & 5G NR cell tower scanner for Android**
See every tower around you — T-Mobile, Verizon, AT&T, FirstNet, Dish, US Cellular — with accurate band, RSRP, SINR, PCI, and carrier name. No root required.

**Cellfire does what no other free app on the Play Store does.**

| Feature                                      | Cellfire    | CellMapper  | Network Cell Info | SignalCheck |
|----------------------------------------------|-------------|-------------|-------------------|-------------|
| Real-time all-carrier neighbor cell display  | ✅ Yes       | ✅ Yes*      | ✅ Yes*            | ✅ Yes*      |
| Accurate band (n71, n41, B66, B13, etc.)     | ✅ Yes       | ✅ Yes       | ✅ Yes             | ✅ Yes       |
| Carrier name without uploading your data     | ✅ Yes       | ❌ No        | ❌ No              | ❌ No        |
| Confidence scoring per tower                 | ✅ Yes       | ❌ No        | ❌ No              | ❌ No        |
| Drive test mode + CSV export                 | ✅ Yes       | ❌ No        | 💲 Paid            | 💲 Paid      |
| Timing advance + distance estimate           | ✅ Yes       | ❌ No        | ❌ No              | ❌ No        |

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
- Cannot tell you the carrier name without a live internet lookup
- Costs money for the features that matter

**Cellfire does none of that.**
Exclusive spectrum bands (B13 → Verizon, B71 → T-Mobile, B14 → FirstNet) resolve instantly with no lookup at all. A crowd-sourced tile database cached on your device handles everything else — downloaded once, works offline. You own the spectrum. You see everything.

---

## Download

<p align="center">
  <a href="https://github.com/veteranop/Cellfire_app_public/releases/latest/download/cellfire-release.apk">
    <img src="https://img.shields.io/badge/Download%20APK-v1.0.1.22-green?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://cellfire.io">
    <img src="https://img.shields.io/badge/More%20at-cellfire.io-orange?style=for-the-badge" alt="cellfire.io"/>
  </a>
</p>

> **Sideload instructions:** Go to **Settings → Apps → Special app access → Install unknown apps**, enable your browser or file manager, then open the downloaded APK.
> Updates can be checked from within the app: **Settings → App Update**.

See [CHANGELOG.md](CHANGELOG.md) for the full release history.

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
3. **PCI+RSRP inference** — unknown neighbor cells automatically inherit the carrier of a nearby registered cell with the same PCI when signal strength matches.
4. **PCI range heuristic** — statistical fallback for towers with no other match.

Signal rules are fetched live from cellfire.io — carrier detection improves without an app update.

### Timing Advance
- Serving cell detail shows estimated distance to tower based on LTE Timing Advance
- TA values contributed anonymously to the crowd-source database for future tower trilateration

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
| ● | **Yellow** | 40–74 | **Medium confidence** — limited observations or PCI inference. Probably correct. |
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
| **TA** | Timing Advance — modem-reported round-trip delay; used to estimate distance to tower |

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

- Crowdsourcing is **opt-in** — disable at any time: **Settings → Upload cell data**
- Uploaded observations contain only radio metadata (PCI, TAC, band, approximate location) — never IMEI, IMSI, phone number, or personal identifiers
- Location is approximate (tile-level, ~30 × 30 mile grid) — not precise GPS
- Full privacy policy: [cellfire.io/privacy](https://cellfire.io/privacy)

---

## Permissions

| Permission | Why it is needed |
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
- A Cellfire account — [register at cellfire.io](https://cellfire.io/register)

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
| **What's New** | View the in-app changelog |

---

<p align="center">
  <a href="https://cellfire.io">cellfire.io</a> &nbsp;·&nbsp;
  <a href="https://cellfire.io/privacy">Privacy Policy</a> &nbsp;·&nbsp;
  <a href="CHANGELOG.md">Changelog</a>
</p>

<p align="center">© 2026 VeteranOp, LLC. All rights reserved.</p>

<p align="center">
  <a href="https://github.com/veteranop/Cellfire_app_public/releases/latest">
    <img src="https://img.shields.io/github/v/release/veteranop/Cellfire_app_public?label=latest&color=orange" alt="Latest Release"/>
  </a>
  <img src="https://img.shields.io/badge/platform-Android%2010%2B-brightgreen" alt="Android 10+"/>
  <img src="https://img.shields.io/badge/LTE%20%2F%205G%20NR-supported-blue" alt="LTE / 5G NR"/>
</p>

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
> Updates can also be checked from within the app: **Settings → App Update**.

---

## What is Cellfire?

Cellfire is a real-time **LTE and 5G NR cell tower scanner** for Android. It uses your device's radio hardware to identify every tower your phone can see — not just the one you're connected to — and resolves each tower's carrier, band, signal strength, and frequency.

Unlike generic signal apps, Cellfire cross-references a live crowd-sourced database to identify towers your modem doesn't directly name, and improves over time as more users contribute data.

Built for RF professionals, network engineers, field technicians, and anyone curious about the radio infrastructure around them.

---

## Features

### 📡 Real-Time Cell Scanning
- Scans all visible LTE and 5G NR towers simultaneously — not just your serving cell
- Reports PCI, EARFCN/NR-ARFCN, band, RSRP, RSRQ, SINR, channel bandwidth, and TAC
- **Deep scan mode** — extended 35-second scan cycle for thorough neighbor cell detection
- Persistent foreground service with automatic 5-second polling
- Supports LTE, NR (5G), WCDMA, and GSM

### 🎯 Carrier Resolution Engine

Cellfire uses a layered resolution chain to identify the carrier for every visible tower:

1. **Exclusive band** — certain spectrum bands are licensed to exactly one US carrier by law. B13 is always Verizon. B71/B41/B26 are always T-Mobile. B14 is always FirstNet. These resolve instantly with no data lookup needed.
2. **Community database** — a crowd-sourced tile database downloaded to your device identifies towers by their PCI + TAC fingerprint, pre-scored by confidence based on how many registered users have confirmed them.
3. **PCI inference** — a statistical fallback based on PCI range patterns when no database record exists.

Signal rules (exclusive bands, carrier/band mappings) are updated server-side weekly from FCC licensing data and fetched live — carrier identification can improve without an app update.

### 🗺️ Community Cell Database
- Downloads crowd-sourced cell tower tiles from **cellfire.io** for your area (~80-mile radius, ~49 tiles)
- Each tile is a 0.5° × 0.5° grid square (~30 × 30 miles), gzip-compressed and cached on-device
- Tiles refresh automatically as you move; unchanged tiles use conditional HTTP requests (ETag / If-Modified-Since) to save bandwidth
- Database entries carry a **confidence score (0–100)** based on source quality and observation count
- Every confirmed tower observation is crowd-sourced back anonymously to improve the database for all users

### 🚗 Drive Test Mode
- Records all visible cells with GPS coordinates and full signal history (RSRP/RSRQ/SINR over time)
- Export to CSV for offline analysis or import into mapping tools
- Visualize per-tower signal trends within the app

### 🔍 PCI Discovery Tracker
- Tracks every unique Physical Cell ID seen during a session
- Shows total discovered count in the main screen
- Bulk upload discovered cells from Settings to seed new coverage areas

---

## Understanding the Dot Colors

Each cell row shows a **colored dot** indicating how confident the app is in the carrier identification:

| Dot | Color | Confidence | Meaning |
|-----|-------|-----------|---------|
| ● | **Blue (gold border)** | — | **Your registered serving cell** — the tower your modem is actively camped on. Carrier confirmed directly by the modem. |
| ● | **Dark green** | 100 | **Verified** — confirmed by one or more registered users who were actually camped on this cell. Highest trust. |
| ● | **Green** | 75–99 | **High confidence** — strong crowd-sourced match or exclusive band determination. Reliable. |
| ● | **Yellow** | 40–74 | **Medium confidence** — database match with limited observations. Probably correct. |
| ● | **Red** | < 40 | **Low confidence** — PCI range heuristic only. Treat as approximate. |
| ● | **Grey** | — | **No record** — this tower is not in any loaded tile. May be newly deployed or in sparse coverage area. |

> The gold border on the serving cell is separate from dot color — a serving cell can be dark green (verified in DB and you're registered to it) or grey (not in DB yet).

---

## Signal Measurements

| Field | Description |
|-------|-------------|
| **RSRP** | Reference Signal Received Power (dBm). Raw signal strength. ≥ −85 good · −85 to −100 fair · < −100 weak |
| **RSRQ** | Reference Signal Received Quality (dB). Signal quality relative to interference. ≥ −10 good |
| **SINR** | Signal-to-Interference-plus-Noise Ratio (dB). Best indicator of actual throughput potential |
| **Band** | LTE or NR frequency band (e.g. B66 = AWS 1700/2100 MHz · B12 = 700 MHz · n71 = 600 MHz NR) |
| **EARFCN** | Absolute Radio Frequency Channel Number — the exact frequency channel within a band |
| **BW** | Channel bandwidth in MHz. Wider = higher theoretical peak data rates |
| **PCI** | Physical Cell Identity (0–503). The radio-layer ID broadcast by every tower |
| **TAC** | Tracking Area Code. Carrier-assigned regional identifier — a reliable carrier fingerprint when available |

---

## Carrier Color Coding

Each carrier's row is highlighted in a distinct background color for fast visual identification:

| Carrier | Row Color |
|---------|-----------|
| T-Mobile | Magenta |
| AT&T | Blue |
| Verizon | Red |
| US Cellular | Purple |
| FirstNet / AT&T | Dark |
| Dish Wireless (Boost) | Orange |

---

## How Mapping Works

Cellfire builds its database from anonymous observations contributed by users worldwide.

**What gets uploaded:**
- PCI, TAC, carrier name, band, EARFCN, and approximate GPS location
- Source tag indicating how the carrier was identified (modem-confirmed, band-law, or inferred)
- No personal identifiers, IMEI, phone number, or account information

**How confidence is scored:**

| Source | Base Score | Description |
|--------|-----------|-------------|
| Modem-confirmed (alpha/plmn) | 100 | Your phone was registered to this cell and the modem named the carrier |
| Exclusive band | 90 | Spectrum law permits only one carrier on this band nationally |
| FCC geo (server-side) | 90 | Single licensed carrier for this band in this state per FCC records — updated weekly from FCC ULS bulk data |
| Database match | 70 | Confirmed by prior crowd observation |
| Neighbor inference | 55 | Inferred from nearby confirmed cells using PCI/TAC/geographic proximity |
| PCI range heuristic | 20 | Statistical PCI bucket guess — lowest trust |

Sample count adds up to +15 to the base score (log₂ scale). Carrier conflicts subtract up to −20.

**How tiles get smarter over time:**
When a registered user's phone camps on a cell that was previously only seen as a neighbor (TAC unknown), their observation promotes that record to confidence 100 and writes the confirmed TAC and EARFCN. All users who download that tile subsequently see the dark green dot immediately.

---

## Privacy

- No account required to use the app
- Crowdsourcing is **opt-in** and can be disabled at any time: **Settings → Upload cell data**
- Uploaded observations contain only radio metadata (PCI, TAC, band, approximate location) — never personal identifiers, IMEI, IMSI, or phone number
- Location data is approximate (tile-level, ~30 × 30 mile grid) — not precise GPS coordinates
- No analytics or crash reporting beyond what Firebase provides for service reliability

---

## Permissions

| Permission | Why it's needed |
|-----------|----------------|
| `ACCESS_FINE_LOCATION` | Android requires precise location to access radio/cell info via TelephonyManager |
| `READ_PHONE_STATE` | Required to call `getAllCellInfo()` and read signal measurements |
| `FOREGROUND_SERVICE` | Keeps the scan running while the app is in the background |
| `INTERNET` | Downloads tile database, fetches signal rule updates, uploads observations |
| `REQUEST_INSTALL_PACKAGES` | Required for the in-app update installer |

---

## Requirements

- Android 10+ (API 26 minimum; API 31+ recommended for best neighbor cell data)
- A device with Qualcomm or similar modem for full neighbor cell EARFCN reporting
- Location services enabled

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

## Related

| | |
|--|--|
| [Cellfire RF Studio](https://cellfire.io) | Desktop RF analysis suite (Windows / macOS) |
| [cellfire.io](https://cellfire.io) | Platform, tile database, and downloads |

---

<p align="center">
  <a href="https://cellfire.io">cellfire.io</a> &nbsp;·&nbsp;
  <a href="https://cellfire.io/downloads">Downloads</a> &nbsp;·&nbsp;
  <a href="https://cellfire.io/privacy-policy">Privacy Policy</a>
</p>

<p align="center">© 2026 Cellfire. All rights reserved.</p>
