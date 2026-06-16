# Changelog — Cellfire Android App

All notable changes to the Cellfire app are listed here, newest first.

---

## v1.0.1.22 — 2026-05-18
- **Fix:** Account screen now shows real subscription expiry date instead of JWT token expiry
- **Fix:** Email verification no longer resets expiry to trial date for paid/enterprise accounts

## v1.0.1.21 — 2026-05-18
- **New:** Email verification for new signups — verification link sent on registration; login rejects unverified accounts with a clear message and resend option
- **New:** Enterprise multi-device support — up to 5 registered devices per enterprise account

## v1.0.1.20 — 2026-05-18
- **Fix:** Subscription status now cached locally — no longer shows "Inactive" due to network hiccups or token expiry between refreshes
- **Fix:** App no longer logs users out after OS updates that reset the Android keystore

## v1.0.1.19 — 2026-05-18
- **New:** PCI+RSRP inference — unknown neighbor cells automatically inherit the carrier of a registered serving cell when they share the same PCI and signal strength matches within 5 dB; resolves same-tower multi-band unknowns without any database lookup
- Inferred cells display with a yellow dot and contribute to the crowd-source database

## v1.0.1.18 — 2026-05-12
- 5G NR cell detail now includes a Timing Advance field (ready and waiting — Android does not yet expose NR TA in the public API)

## v1.0.1.17 — 2026-05-12
- **New:** Manual carrier edits now carry the highest confidence score and submit directly to the crowd-source database
- Frequency calculator fully rewritten against 3GPP TS 36.101 — corrects Band 4 uplink; adds B17, B25, B26, B29, B30, B48
- Carrier EARFCN map updated from current FCC license data
- **Fix:** Battery drain when app is closed — GPS and service now halt completely on swipe-away

## v1.0.1.16 — 2026-05-12
- **Fix:** Foreground service now reliably stops when the app is closed — GPS no longer runs silently in the background after a swipe-away

## v1.0.1.15 — 2026-05-11
- **Fix:** 5G NR registered cell now shows "N/A — not reported by modem" in the TA field instead of hiding it entirely

## v1.0.1.14 — 2026-05-11
- **Fix:** Exclusive-band cells (n71, B13, B14, etc.) now display the correct green confidence dot
- TA row always visible on the serving cell detail card — shows modem availability status when value is unavailable

## v1.0.1.13 — 2026-05-11
- **New:** Timing Advance capture — estimated distance to tower displayed on the serving cell detail card
- TA values included in crowd-sourced observations for future tower trilateration
- **New:** "What's New" changelog screen added to Settings

## v1.0.1.12 — 2026-05-11
- **Fix:** Registered serving cell always displays the dark-green verified confidence dot

## v1.0.1.11 — 2026-05-11
- App name graphic updated to transparent PNG (removed white background artifact)

## v1.0.1.10 — 2026-05-11
- New launcher icon, splash background, and app name graphic — updated branding

## v1.0.1.8 / v1.0.1.9 — 2026-04-05
- **New:** Persistent login — session survives app restarts without re-entering credentials
- Consolidated "Upgrade Cell DB" into a single button; fixed button clipping on smaller screens

## v1.0.1.4 — 2026-03-26
- **Security:** Crowd-source writes to Firebase now require a valid anonymous auth token — locks down the Realtime Database against unauthorized writes

## v1.0.1.3 — 2026-03-26
- Scan rate tuning and background stability improvements
- Signal collection reliability fixes

## v1.0.1.6 — 2026-03-22 *(Initial stable release)*
- Real-time LTE and 5G NR cell scanning — all visible towers, not just the serving cell
- All-carrier neighbor cell display with accurate band, RSRP, RSRQ, SINR, PCI, TAC
- Exclusive band resolution (B13 → Verizon, B71/B41 → T-Mobile, B14 → FirstNet) — instant, no lookup
- Crowd-sourced tile database downloaded and cached on-device for offline carrier identification
- Confidence scoring per tower (0–100) based on source quality and observation count
- Drive test mode with GPS tagging and CSV export
- PCI discovery tracker
- Signal rules fetched live from cellfire.io
- About screen with signal reference guide
