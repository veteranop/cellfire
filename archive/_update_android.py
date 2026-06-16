#!/usr/bin/env python3
"""Update the Built Different and Why Pros sections in cf_android.php."""
import sys, os
sys.stdout = open(sys.stdout.fileno(), mode='w', encoding='utf-8', buffering=1)

src = os.path.join(os.path.dirname(__file__), 'site_template/articles/cf_android.php')
with open(src, encoding='utf-8') as f:
    content = f.read()

# Locate section boundaries
s1 = content.rfind('<!--', 0, content.find("HOW IT"))
s2 = content.rfind('<!--', 0, content.find("WHO USES"))
cta = content.rfind('<!--', 0, content.find("cf-cta-band"))

print(f"s1={s1}, s2={s2}, cta={cta}")

NEW_BUILT = """\
<!-- ═══════════════════════════════════════════════════

     HOW IT'S DIFFERENT

══════════════════════════════════════════════════════ -->

<div class="cf-page-section cf-page-section--alt">

\t<div class="cf-container">

\t\t<div class="cf-section-head">

\t\t\t<div class="cf-section-label">Built Different</div>

\t\t\t<h2>Your carrier has been hiding<br><span class="cf-accent">what your phone actually knows.</span></h2>

\t\t\t<p>Every Android device has a full RF modem reporting dozens of real-time metrics. Bars are a marketing decision. Cellfire bypasses all of it and reads the raw data directly.</p>

\t\t</div>

\t\t<div class="cf-diff-layout">

\t\t\t<div class="cf-diff-col cf-diff-col--bad cf-animate">

\t\t\t\t<div class="cf-diff-label cf-diff-label--bad">📵 Every Other Signal App</div>

\t\t\t\t<ul class="cf-diff-list">

\t\t\t\t\t<li>Shows you "bars" — a number your carrier controls, not your modem</li>

\t\t\t\t\t<li>Only reports your serving cell — the tower the carrier picked for you</li>

\t\t\t\t\t<li>Uploads your location and signal scans to their servers</li>

\t\t\t\t\t<li>Sells your RF data to carriers and analytics companies</li>

\t\t\t\t\t<li>Gives you 1 metric when your modem is producing 20+</li>

\t\t\t\t\t<li>Requires root, a carrier deal, or expensive hardware to go deeper</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t\t<div class="cf-diff-vs">VS</div>

\t\t\t<div class="cf-diff-col cf-diff-col--good cf-animate">

\t\t\t\t<div class="cf-diff-label cf-diff-label--good">✅ Cellfire</div>

\t\t\t\t<ul class="cf-diff-list">

\t\t\t\t\t<li>Direct modem access: RSRP, RSRQ, SINR, CQI, ARFCN, PCI, TAC, timing advance — all of it, live</li>

\t\t\t\t\t<li>Full neighbor cell list — <strong>every tower your phone hears</strong>, not just the one it's attached to</li>

\t\t\t\t\t<li><strong>Nothing leaves your device.</strong> No uploads, no accounts required to collect data</li>

\t\t\t\t\t<li>Your data is yours — export CSV, open in RF Studio, or share with your team</li>

\t\t\t\t\t<li>Per-cell full panel: band, frequency, power, quality, and cell identity in one view</li>

\t\t\t\t\t<li>Works on any unlocked Android — no root, no carrier permission, no expensive hardware</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t</div>

\t\t<div class="cf-diff-callout cf-animate">

\t\t\t<div class="cf-diff-callout-icon">🔒</div>

\t\t\t<div>

\t\t\t\t<strong>Zero data harvesting. Ever.</strong>

\t\t\t\t<span>Cellfire doesn't phone home with your scans, sell your location history, or aggregate your RF data. What you collect stays on your device — or goes exactly where <em>you</em> send it.</span>

\t\t\t</div>

\t\t</div>

\t</div>

</div>



"""

NEW_WHO = """\
<!-- ═══════════════════════════════════════════════════

     WHO USES CELLFIRE

══════════════════════════════════════════════════════ -->

<div class="cf-page-section">

\t<div class="cf-container">

\t\t<div class="cf-section-head">

\t\t\t<div class="cf-section-label">Who Uses Cellfire</div>

\t\t\t<h2>The tool serious RF people<br><span class="cf-accent">actually reach for.</span></h2>

\t\t\t<p>From DAS installs to carrier disputes — Cellfire gives you the ground truth that coverage maps and speed tests can't.</p>

\t\t</div>

\t\t<div class="cf-persona-grid cf-persona-grid--app">

\t\t\t<div class="cf-persona-card cf-animate">

\t\t\t\t<div class="cf-persona-header">

\t\t\t\t\t<div class="cf-persona-icon">🏗️</div>

\t\t\t\t\t<div>

\t\t\t\t\t\t<div class="cf-persona-role">DAS Engineers</div>

\t\t\t\t\t\t<div class="cf-persona-sub">In-building · Coverage Verification · Pre/Post Install</div>

\t\t\t\t\t</div>

\t\t\t\t</div>

\t\t\t\t<p>Walk a building floor by floor and collect GPS-stamped RSRP, SINR, and serving cell data at every waypoint — all carriers at once. Pre-install surveys that produce real data. Post-install verification that holds up in a report.</p>

\t\t\t\t<ul class="cf-persona-workflow">

\t\t\t\t\t<li>Map signal penetration by floor, room, and carrier before a single piece of cable goes in</li>

\t\t\t\t\t<li>See RSRP and SINR side-by-side — not just which bar you're at</li>

\t\t\t\t\t<li>Post-install: compare before/after measurements with exportable, GPS-tagged data</li>

\t\t\t\t\t<li>Feed measurements into RF Studio to compare against your propagation model</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t\t<div class="cf-persona-card cf-animate">

\t\t\t\t<div class="cf-persona-header">

\t\t\t\t\t<div class="cf-persona-icon">🚗</div>

\t\t\t\t\t<div>

\t\t\t\t\t\t<div class="cf-persona-role">Drive Test Techs</div>

\t\t\t\t\t\t<div class="cf-persona-sub">Coverage Surveys · Handoff Analysis · Network Audits</div>

\t\t\t\t\t</div>

\t\t\t\t</div>

\t\t\t\t<p>Start a session, drive your route, done. GPS-tagged signal data every second — no laptop, no external hardware, no setup ritual. Your phone does the work of a $15K drive test rig.</p>

\t\t\t\t<ul class="cf-persona-workflow">

\t\t\t\t\t<li>Continuous RSRP, RSRQ, SINR, and band logging along your entire route</li>

\t\t\t\t\t<li>PCI and ARFCN tracking to catch tower handoffs and coverage gaps in real time</li>

\t\t\t\t\t<li>Export clean CSV that feeds directly into your existing analysis tools</li>

\t\t\t\t\t<li>Cover more ground with less gear — your Android is your rig</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t\t<div class="cf-persona-card cf-animate">

\t\t\t\t<div class="cf-persona-header">

\t\t\t\t\t<div class="cf-persona-icon">💼</div>

\t\t\t\t\t<div>

\t\t\t\t\t\t<div class="cf-persona-role">Enterprise &amp; Facilities</div>

\t\t\t\t\t\t<div class="cf-persona-sub">Coverage Accountability · SLA Disputes · Dead Zone Hunting</div>

\t\t\t\t\t</div>

\t\t\t\t</div>

\t\t\t\t<p>You're paying for enterprise wireless and your team is dropping calls in the warehouse. Cellfire walks your facility and produces a GPS-mapped dataset that tells you exactly where coverage fails — so you bring <em>evidence</em> to the carrier conversation, not complaints.</p>

\t\t\t\t<ul class="cf-persona-workflow">

\t\t\t\t\t<li>Walk every floor, wing, and loading dock — map actual RSRP levels, not carrier claims</li>

\t\t\t\t\t<li>Identify whether dead zones are carrier-specific or affect everyone</li>

\t\t\t\t\t<li>Build a coverage baseline to hold carriers accountable to their SLA commitments</li>

\t\t\t\t\t<li>Show up to the meeting with GPS-tagged measurement data — not anecdotes</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t\t<div class="cf-persona-card cf-animate">

\t\t\t\t<div class="cf-persona-header">

\t\t\t\t\t<div class="cf-persona-icon">🚐</div>

\t\t\t\t\t<div>

\t\t\t\t\t\t<div class="cf-persona-role">Remote Workers &amp; VanLife</div>

\t\t\t\t\t\t<div class="cf-persona-sub">Campsite Signal · Carrier Comparison · Real Data</div>

\t\t\t\t\t</div>

\t\t\t\t</div>

\t\t\t\t<p>Parked at a new campsite, debating which SIM to run? Cellfire shows every carrier's actual RSRP at your exact location — not marketing maps. Know whether you're on a strong LTE band or crawling off a distant tower before you commit.</p>

\t\t\t\t<ul class="cf-persona-workflow">

\t\t\t\t\t<li>See all carriers' RSRP simultaneously at your current location</li>

\t\t\t\t\t<li>Check what band and frequency you're on — B12, B71, n41, or NR SA</li>

\t\t\t\t\t<li>Identify the nearest tower PCIs to understand your actual options</li>

\t\t\t\t\t<li>Make informed carrier vs. Starlink decisions with real RF data, not guesses</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t\t<div class="cf-persona-card cf-animate">

\t\t\t\t<div class="cf-persona-header">

\t\t\t\t\t<div class="cf-persona-icon">🔧</div>

\t\t\t\t\t<div>

\t\t\t\t\t\t<div class="cf-persona-role">Builders &amp; Tinkerers</div>

\t\t\t\t\t\t<div class="cf-persona-sub">Antenna Builds · RF Shielding · SDR · Signal Optimization</div>

\t\t\t\t\t</div>

\t\t\t\t</div>

\t\t\t\t<p>Does your rooftop antenna build actually do anything? Cellfire shows RSRP live as you adjust orientation and height. Build, measure, iterate — with real numbers instead of speed test variance.</p>

\t\t\t\t<ul class="cf-persona-workflow">

\t\t\t\t\t<li>Watch RSRP change live as you rotate, tilt, or reposition an external antenna</li>

\t\t\t\t\t<li>Test RF shielding effectiveness with before/after measurements</li>

\t\t\t\t\t<li>Map signal leakage in your workspace to hunt down interference sources</li>

\t\t\t\t\t<li>Compare antenna builds objectively — logged RSRP per position, exported to CSV</li>

\t\t\t\t</ul>

\t\t\t</div>

\t\t</div>

\t</div>

</div>



"""

new_content = content[:s1] + NEW_BUILT + NEW_WHO + content[cta:]

with open(src, 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f"Done. Length: {len(new_content)}")
print(f"Has new built: {'Your carrier has been hiding' in new_content}")
print(f"Has new pros: {'serious RF people' in new_content}")
