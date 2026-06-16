import csv
import sys
from collections import defaultdict, Counter

FILES = {
    "File1_TM": r"C:\Users\markd\Downloads\CellFire_ALL_1774207762125.csv",
    "File2":    r"C:\Users\markd\Downloads\CellFire_ALL_1774207916561.csv",
    "File3":    r"C:\Users\markd\Proton Drive\Tomato35\My files\CellFire_ALL_1774207974254.csv",
    "File4":    r"C:\Users\markd\Downloads\CellFire_ALL_1774208042202.csv",
}

def safe_float(v):
    try: return float(v)
    except: return None

def safe_int(v):
    try: return int(v)
    except: return None

# ---- Load all data ----
all_rows = []
file_data = {}

for label, path in FILES.items():
    rows = []
    with open(path, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
            all_rows.append((label, row))
    file_data[label] = rows
    print(f"Loaded {label}: {len(rows)} rows from {path.split(chr(92))[-1]}")

print()

# ==================================================================
# 1. Per-file summary
# ==================================================================
print("=" * 70)
print("SECTION 1: PER-FILE SUMMARY")
print("=" * 70)

for label, rows in file_data.items():
    reg_rows = [r for r in rows if r.get('Registered','').lower() == 'true']
    carriers_reg = Counter(r['Carrier'] for r in reg_rows)
    bands_reg = Counter(r['Band'] for r in reg_rows)
    pcis_reg = Counter(r['PCI'] for r in reg_rows)
    lats = [safe_float(r['Latitude']) for r in rows if safe_float(r['Latitude']) is not None]
    lons = [safe_float(r['Longitude']) for r in rows if safe_float(r['Longitude']) is not None]

    print(f"\n--- {label} ---")
    print(f"  Total rows: {len(rows)}")
    print(f"  Registered=true rows: {len(reg_rows)}")
    print(f"  Registered carriers: {dict(carriers_reg.most_common())}")
    print(f"  Dominant bands (reg): {dict(bands_reg.most_common(8))}")
    print(f"  Dominant PCIs (reg, top 10): {dict(pcis_reg.most_common(10))}")
    types = Counter(r['Type'] for r in rows)
    print(f"  Cell types: {dict(types)}")
    if lats:
        print(f"  Geo bbox: lat [{min(lats):.5f}, {max(lats):.5f}]  lon [{min(lons):.5f}, {max(lons):.5f}]")

print()

# ==================================================================
# 2. Combined carrier distribution
# ==================================================================
print("=" * 70)
print("SECTION 2: COMBINED CARRIER DISTRIBUTION (all rows)")
print("=" * 70)
all_carriers = Counter()
for label, row in all_rows:
    all_carriers[row['Carrier']] += 1
for c, n in all_carriers.most_common():
    print(f"  {c}: {n}")

print()
print("  Registered=true only:")
reg_carriers = Counter()
for label, row in all_rows:
    if row.get('Registered','').lower() == 'true':
        reg_carriers[row['Carrier']] += 1
for c, n in reg_carriers.most_common():
    print(f"  {c}: {n}")

print()

# ==================================================================
# 3. Combined unique PCIs grouped by carrier
# ==================================================================
print("=" * 70)
print("SECTION 3: UNIQUE PCIs BY CARRIER ASSIGNMENT (registered rows)")
print("=" * 70)
carrier_pci_set = defaultdict(set)
for label, row in all_rows:
    if row.get('Registered','').lower() == 'true':
        carrier_pci_set[row['Carrier']].add(row['PCI'])

for carrier in sorted(carrier_pci_set):
    pcis = sorted(carrier_pci_set[carrier], key=lambda x: int(x) if x.isdigit() else 9999)
    print(f"  {carrier}: {len(pcis)} unique PCIs")
    print(f"    PCIs: {', '.join(pcis[:40])}{'...' if len(pcis) > 40 else ''}")

print()

# ==================================================================
# 4. Cross-phone validation
# ==================================================================
print("=" * 70)
print("SECTION 4: CROSS-PHONE PCI CARRIER VALIDATION")
print("=" * 70)

file_pci_carrier = {}
for label, rows in file_data.items():
    d = defaultdict(Counter)
    for row in rows:
        if row.get('Registered','').lower() == 'true':
            d[row['PCI']][row['Carrier']] += 1
    file_pci_carrier[label] = {pci: ctr.most_common(1)[0][0] for pci, ctr in d.items()}

pci_files = defaultdict(list)
for label in FILES:
    for pci, carrier in file_pci_carrier[label].items():
        pci_files[pci].append((label, carrier))

multi_pci = {pci: entries for pci, entries in pci_files.items() if len(entries) > 1}
agreements = []
disagreements = []
for pci, entries in sorted(multi_pci.items(), key=lambda x: int(x[0]) if x[0].isdigit() else 9999):
    carriers_seen = set(e[1] for e in entries)
    if len(carriers_seen) == 1:
        agreements.append((pci, entries))
    else:
        disagreements.append((pci, entries))

print(f"  PCIs seen in multiple files: {len(multi_pci)}")
print(f"  Agreements (same carrier): {len(agreements)}")
print(f"  DISAGREEMENTS (different carrier for same PCI): {len(disagreements)}")
if disagreements:
    print()
    print("  *** DISAGREEMENTS ***")
    for pci, entries in disagreements:
        print(f"    PCI {pci}:")
        for file, carrier in entries:
            print(f"      {file}: {carrier}")
        # Show band/earfcn context for each disagreement
        for file, carrier in entries:
            bands_seen = Counter()
            earfcns_seen = Counter()
            for lbl, row in all_rows:
                if lbl == file and row['PCI'] == pci and row.get('Registered','').lower() == 'true':
                    bands_seen[row['Band']] += 1
                    earfcns_seen[row['EARFCN']] += 1
            print(f"      {file} bands: {dict(bands_seen.most_common(5))}  earfcns: {dict(earfcns_seen.most_common(3))}")
else:
    print("  (No disagreements found)")

if agreements:
    print()
    print("  Agreeing multi-phone PCIs:")
    for pci, entries in agreements:
        carrier = entries[0][1]
        files = [e[0] for e in entries]
        print(f"    PCI {pci} -> {carrier}  ({', '.join(files)})")

print()

# ==================================================================
# 5. Unknown cells
# ==================================================================
print("=" * 70)
print("SECTION 5: UNKNOWN CARRIER CELLS")
print("=" * 70)
unknown_pci = defaultdict(lambda: {'earfcn': Counter(), 'band': Counter(), 'files': Counter(), 'count': 0})
for label, row in all_rows:
    if row['Carrier'] == 'Unknown':
        pci = row['PCI']
        unknown_pci[pci]['earfcn'][row['EARFCN']] += 1
        unknown_pci[pci]['band'][row['Band']] += 1
        unknown_pci[pci]['files'][label] += 1
        unknown_pci[pci]['count'] += 1

print(f"  Total unique Unknown PCIs: {len(unknown_pci)}")
for pci in sorted(unknown_pci.keys(), key=lambda x: int(x) if x.isdigit() else 9999):
    d = unknown_pci[pci]
    top_earfcn = d['earfcn'].most_common(3)
    top_band = d['band'].most_common(3)
    print(f"  PCI {pci}: count={d['count']}  band={[b+':'+str(n) for b,n in top_band]}  earfcn={[e+':'+str(n) for e,n in top_earfcn]}  files={dict(d['files'])}")

print()

# ==================================================================
# 6. Gold records
# ==================================================================
print("=" * 70)
print("SECTION 6: GOLD RECORDS (Registered=true, TAC>0, carrier known)")
print("=" * 70)
gold = {}
for label, row in all_rows:
    if row.get('Registered','').lower() != 'true':
        continue
    try:
        tac = int(row['TAC'])
    except:
        continue
    if tac <= 0:
        continue
    carrier = row['Carrier']
    if not carrier or carrier == 'Unknown':
        continue
    pci = row['PCI']
    band = row['Band']
    earfcn = row['EARFCN']
    key = (pci, str(tac), carrier, band, earfcn)
    lat = safe_float(row['Latitude'])
    lon = safe_float(row['Longitude'])
    if key not in gold:
        gold[key] = {'lats': [], 'lons': [], 'count': 0, 'files': set()}
    gold[key]['count'] += 1
    gold[key]['files'].add(label)
    if lat is not None: gold[key]['lats'].append(lat)
    if lon is not None: gold[key]['lons'].append(lon)

print(f"  Total gold records: {len(gold)}")
print()
hdr = f"  {'PCI':<6} {'TAC':<7} {'Carrier':<18} {'Band':<6} {'EARFCN':<8} {'Obs':<6} {'Lat':<10} {'Lon':<12} Files"
print(hdr)
print("  " + "-"*90)
for key in sorted(gold.keys(), key=lambda k: (k[2], k[3], int(k[0]) if k[0].isdigit() else 9999)):
    pci, tac, carrier, band, earfcn = key
    d = gold[key]
    avg_lat = sum(d['lats'])/len(d['lats']) if d['lats'] else 0
    avg_lon = sum(d['lons'])/len(d['lons']) if d['lons'] else 0
    files_str = '+'.join(sorted(d['files']))
    print(f"  {pci:<6} {tac:<7} {carrier:<18} {band:<6} {earfcn:<8} {d['count']:<6} {avg_lat:<10.5f} {avg_lon:<12.5f} {files_str}")

print()

# ==================================================================
# 7. TACs per carrier
# ==================================================================
print("=" * 70)
print("SECTION 7: TAC VALUES PER CARRIER (registered, TAC>0)")
print("=" * 70)
carrier_tac = defaultdict(set)
for label, row in all_rows:
    if row.get('Registered','').lower() == 'true':
        try:
            tac = int(row['TAC'])
        except:
            continue
        if tac > 0:
            carrier_tac[row['Carrier']].add(tac)
for carrier in sorted(carrier_tac):
    tacs = sorted(carrier_tac[carrier])
    print(f"  {carrier}: {tacs}")

print()

# ==================================================================
# 8. PCIs on both B12 and B66
# ==================================================================
print("=" * 70)
print("SECTION 8: PCIs APPEARING ON BOTH B12 AND B66")
print("=" * 70)
pci_bands = defaultdict(set)
for label, row in all_rows:
    pci_bands[row['PCI']].add(row['Band'])
b12_b66_both = {pci: bands for pci, bands in pci_bands.items() if 'B12' in bands and 'B66' in bands}
if b12_b66_both:
    print(f"  WARNING: {len(b12_b66_both)} PCIs appear on both B12 and B66:")
    for pci, bands in sorted(b12_b66_both.items(), key=lambda x: int(x[0]) if x[0].isdigit() else 9999):
        b12_carriers = Counter()
        b66_carriers = Counter()
        for label, row in all_rows:
            if row['PCI'] == pci:
                if row['Band'] == 'B12': b12_carriers[row['Carrier']] += 1
                if row['Band'] == 'B66': b66_carriers[row['Carrier']] += 1
        print(f"    PCI {pci}: bands={sorted(bands)}")
        print(f"      B12 carriers: {dict(b12_carriers)}")
        print(f"      B66 carriers: {dict(b66_carriers)}")
else:
    print("  None found. (No B12/B66 cross-contamination)")

print()

# ==================================================================
# 9. Geographic bounding box
# ==================================================================
print("=" * 70)
print("SECTION 9: GEOGRAPHIC BOUNDING BOX PER FILE")
print("=" * 70)
for label, rows in file_data.items():
    lats = [safe_float(r['Latitude']) for r in rows if safe_float(r['Latitude']) is not None]
    lons = [safe_float(r['Longitude']) for r in rows if safe_float(r['Longitude']) is not None]
    if lats:
        print(f"  {label}:")
        print(f"    Lat: {min(lats):.6f} to {max(lats):.6f}  (span {max(lats)-min(lats):.4f} deg)")
        print(f"    Lon: {min(lons):.6f} to {max(lons):.6f}  (span {max(lons)-min(lons):.4f} deg)")
        clat = (min(lats)+max(lats))/2
        clon = (min(lons)+max(lons))/2
        print(f"    Center: {clat:.5f}, {clon:.5f}")

print()
print("Done.")
