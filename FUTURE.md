# Cellfire — Future / Backlog

Non-urgent items to address eventually. Not blocking launch. Prioritize when relevant work is already in flight.

---

## Firebase

### Tighten Realtime DB rules (minor hardening)
Current rules work fine. Three small improvements worth making when touching Firebase anyway:
- Add `newData.child('processed').val() == false` — prevents a client from submitting an already-processed observation, which would make it invisible to the merge pipeline
- Add string length limits on `carrier` / `source` fields (e.g. `<= 64`) — prevents DB storage abuse
- Add required fields to `.validate`: `tac`, `carrier`, `source`, `timestamp`, `processed`

These do NOT affect legitimate app writes or the merge script (Admin SDK bypasses rules).

### Upgrade Firebase auth from Anonymous → Cellfire-backed custom tokens
Currently the app signs in with Firebase Anonymous Auth before writing observations.
A stronger model: after a user logs in with their Cellfire account, the Cellfire backend issues a Firebase Custom Token (using the Firebase Admin SDK), and the app signs into Firebase with that. This ties Firebase identity to a verified Cellfire account instead of a throwaway anonymous UID.
- Requires adding `firebase-admin` to the server and a new `auth.firebase_token` API endpoint
- Only worth doing if abuse becomes a real problem

---

## Android App

### NR (5G) support in tile DB lookup
`CellfireDbManager.lookupByPciTac()` currently only surfaces LTE cells from tiles. 5G NR cells have PCI 0–1007 and use different TAC encoding in some cases. The tile data schema would need an `rat` field to distinguish, and the lookup logic needs to handle NR-specific fields.

### Fix EARFCN schema gap in DbCell
`DbCell` has no `earfcn` field, so cross-band inference for B12 unknowns can't fire during tile lookups. Add `earfcn` to the tile JSON schema and `DbCell` data class, then wire it into `CarrierResolver`.

### Add `ci` (Cell Identity) to tile DB schema
Currently tiles store `cellid` but the app's `DbCell` doesn't expose it for matching. Useful for future disambiguation when PCI+TAC isn't unique within a tile.

---

## Server / Data

### Clean up 24 corrupt zero-byte tiles
There are 24 known zero-byte `.json.gz` tile files on the server that return empty arrays. Clients request them, get nothing, and move on — harmless but wasteful. Requires listing all tiles and deleting zero-byte ones. Awaiting a convenient time (no SSH needed — can be done via a runner script).

### Seed more tile coverage
Tile DB is currently sparse outside of PR/USVI/Pacific territories. Coverage grows naturally via crowdsourcing as users adopt the app. No manual action needed — just track growth.

---

## Website

### Signal Rules admin module (`com_cellfireapp`)
Build a Joomla backend component to edit `band_carrier_lookup.json`, `pci_carrier_data.json`, and `carrier_earfcn_map.json` from the Joomla admin panel — no cPanel/Admin Tool needed for rule edits. Low priority since the Admin Tool Rules tab already handles this.

---

## Play Store (post-launch polish)

### In-app review prompt
Add `com.google.android.play:review` to trigger the Play Store in-app review dialog after a user has scanned for a few sessions. Don't block on this for initial launch.

### Adaptive icon refinement
Current adaptive icon uses foreground/background layers — works correctly. Consider a higher-contrast foreground layer for better visibility on colored launcher backgrounds.
