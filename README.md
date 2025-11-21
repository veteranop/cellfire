# CellFire — The Wireless Warlord Scanner

**Real-time, all-carrier cell tower scanner for Android**  
See every tower around you — T-Mobile, Verizon, AT&T, FirstNet, Dish — with accurate band, RSRP, SNR, PCI, and carrier name. No root. No upload. No internet required.


**CellFire does what no other free app on the Play Store can do in 2025.**

| Feature                              | CellFire | CellMapper | Network Cell Info | SignalCheck |
|--------------------------------------|----------|------------|-------------------|-------------|
| Real-time all-carrier tower display  | Soon     | Yes*       | Yes*              | Yes*        |
| Accurate band (n71, n41, B66, etc.)  | Yes      | Yes        | Yes               | Yes         |
| Carrier name without internet        | Yes      | No         | No                | No          |
| Zero data upload / privacy first     | Yes      | No         | No                | No          |
| Tactical battlefield UI              | Yes      | No         | No                | No          |
| Free                                 |with ads  | Yes        | No                | No          |

\* Requires paid version or data upload

## Why CellFire Exists

Every other app either:
- Hides neighbor cells from other carriers
- Requires you to upload your data to see towers
- Needs internet to resolve carrier names
- Costs money

**CellFire does none of that.**  
You own the spectrum. You see everything. Instantly.

## Features

- **All carriers** — T-Mobile, Verizon, AT&T, FirstNet, Dish, US Cellular
- **All bands** — n71, n41, n77/n78, B66, B13, B12, B71, etc.
- **Real signal values** — RSRP, SNR/SINR, PCI
- **LTE + 5G NR** — full support
- **Tower map** — in development - Soon
- **No internet required** — carrier from band + local lookup
- **Zero data collection** — nothing leaves your phone


## Requirements

- Android 9+ (API 26)
- Location permission
- Phone state permission

## Installation

Download the latest APK from [Releases](https://github.com/veteranop/CellFire/releases)

Or build from source:

```bash
git clone https://github.com/veteranop/CellFire.git
cd CellFire
./gradlew assembleRelease
