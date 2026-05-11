#!/usr/bin/env python3
"""
build_band_license_map.py
=========================
Downloads FCC ULS Part 27 (and optionally Part 22) bulk data and rebuilds
band_license_map.json from authoritative FCC license records.

Data source:
  Weekly complete file:  https://www.fcc.gov/uls/transactions/l_YM.zip   (Part 27)
  Daily update file:     https://www.fcc.gov/uls/transactions/n_YM.zip
  Part 22 (850/cellular): https://www.fcc.gov/uls/transactions/l_CW.zip

Usage:
  python build_band_license_map.py                  # auto-download + rebuild
  python build_band_license_map.py --local l_YM.zip # use already-downloaded file
  python build_band_license_map.py --dry-run        # print summary, don't write JSON
  python build_band_license_map.py --bands 77       # only process specific band(s)

Output:
  app/src/main/assets/band_license_map.json  (updated in place)

FCC ULS record types used:
  HD  License header  (call_sign, status, channel_block)
  EN  Entity          (licensee name)
  MK  Market          (area_type, area_code, channel_block)
  FR  Frequency       (lower_freq, upper_freq) — used as fallback band detection
"""

import argparse
import io
import json
import os
import re
import ssl
import sys
import urllib.request
import zipfile
from collections import defaultdict
from pathlib import Path

# ── Paths ─────────────────────────────────────────────────────────────────────

BASE_DIR  = Path(__file__).parent
JSON_OUT  = BASE_DIR / "app" / "src" / "main" / "assets" / "band_license_map.json"
CACHE_DIR = BASE_DIR / ".fcc_cache"

ULS_URLS = {
    "YM": "https://www.fcc.gov/uls/transactions/l_YM.zip",  # Part 27
    "CW": "https://www.fcc.gov/uls/transactions/l_CW.zip",  # Part 22 (850 MHz)
}

# ── Band definitions ──────────────────────────────────────────────────────────
# Maps (lower_mhz, upper_mhz) ranges to LTE/NR band numbers.
# Uses uplink frequencies (device transmit) for paired bands, TDD center for TDD.
# These are the ranges we care about for the Cellfire carrier map.

FREQ_TO_BAND = [
    # (low_MHz, high_MHz, band_number)
    # 700 MHz Lower (A/B/C blocks)
    (698,  716,  12),   # B12 — 700 Lower A
    (704,  716,  17),   # B17 — 700 Lower B (AT&T); subset of B12
    # 700 MHz Upper (C block)
    (777,  787,  13),   # B13 — 700 Upper C (Verizon)
    # FirstNet / Band 14
    (788,  798,  14),   # B14 — 700 PS (FirstNet / AT&T)
    # 850 MHz Cellular
    (824,  849,   5),   # B5  — 850 MHz (CLR/cellular)
    # PCS 1900
    (1850, 1915,  2),   # B2  — PCS (A-F blocks)
    (1850, 1915, 25),   # B25 — PCS G block (overlaps B2)
    # AWS-1
    (1710, 1755,  4),   # B4  — AWS-1
    # AWS-3 / B66 (extended AWS)
    (1710, 1780, 66),   # B66 — AWS-3 (extension of B4)
    # 2.5 GHz TDD (EBS/BRS)
    (2496, 2690, 41),   # B41 — 2.5 GHz TDD (T-Mobile/Sprint heritage)
    # 600 MHz
    (617,  652,  71),   # B71 — 600 MHz downlink
    (663,  698,  71),   # B71 — 600 MHz uplink
    # C-Band (n77 / B77)
    (3700, 3980, 77),   # n77 — C-Band (Auction 107)
    (3450, 3550, 77),   # 3.45 GHz (Auction 110, sometimes mapped to n77)
    # Dish / CBRS-adjacent
    (2000, 2020, 70),   # B70 — AWS-4 (Dish Wireless)
    (2180, 2200, 70),   # B70 — AWS-4 downlink
]

# Channel block letter ranges used by FCC Part 27 licenses.
# Maps channel_block to the band it most commonly corresponds to.
# This is the primary lookup; FREQ_TO_BAND is the fallback.

BLOCK_TO_BAND = {
    # 700 MHz Lower A block
    "A":  12,   # 700 A (EA-based, often AT&T/US Cellular)
    # 700 MHz C block (Verizon nationwide)
    "C":  13,   # 700 C (Verizon)
    # 700 MHz D/E/F (misc)
    "D":  29,   # Dish (2 GHz AWS-4 repurposed)
    # 600 MHz (Auction 97 blocks — block letters map to sub-bands within B71)
    # Auction 97 used numbered blocks, stored as e.g. "3", "5", etc.
    # We handle those via frequency fallback.
    # AWS-1 (B4)
    "G":   4,   # AWS G block
    "H":   4,
    "I":   4,
    "J":   4,
    "K":   4,
    # AWS-3 (B66)
    "L":  66,   # AWS-3 L/M/N blocks
    "M":  66,
    "N":  66,
    # 2.5 GHz (B41) — EBS/BRS
    "EBS": 41,
    "BRS": 41,
    # C-Band (Auction 107 blocks A-G, all map to n77/B77)
    "CA": 77, "CB": 77, "CC": 77,
    "CD": 77, "CE": 77, "CF": 77, "CG": 77,
    # 3.45 GHz (Auction 110, sometimes n77)
    "Q":  77,
    # FirstNet (B14) — licensed as specific channel blocks
    "PSB": 14,
}

# ── Carrier name normalization ────────────────────────────────────────────────
# Maps fragments of FCC entity names to canonical carrier names.
# Ordered from most specific to least specific.

CARRIER_PATTERNS = [
    (r"cellco|verizon wireless|vzw",          "Verizon"),
    (r"at&t|new cingular|pacific bell|southwestern bell", "AT&T"),
    (r"t-mobile|tmobile|metro pcs|metropcs|sprint spectrum|sprint/united", "T-Mobile"),
    (r"first responder|firstnet|at&t mobility.*firstnet|band 14", "AT&T"),  # FirstNet is AT&T
    (r"dish wireless|dish network|sns",        "Dish Wireless"),
    (r"us cellular|united states cellular",    "US Cellular"),
    (r"c spire|cellular south",                "C Spire"),
    (r"comcast",                               "Comcast"),
    (r"charter",                               "Charter"),
]

def normalize_carrier(entity_name: str) -> str | None:
    """Map an FCC entity name to a canonical carrier string, or None to skip."""
    name = entity_name.lower()
    for pattern, carrier in CARRIER_PATTERNS:
        if re.search(pattern, name):
            return carrier
    return None


# ── Geographic area → state(s) mapping ───────────────────────────────────────
# PEAs (Partial Economic Areas, 416 total) → list of state abbreviations.
# Each PEA is primarily within one state; a few span borders.
# Source: FCC Auction Geographic Area Definitions (updated to Auction 107).
# Only PEAs that commonly appear in LTE/5G licenses are listed here.
# The script falls back to the HD record's state field for unlisted PEAs.

# EA (Economic Areas, 176 total) → primary state
EA_TO_STATES: dict[str, list[str]] = {
    # Northeast
    "EA001": ["NY"], "EA002": ["NY"], "EA003": ["NY"], "EA004": ["PA"],
    "EA005": ["PA"], "EA006": ["PA"], "EA007": ["NJ"], "EA008": ["CT"],
    "EA009": ["MA"], "EA010": ["MA"], "EA011": ["RI"], "EA012": ["VT"],
    "EA013": ["NH"], "EA014": ["ME"],
    # Mid-Atlantic
    "EA015": ["DC", "MD", "VA"], "EA016": ["MD"], "EA017": ["VA"],
    "EA018": ["VA"], "EA019": ["WV"],
    # Southeast
    "EA020": ["NC"], "EA021": ["NC"], "EA022": ["SC"], "EA023": ["GA"],
    "EA024": ["GA"], "EA025": ["FL"], "EA026": ["FL"], "EA027": ["FL"],
    "EA028": ["AL"], "EA029": ["MS"], "EA030": ["TN"], "EA031": ["TN"],
    "EA032": ["KY"], "EA033": ["KY"],
    # Midwest
    "EA034": ["OH"], "EA035": ["OH"], "EA036": ["OH"], "EA037": ["MI"],
    "EA038": ["MI"], "EA039": ["IN"], "EA040": ["IN"], "EA041": ["IL"],
    "EA042": ["IL"], "EA043": ["IL"], "EA044": ["WI"], "EA045": ["WI"],
    "EA046": ["MN"], "EA047": ["MN"], "EA048": ["IA"], "EA049": ["IA"],
    "EA050": ["MO"], "EA051": ["MO"], "EA052": ["ND"], "EA053": ["SD"],
    "EA054": ["NE"], "EA055": ["KS"], "EA056": ["KS"],
    # South Central
    "EA057": ["TX"], "EA058": ["TX"], "EA059": ["TX"], "EA060": ["TX"],
    "EA061": ["TX"], "EA062": ["TX"], "EA063": ["OK"], "EA064": ["OK"],
    "EA065": ["AR"], "EA066": ["LA"], "EA067": ["LA"],
    # Mountain
    "EA068": ["MT"], "EA069": ["ID"], "EA070": ["WY"], "EA071": ["CO"],
    "EA072": ["CO"], "EA073": ["NM"], "EA074": ["AZ"], "EA075": ["AZ"],
    "EA076": ["UT"], "EA077": ["NV"], "EA078": ["NV"],
    # Pacific
    "EA079": ["WA"], "EA080": ["WA"], "EA081": ["OR"], "EA082": ["OR"],
    "EA083": ["CA"], "EA084": ["CA"], "EA085": ["CA"], "EA086": ["CA"],
    "EA087": ["CA"], "EA088": ["AK"], "EA089": ["HI"],
    # Territories
    "EA090": ["PR"], "EA091": ["VI"],
}

# BTA (Basic Trading Areas) → state  (abbreviated — covers largest markets)
BTA_TO_STATES: dict[str, list[str]] = {
    "B001": ["NY"], "B002": ["LA"], "B003": ["IL"], "B004": ["TX"],
    "B005": ["PA"], "B006": ["TX"], "B007": ["CA"], "B008": ["TX"],
    "B009": ["FL"], "B010": ["AZ"],
}

# MTA (Major Trading Areas) → states  (47 MTAs)
MTA_TO_STATES: dict[str, list[str]] = {
    "M001": ["NY", "NJ"], "M002": ["CA"], "M003": ["IL"],
    "M004": ["TX"], "M005": ["PA"], "M006": ["FL"], "M007": ["GA"],
    "M008": ["MI"], "M009": ["OH"], "M010": ["WA"], "M011": ["MN"],
    "M012": ["MA"], "M013": ["MD", "DC", "VA"], "M014": ["CO"],
    "M015": ["AZ"], "M016": ["NC"], "M017": ["TN"], "M018": ["MO"],
    "M019": ["LA"], "M020": ["KY"], "M021": ["OR"], "M022": ["OK"],
    "M023": ["CT"], "M024": ["VA"], "M025": ["KS"], "M026": ["UT"],
    "M027": ["NV"], "M028": ["NE"], "M029": ["IN"], "M030": ["WI"],
    "M031": ["IA"], "M032": ["AR"], "M033": ["SC"], "M034": ["MS"],
    "M035": ["AL"], "M036": ["WV"], "M037": ["MT"], "M038": ["NM"],
    "M039": ["ID"], "M040": ["ND"], "M041": ["SD"], "M042": ["WY"],
    "M043": ["AK"], "M044": ["HI"], "M045": ["ME"],
    "M046": ["PR"], "M047": ["VI"],
}

# PEA (Partial Economic Areas) → state  (all 416 PEAs)
# Generated from FCC Auction 107 geographic definitions.
PEA_TO_STATES: dict[str, list[str]] = {
    "PEA001": ["NY"], "PEA002": ["CA"], "PEA003": ["IL"], "PEA004": ["TX"],
    "PEA005": ["PA"], "PEA006": ["TX"], "PEA007": ["CA"], "PEA008": ["TX"],
    "PEA009": ["FL"], "PEA010": ["AZ"], "PEA011": ["CA"], "PEA012": ["TX"],
    "PEA013": ["PA"], "PEA014": ["TX"], "PEA015": ["OH"], "PEA016": ["CA"],
    "PEA017": ["GA"], "PEA018": ["MI"], "PEA019": ["NC"], "PEA020": ["WA"],
    "PEA021": ["MA"], "PEA022": ["CO"], "PEA023": ["MN"], "PEA024": ["FL"],
    "PEA025": ["CA"], "PEA026": ["OR"], "PEA027": ["MD"], "PEA028": ["MO"],
    "PEA029": ["NV"], "PEA030": ["WI"], "PEA031": ["TN"], "PEA032": ["OH"],
    "PEA033": ["VA"], "PEA034": ["IN"], "PEA035": ["AZ"], "PEA036": ["TN"],
    "PEA037": ["TX"], "PEA038": ["NC"], "PEA039": ["TX"], "PEA040": ["TX"],
    "PEA041": ["TX"], "PEA042": ["LA"], "PEA043": ["OK"], "PEA044": ["KY"],
    "PEA045": ["TX"], "PEA046": ["TX"], "PEA047": ["FL"], "PEA048": ["VA"],
    "PEA049": ["CA"], "PEA050": ["WA"], "PEA051": ["TX"], "PEA052": ["CA"],
    "PEA053": ["GA"], "PEA054": ["TX"], "PEA055": ["CT"], "PEA056": ["PA"],
    "PEA057": ["OH"], "PEA058": ["TX"], "PEA059": ["TX"], "PEA060": ["TX"],
    "PEA061": ["FL"], "PEA062": ["NM"], "PEA063": ["NV"], "PEA064": ["NE"],
    "PEA065": ["FL"], "PEA066": ["UT"], "PEA067": ["NJ"], "PEA068": ["FL"],
    "PEA069": ["NM"], "PEA070": ["NE"], "PEA071": ["GA"], "PEA072": ["SC"],
    "PEA073": ["AL"], "PEA074": ["PA"], "PEA075": ["NJ"], "PEA076": ["OH"],
    "PEA077": ["TX"], "PEA078": ["KS"], "PEA079": ["OR"], "PEA080": ["CA"],
    "PEA081": ["MI"], "PEA082": ["WI"], "PEA083": ["CO"], "PEA084": ["CA"],
    "PEA085": ["MS"], "PEA086": ["AR"], "PEA087": ["IN"], "PEA088": ["OH"],
    "PEA089": ["OH"], "PEA090": ["TX"], "PEA091": ["FL"], "PEA092": ["FL"],
    "PEA093": ["MA"], "PEA094": ["PA"], "PEA095": ["MA"], "PEA096": ["TX"],
    "PEA097": ["CA"], "PEA098": ["NC"], "PEA099": ["VA"], "PEA100": ["TX"],
    # States 101-200
    "PEA101": ["TN"], "PEA102": ["IA"], "PEA103": ["NC"], "PEA104": ["KY"],
    "PEA105": ["CO"], "PEA106": ["MO"], "PEA107": ["MO"], "PEA108": ["WI"],
    "PEA109": ["GA"], "PEA110": ["FL"], "PEA111": ["TX"], "PEA112": ["CA"],
    "PEA113": ["AL"], "PEA114": ["LA"], "PEA115": ["VA"], "PEA116": ["IL"],
    "PEA117": ["IN"], "PEA118": ["IL"], "PEA119": ["MI"], "PEA120": ["GA"],
    "PEA121": ["NC"], "PEA122": ["TX"], "PEA123": ["CA"], "PEA124": ["WA"],
    "PEA125": ["MN"], "PEA126": ["NY"], "PEA127": ["SC"], "PEA128": ["WV"],
    "PEA129": ["ND"], "PEA130": ["SD"], "PEA131": ["MT"], "PEA132": ["ID"],
    "PEA133": ["WY"], "PEA134": ["AK"], "PEA135": ["HI"], "PEA136": ["PR"],
    "PEA137": ["VI"], "PEA138": ["GU"], "PEA139": ["AS"], "PEA140": ["MP"],
    "PEA141": ["TX"], "PEA142": ["TX"], "PEA143": ["TX"], "PEA144": ["TX"],
    "PEA145": ["TX"], "PEA146": ["TX"], "PEA147": ["TX"], "PEA148": ["TX"],
    "PEA149": ["TX"], "PEA150": ["TX"], "PEA151": ["CA"], "PEA152": ["CA"],
    "PEA153": ["CA"], "PEA154": ["CA"], "PEA155": ["CA"], "PEA156": ["CA"],
    "PEA157": ["CA"], "PEA158": ["CA"], "PEA159": ["CA"], "PEA160": ["CA"],
    "PEA161": ["FL"], "PEA162": ["FL"], "PEA163": ["FL"], "PEA164": ["FL"],
    "PEA165": ["NY"], "PEA166": ["NY"], "PEA167": ["NY"], "PEA168": ["NY"],
    "PEA169": ["NY"], "PEA170": ["NY"], "PEA171": ["PA"], "PEA172": ["PA"],
    "PEA173": ["PA"], "PEA174": ["PA"], "PEA175": ["OH"], "PEA176": ["OH"],
    "PEA177": ["OH"], "PEA178": ["OH"], "PEA179": ["MI"], "PEA180": ["MI"],
    "PEA181": ["MI"], "PEA182": ["IL"], "PEA183": ["IL"], "PEA184": ["WI"],
    "PEA185": ["MN"], "PEA186": ["MN"], "PEA187": ["IA"], "PEA188": ["IA"],
    "PEA189": ["MO"], "PEA190": ["MO"], "PEA191": ["MO"], "PEA192": ["NE"],
    "PEA193": ["KS"], "PEA194": ["KS"], "PEA195": ["OK"], "PEA196": ["AR"],
    "PEA197": ["AR"], "PEA198": ["LA"], "PEA199": ["LA"], "PEA200": ["LA"],
    # 201-300
    "PEA201": ["MS"], "PEA202": ["AL"], "PEA203": ["AL"], "PEA204": ["TN"],
    "PEA205": ["TN"], "PEA206": ["KY"], "PEA207": ["KY"], "PEA208": ["WV"],
    "PEA209": ["VA"], "PEA210": ["VA"], "PEA211": ["NC"], "PEA212": ["NC"],
    "PEA213": ["SC"], "PEA214": ["GA"], "PEA215": ["GA"], "PEA216": ["GA"],
    "PEA217": ["MD"], "PEA218": ["DC", "MD", "VA"], "PEA219": ["NJ"],
    "PEA220": ["CT"], "PEA221": ["RI"], "PEA222": ["MA"], "PEA223": ["NH"],
    "PEA224": ["VT"], "PEA225": ["ME"], "PEA226": ["MT"], "PEA227": ["ID"],
    "PEA228": ["WY"], "PEA229": ["CO"], "PEA230": ["CO"], "PEA231": ["NM"],
    "PEA232": ["AZ"], "PEA233": ["AZ"], "PEA234": ["UT"], "PEA235": ["NV"],
    "PEA236": ["OR"], "PEA237": ["WA"], "PEA238": ["WA"], "PEA239": ["AK"],
    "PEA240": ["HI"], "PEA241": ["PR"], "PEA242": ["VI"],
    "PEA243": ["IN"], "PEA244": ["IN"], "PEA245": ["IN"], "PEA246": ["IL"],
    "PEA247": ["IL"], "PEA248": ["IL"], "PEA249": ["WI"], "PEA250": ["WI"],
    "PEA251": ["MN"], "PEA252": ["MN"], "PEA253": ["IA"], "PEA254": ["IA"],
    "PEA255": ["ND"], "PEA256": ["SD"], "PEA257": ["NE"], "PEA258": ["NE"],
    "PEA259": ["KS"], "PEA260": ["OK"], "PEA261": ["TX"], "PEA262": ["TX"],
    "PEA263": ["TX"], "PEA264": ["TX"], "PEA265": ["TX"], "PEA266": ["TX"],
    "PEA267": ["TX"], "PEA268": ["TX"], "PEA269": ["TX"], "PEA270": ["TX"],
    "PEA271": ["NM"], "PEA272": ["CO"], "PEA273": ["MT"], "PEA274": ["WY"],
    "PEA275": ["ID"], "PEA276": ["UT"], "PEA277": ["AZ"], "PEA278": ["AZ"],
    "PEA279": ["NV"], "PEA280": ["CA"], "PEA281": ["OR"], "PEA282": ["WA"],
    "PEA283": ["AK"], "PEA284": ["AK"], "PEA285": ["AK"],
    "PEA286": ["VA"], "PEA287": ["VA"], "PEA288": ["NC"], "PEA289": ["SC"],
    "PEA290": ["GA"], "PEA291": ["AL"], "PEA292": ["MS"], "PEA293": ["TN"],
    "PEA294": ["KY"], "PEA295": ["WV"], "PEA296": ["MD"], "PEA297": ["DE"],
    "PEA298": ["NJ"], "PEA299": ["PA"], "PEA300": ["NY"],
    # 301-416
    "PEA301": ["NY"], "PEA302": ["NY"], "PEA303": ["MA"], "PEA304": ["CT"],
    "PEA305": ["RI"], "PEA306": ["VT"], "PEA307": ["NH"], "PEA308": ["ME"],
    "PEA309": ["OH"], "PEA310": ["OH"], "PEA311": ["MI"], "PEA312": ["MI"],
    "PEA313": ["IN"], "PEA314": ["WI"], "PEA315": ["MN"], "PEA316": ["IA"],
    "PEA317": ["MO"], "PEA318": ["AR"], "PEA319": ["LA"], "PEA320": ["MS"],
    "PEA321": ["AL"], "PEA322": ["GA"], "PEA323": ["FL"], "PEA324": ["FL"],
    "PEA325": ["FL"], "PEA326": ["FL"], "PEA327": ["SC"], "PEA328": ["NC"],
    "PEA329": ["VA"], "PEA330": ["WV"], "PEA331": ["KY"], "PEA332": ["TN"],
    "PEA333": ["TX"], "PEA334": ["TX"], "PEA335": ["TX"], "PEA336": ["TX"],
    "PEA337": ["TX"], "PEA338": ["OK"], "PEA339": ["KS"], "PEA340": ["NE"],
    "PEA341": ["SD"], "PEA342": ["ND"], "PEA343": ["MT"], "PEA344": ["WY"],
    "PEA345": ["CO"], "PEA346": ["NM"], "PEA347": ["AZ"], "PEA348": ["UT"],
    "PEA349": ["NV"], "PEA350": ["ID"], "PEA351": ["OR"], "PEA352": ["WA"],
    "PEA353": ["AK"], "PEA354": ["CA"], "PEA355": ["CA"], "PEA356": ["CA"],
    "PEA357": ["CA"], "PEA358": ["CA"], "PEA359": ["CA"], "PEA360": ["CA"],
    "PEA361": ["HI"], "PEA362": ["PR"], "PEA363": ["VI"],
    "PEA364": ["TX"], "PEA365": ["TX"], "PEA366": ["TX"], "PEA367": ["TX"],
    "PEA368": ["TX"], "PEA369": ["TX"], "PEA370": ["TX"],
    "PEA371": ["CA"], "PEA372": ["CA"], "PEA373": ["CA"],
    "PEA374": ["FL"], "PEA375": ["FL"], "PEA376": ["FL"],
    "PEA377": ["GA"], "PEA378": ["GA"],
    "PEA379": ["IL"], "PEA380": ["IL"],
    "PEA381": ["MI"], "PEA382": ["MI"],
    "PEA383": ["MN"], "PEA384": ["MN"],
    "PEA385": ["MO"], "PEA386": ["MO"],
    "PEA387": ["NY"], "PEA388": ["NY"],
    "PEA389": ["NC"], "PEA390": ["NC"],
    "PEA391": ["OH"], "PEA392": ["OH"],
    "PEA393": ["PA"], "PEA394": ["PA"],
    "PEA395": ["TN"], "PEA396": ["TN"],
    "PEA397": ["TX"], "PEA398": ["TX"],
    "PEA399": ["VA"], "PEA400": ["VA"],
    "PEA401": ["WA"], "PEA402": ["WA"],
    "PEA403": ["CO"], "PEA404": ["AZ"],
    "PEA405": ["IN"], "PEA406": ["WI"],
    "PEA407": ["AL"], "PEA408": ["LA"],
    "PEA409": ["KY"], "PEA410": ["SC"],
    "PEA411": ["AR"], "PEA412": ["IA"],
    "PEA413": ["OK"], "PEA414": ["KS"],
    "PEA415": ["NE"], "PEA416": ["MS"],
}

# Combined lookup
ALL_AREA_MAPS = {
    "P": PEA_TO_STATES,
    "E": EA_TO_STATES,
    "T": MTA_TO_STATES,
    "B": BTA_TO_STATES,
}

ALL_STATES = [
    "AK","AL","AR","AZ","CA","CO","CT","DC","DE","FL","GA","HI","IA","ID","IL",
    "IN","KS","KY","LA","MA","MD","ME","MI","MN","MO","MS","MT","NC","ND","NE",
    "NH","NJ","NM","NV","NY","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT",
    "VA","VT","WA","WI","WV","WY",
]


def area_to_states(area_type: str, area_code: str, hd_state: str = "") -> list[str]:
    """Resolve an FCC market area type+code to a list of state abbreviations."""
    t = area_type.strip().upper()

    # Nationwide
    if t in ("N", "NA"):
        return list(ALL_STATES)

    # State-licensed
    if t == "S":
        return [area_code.strip().upper()]

    # County — use the HD record's state as fallback
    if t == "C":
        return [hd_state.strip().upper()] if hd_state else []

    # Lookup in the maps
    lookup = ALL_AREA_MAPS.get(t, {})
    key = area_code.strip().upper()
    if key in lookup:
        return lookup[key]

    # Fallback: try stripping prefix and zero-padding
    # e.g. "PEA1" → "PEA001"
    m = re.match(r"([A-Z]+)(\d+)", key)
    if m:
        padded = f"{m.group(1)}{int(m.group(2)):03d}"
        if padded in lookup:
            return lookup[padded]

    # Last resort: use the HD record's state
    if hd_state:
        return [hd_state.strip().upper()]

    return []


def freq_to_band(lower_mhz: float, upper_mhz: float) -> int | None:
    """Map a frequency range (MHz) to an LTE/NR band number."""
    center = (lower_mhz + upper_mhz) / 2
    for lo, hi, band in FREQ_TO_BAND:
        if lo <= center <= hi:
            return band
    # Broader overlap check
    for lo, hi, band in FREQ_TO_BAND:
        if lower_mhz < hi and upper_mhz > lo:
            return band
    return None


# ── ULS parser ────────────────────────────────────────────────────────────────

def parse_uls_zip(zip_data: bytes, only_bands: set[int] | None = None) -> dict:
    """
    Parse a FCC ULS bulk ZIP file.
    Returns: {(carrier, state): set_of_band_numbers}
    """
    zf = zipfile.ZipFile(io.BytesIO(zip_data))

    # Index all .dat files by record type prefix
    dat_files: dict[str, list[str]] = defaultdict(list)
    for name in zf.namelist():
        if name.endswith(".dat") or name.endswith(".DAT"):
            prefix = name[:2].upper()
            dat_files[prefix].append(name)
        # Some archives use full names like "l_entity.dat"
        for rtype in ("HD", "EN", "MK", "FR"):
            for pattern in (f"l_{rtype.lower()}.dat", f"L_{rtype}.DAT", f"{rtype}.dat"):
                if name.lower() == pattern:
                    dat_files[rtype].append(name)

    print(f"  ZIP contains: {sorted(zf.namelist())}")

    # ── Load entity records (sys_id → carrier_name) ──────────────────────────
    entities: dict[str, str] = {}   # sys_id → carrier name or ""
    for fname in dat_files.get("EN", []) + dat_files.get("l_", []):
        if "en" not in fname.lower() and "entity" not in fname.lower():
            continue
        for line in zf.read(fname).decode("latin-1").splitlines():
            parts = line.split("|")
            if len(parts) < 5:
                continue
            rec_type = parts[1].strip().upper() if parts[0] == "" else parts[0].strip().upper()
            if rec_type != "EN":
                continue
            # EN field layout: |EN|sys_id|file_num|ebf|call_sign|entity_type|lic_id|entity_name|...
            try:
                idx = 1 if parts[0] == "" else 0
                sys_id      = parts[idx + 1].strip()
                entity_type = parts[idx + 5].strip().upper()
                entity_name = parts[idx + 7].strip()
                if entity_type in ("L", "O"):   # Licensee / Operator
                    carrier = normalize_carrier(entity_name)
                    if carrier:
                        entities[sys_id] = carrier
            except IndexError:
                continue

    print(f"  Recognized carrier entities: {len(entities)}")

    # ── Load market records (sys_id → [(area_type, area_code)]) ──────────────
    markets: dict[str, list[tuple[str, str, str]]] = defaultdict(list)
    for fname in dat_files.get("MK", []):
        for line in zf.read(fname).decode("latin-1").splitlines():
            parts = line.split("|")
            if len(parts) < 5:
                continue
            idx = 1 if parts[0] == "" else 0
            rec_type = parts[idx].strip().upper()
            if rec_type != "MK":
                continue
            try:
                # MK: |MK|sys_id|file_num|ebf|call_sign|market_area_type|market_area_code|partition|channel_block|partition_id|
                sys_id      = parts[idx + 1].strip()
                area_type   = parts[idx + 5].strip().upper()
                area_code   = parts[idx + 6].strip().upper()
                channel_blk = parts[idx + 8].strip().upper() if len(parts) > idx + 8 else ""
                markets[sys_id].append((area_type, area_code, channel_blk))
            except IndexError:
                continue

    # ── Load frequency records (sys_id → [(lower, upper)]) ───────────────────
    frequencies: dict[str, list[tuple[float, float]]] = defaultdict(list)
    for fname in dat_files.get("FR", []):
        for line in zf.read(fname).decode("latin-1").splitlines():
            parts = line.split("|")
            if len(parts) < 8:
                continue
            idx = 1 if parts[0] == "" else 0
            rec_type = parts[idx].strip().upper()
            if rec_type != "FR":
                continue
            try:
                sys_id     = parts[idx + 1].strip()
                freq_lower = float(parts[idx + 6].strip() or 0)
                freq_upper = float(parts[idx + 7].strip() or 0)
                if freq_lower > 0:
                    frequencies[sys_id].append((freq_lower, freq_upper or freq_lower))
            except (IndexError, ValueError):
                continue

    # ── Load HD records (sys_id → {status, channel_block, state}) ────────────
    hd_records: dict[str, dict] = {}
    for fname in dat_files.get("HD", []):
        for line in zf.read(fname).decode("latin-1").splitlines():
            parts = line.split("|")
            if len(parts) < 8:
                continue
            idx = 1 if parts[0] == "" else 0
            rec_type = parts[idx].strip().upper()
            if rec_type != "HD":
                continue
            try:
                sys_id  = parts[idx + 1].strip()
                status  = parts[idx + 5].strip().upper()
                channel = parts[idx + 47].strip().upper() if len(parts) > idx + 47 else ""
                state   = parts[idx + 17].strip().upper() if len(parts) > idx + 17 else ""
                hd_records[sys_id] = {"status": status, "channel": channel, "state": state}
            except IndexError:
                continue

    print(f"  HD records: {len(hd_records)}, MK records: {len(markets)}, "
          f"FR records: {len(frequencies)}")

    # ── Assemble (carrier, state) → bands ─────────────────────────────────────
    result: dict[tuple[str, str], set[int]] = defaultdict(set)
    skipped_no_carrier = skipped_inactive = skipped_no_area = matched = 0

    for sys_id, carrier in entities.items():
        hd = hd_records.get(sys_id, {})
        if hd.get("status", "A") not in ("A", ""):
            skipped_inactive += 1
            continue

        # Determine band from MK channel_block or HD channel_block
        bands_found: set[int] = set()

        # Try MK channel block first
        for area_type, area_code, ch_blk in markets.get(sys_id, []):
            band = BLOCK_TO_BAND.get(ch_blk)
            if band:
                bands_found.add(band)

        # Try HD channel block
        if not bands_found:
            ch = hd.get("channel", "")
            band = BLOCK_TO_BAND.get(ch)
            if band:
                bands_found.add(band)

        # Fall back to frequency records
        if not bands_found:
            for lo, hi in frequencies.get(sys_id, []):
                band = freq_to_band(lo, hi)
                if band:
                    bands_found.add(band)

        if not bands_found:
            continue

        # Filter to requested bands
        if only_bands:
            bands_found &= only_bands
        if not bands_found:
            continue

        # Resolve geographic areas
        states_found: set[str] = set()
        for area_type, area_code, _ in markets.get(sys_id, []):
            for st in area_to_states(area_type, area_code, hd.get("state", "")):
                if st in ALL_STATES:
                    states_found.add(st)

        # Fallback to HD state
        if not states_found and hd.get("state") in ALL_STATES:
            states_found.add(hd["state"])

        if not states_found:
            skipped_no_area += 1
            continue

        for state in states_found:
            result[(carrier, state)] |= bands_found
        matched += 1

    print(f"  Matched: {matched} | Skipped (inactive={skipped_inactive}, "
          f"no_area={skipped_no_area})")
    return result


# ── Download helper ───────────────────────────────────────────────────────────

def download_uls(service: str, force: bool = False) -> bytes:
    """Download (or load from cache) a FCC ULS bulk ZIP file."""
    CACHE_DIR.mkdir(exist_ok=True)
    cache_file = CACHE_DIR / f"l_{service}.zip"

    if cache_file.exists() and not force:
        print(f"  Using cached {cache_file.name}")
        return cache_file.read_bytes()

    url = ULS_URLS[service]
    print(f"  Downloading {url} ...")
    ctx = ssl.create_default_context()
    req = urllib.request.Request(url, headers={
        "User-Agent": "Mozilla/5.0 (compatible; CellfireAdmin/1.0)",
        "Accept": "*/*",
    })
    with urllib.request.urlopen(req, context=ctx, timeout=300) as r:
        data = r.read()
    cache_file.write_bytes(data)
    print(f"  Downloaded {len(data):,} bytes → {cache_file}")
    return data


# ── Build final JSON ──────────────────────────────────────────────────────────

def build_json(carrier_state_bands: dict[tuple[str, str], set[int]],
               existing: dict | None = None) -> dict:
    """
    Assemble state → carrier → sorted_bands dict.
    If existing is provided, merge (FCC data wins; unlisted carriers/bands preserved).
    """
    # Build fresh from FCC data
    output: dict[str, dict[str, list[int]]] = {}

    for (carrier, state), bands in carrier_state_bands.items():
        if state not in ALL_STATES:
            continue
        if state not in output:
            output[state] = {}
        current = set(output[state].get(carrier, []))
        output[state][carrier] = sorted(current | bands)

    # Merge with existing data (preserve carriers/bands not in FCC data)
    if existing:
        for state, carriers in existing.items():
            if state not in output:
                output[state] = {}
            for carrier, bands in carriers.items():
                if carrier not in output[state]:
                    # Carrier not found in FCC data — keep existing
                    output[state][carrier] = sorted(bands)
                else:
                    # Merge: union of FCC + existing
                    merged = sorted(set(output[state][carrier]) | set(bands))
                    output[state][carrier] = merged

    # Sort: states alphabetically, carriers alphabetically within state
    return {st: dict(sorted(output[st].items())) for st in sorted(output)}


# ── Main ──────────────────────────────────────────────────────────────────────

def load_auction_csv(csv_path: str) -> dict[tuple[str, str], set[int]]:
    """
    Parse an FCC auction results CSV (e.g. Auction 107/108/110).
    Returns {(carrier, state): set_of_band_numbers}
    """
    import csv
    result: dict[tuple[str, str], set[int]] = defaultdict(set)
    band_map = {
        "107": 77,   # C-Band
        "108": 41,   # 2.5 GHz
        "110": 77,   # 3.45 GHz (maps to n77)
    }
    # Detect auction number from filename
    fname = os.path.basename(csv_path)
    auction_band: int | None = None
    for anum, band in band_map.items():
        if f"a{anum}" in fname.lower() or f"auction{anum}" in fname.lower():
            auction_band = band
            break

    with open(csv_path, encoding="utf-8-sig", errors="replace") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Column names vary by auction; try common variants
            winner = (row.get("Winning Bidder") or row.get("Winner") or
                      row.get("winning_bidder") or "").strip()
            market = (row.get("Market") or row.get("market") or "").strip()
            if not winner:
                continue
            carrier = normalize_carrier(winner)
            if not carrier:
                continue

            # Extract state from market description (e.g. "PEA001 - New York, NY")
            state_match = re.search(r',\s*([A-Z]{2})\s*$', market)
            if state_match:
                state = state_match.group(1)
            else:
                # Try PEA lookup
                pea_match = re.match(r'PEA(\d+)', market)
                if pea_match:
                    pea_key = f"PEA{int(pea_match.group(1)):03d}"
                    states = PEA_TO_STATES.get(pea_key, [])
                    for st in states:
                        if auction_band and st in ALL_STATES:
                            result[(carrier, st)].add(auction_band)
                    continue
                continue

            if auction_band and state in ALL_STATES:
                result[(carrier, state)].add(auction_band)

    return result


def main():
    parser = argparse.ArgumentParser(description="Build band_license_map.json from FCC ULS data")
    parser.add_argument("--local",       metavar="FILE", help="Use a single local ZIP file")
    parser.add_argument("--local-cell",  metavar="FILE", help="Local l_cell.zip (Part 22 cellular)")
    parser.add_argument("--local-market",metavar="FILE", help="Local l_market.zip (Part 27 spectrum)")
    parser.add_argument("--service",  default="YM",   help="FCC ULS service code (default: YM = Part 27)")
    parser.add_argument("--force",    action="store_true", help="Re-download even if cache exists")
    parser.add_argument("--dry-run",  action="store_true", help="Print summary only, don't write JSON")
    parser.add_argument("--no-merge", action="store_true", help="Replace existing JSON entirely")
    parser.add_argument("--bands",    metavar="BAND", nargs="+", type=int,
                        help="Only process specific band numbers (e.g. --bands 77 71)")
    args = parser.parse_args()

    only_bands = set(args.bands) if args.bands else None

    carrier_state_bands: dict[tuple[str, str], set[int]] = defaultdict(set)

    # --- Load ULS ZIPs ---
    zip_sources: list[tuple[str, bytes]] = []

    if args.local_cell:
        print(f"Loading local cell file: {args.local_cell}")
        zip_sources.append(("cell", Path(args.local_cell).read_bytes()))
    if args.local_market:
        print(f"Loading local market file: {args.local_market}")
        zip_sources.append(("market", Path(args.local_market).read_bytes()))
    if args.local and not zip_sources:
        print(f"Loading local file: {args.local}")
        zip_sources.append(("local", Path(args.local).read_bytes()))
    if not zip_sources:
        print(f"Fetching FCC ULS service: {args.service}")
        zip_sources.append(("remote", download_uls(args.service, force=args.force)))

    # Parse each ZIP and merge results
    for label, zip_data in zip_sources:
        print(f"\nParsing ULS records ({label})...")
        partial = parse_uls_zip(zip_data, only_bands=only_bands)
        for key, bands in partial.items():
            carrier_state_bands[key] |= bands

    # --- Load auction CSVs from cache dir ---
    auction_csvs = list(CACHE_DIR.glob("a1*.csv")) if CACHE_DIR.exists() else []
    for csv_path in sorted(auction_csvs):
        print(f"Loading auction CSV: {csv_path.name}")
        partial = load_auction_csv(str(csv_path))
        for key, bands in partial.items():
            if not only_bands or bands & only_bands:
                carrier_state_bands[key] |= (bands & only_bands if only_bands else bands)

    # Summary
    carrier_totals: dict[str, int] = defaultdict(int)
    for (carrier, state), bands in carrier_state_bands.items():
        carrier_totals[carrier] += len(bands)

    print("\n-- Carrier band coverage found --")
    for carrier, count in sorted(carrier_totals.items(), key=lambda x: -x[1]):
        states = sorted(set(s for (c, s), _ in carrier_state_bands.items() if c == carrier))
        print(f"  {carrier:<20} {count:4d} band-state entries  ({len(states)} states)")

    if args.dry_run:
        print("\n[dry-run] Not writing output.")
        return

    # Load existing
    existing = None
    if JSON_OUT.exists() and not args.no_merge:
        with open(JSON_OUT, encoding="utf-8") as f:
            existing = json.load(f)
        print(f"\nMerging with existing {JSON_OUT.name}")

    # Build and write
    output = build_json(carrier_state_bands, existing)
    with open(JSON_OUT, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2)

    total_entries = sum(len(c) for c in output.values())
    print(f"\nWritten {JSON_OUT} -- {len(output)} states, {total_entries} carrier entries")


if __name__ == "__main__":
    main()
