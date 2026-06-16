#!/usr/bin/env python3
"""
patch_earfcn_map.py
===================
Adds missing LTE band entries to carrier_earfcn_map.json.

The existing file has precise FCC FR-derived sub-ranges for B2, B4, B13, B14,
B41, B66, B71 and their NR counterparts.  The following bands were missing:
  B17  —  AT&T 700 Lower B/C        EARFCN 5730–5849
  B25  —  PCS+ (T-Mo, VZW, AT&T)   EARFCN 8040–8689
  B26  —  850 CLR+ (US Cellular)    EARFCN 8690–9039
  B29  —  700 D DL-only supplement  EARFCN 9660–9769
  B30  —  WCS (AT&T)                EARFCN 9770–9869

Source of truth for which carrier holds which band in each state:
  band_license_map.json  (built weekly from FCC ULS data)

Because the FCC FR records have not yet been parsed to per-carrier sub-ranges
for these bands, we add the full 3GPP EARFCN range for now.  That is still
strictly correct — the resolver will only return a carrier name when exactly
one carrier matches, so having a wide range for an uncontested band (B17 = AT&T
only) is accurate, while genuinely shared bands (B25) stay ambiguous until
sub-range data is available.
"""

import json
from pathlib import Path

BASE     = Path(__file__).parent
MAP_IN   = BASE / "app" / "src" / "main" / "assets" / "carrier_earfcn_map.json"
LICENSE  = BASE / "app" / "src" / "main" / "assets" / "band_license_map.json"

# Full 3GPP EARFCN ranges for the bands being added
MISSING_BANDS: dict[str, tuple[int, int]] = {
    "B17": (5730,  5849),   # 700 Lower B/C  (AT&T)
    "B25": (8040,  8689),   # PCS+ 1900       (AT&T, T-Mobile, Verizon)
    "B26": (8690,  9039),   # 850 CLR+        (US Cellular)
    "B29": (9660,  9769),   # 700 D DL-only   (AT&T, Verizon)
    "B30": (9770,  9869),   # WCS 2350        (AT&T)
}

# Map band number (int, as stored in band_license_map.json) → band label
BAND_NUM_TO_LABEL = {17: "B17", 25: "B25", 26: "B26", 29: "B29", 30: "B30"}


def main():
    earfcn_map: dict = json.loads(MAP_IN.read_text(encoding="utf-8"))
    license_map: dict = json.loads(LICENSE.read_text(encoding="utf-8"))

    added = 0
    for state, carriers in license_map.items():
        if state not in earfcn_map:
            earfcn_map[state] = {}

        for carrier, band_list in carriers.items():
            if carrier not in earfcn_map[state]:
                earfcn_map[state][carrier] = {}

            carrier_entry = earfcn_map[state][carrier]

            for band_num in band_list:
                label = BAND_NUM_TO_LABEL.get(band_num)
                if label is None:
                    continue   # already handled or not in scope
                if label in carrier_entry:
                    continue   # already has data — don't overwrite

                lo, hi = MISSING_BANDS[label]
                carrier_entry[label] = [[lo, hi]]
                added += 1

    # Sort keys and strip empty carrier objects created by the init pass
    sorted_map = {
        state: {
            carrier: dict(sorted(bands.items()))
            for carrier, bands in sorted(carriers.items())
            if bands  # drop empty carrier shells
        }
        for state, carriers in sorted(earfcn_map.items())
    }

    MAP_IN.write_text(json.dumps(sorted_map, indent=2), encoding="utf-8")
    print(f"Done. Added {added} band entries across {len(sorted_map)} states.")
    print("Bands added: " + ", ".join(MISSING_BANDS.keys()))


if __name__ == "__main__":
    main()
