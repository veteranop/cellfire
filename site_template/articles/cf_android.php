<?php

/**

 * Cellfire Template — Article Layout: Android App Feature Article

 * Assign to an article, set menu alias to "app" or "android"

 */

defined('_JEXEC') or die;



$imgBase = 'https://cellfire.io/images/Cellfire_app/';
$appVersion = '1.0.1.22'; // APP_VERSION

$slides = [

	[

		'img'     => $imgBase . 'Screenshot%202026-03-01%20190817.png',

		'tag'     => 'Deep Scan',

		'title'   => 'All carriers. All bands. All at once.',

		'body'    => 'Cellfire\'s Deep Scan mode pulls live signal data from every visible tower simultaneously. Verizon, AT&T, T-Mobile — watch them compete for your device in real time.',

		'stats'   => [['−91 dBm', 'RSRP'], ['B2 / B13', 'Bands'], ['LTE', 'Type']],

	],

	[

		'img'     => $imgBase . 'Screenshot%202026-03-01%20190853.png',

		'tag'     => 'Cell Detail',

		'title'   => 'Every metric. Live. On one screen.',

		'body'    => 'Tap any cell to open a full detail panel. PCI, ARFCN, TAC, DL/UL frequencies, RSRP, SINR, RSRQ — plus a rolling real-time signal graph that updates every second.',

		'stats'   => [['1980 MHz', 'DL Freq'], ['PCI 308', 'Cell ID'], ['−7 dB', 'SINR']],

	],

	[

		'img'     => $imgBase . 'Screenshot%202026-03-01%20191142.png',

		'tag'     => 'Drive Test Map',

		'title'   => 'Map your coverage as you drive.',

		'body'    => 'Every scan is GPS-stamped and plotted on an interactive map in real time. Color-coded red to green by signal strength — so dead zones are impossible to miss.',

		'stats'   => [['GPS', 'Tagged'], ['Live', 'Plotting'], ['Export', 'Ready']],

	],

	[

		'img'     => $imgBase . 'Screenshot%202026-03-01%20191234.png',

		'tag'     => 'PCI Discovery',

		'title'   => 'Discover every cell tower in range.',

		'body'    => 'Cellfire logs every PCI it detects, broken down by carrier. AT&T, Verizon, T-Mobile, Dish, FirstNet, US Cellular — one tap exports the full dataset for further analysis.',

		'stats'   => [['49', 'Verizon PCIs'], ['37', 'T-Mobile PCIs'], ['28', 'AT&T PCIs']],

	],

];

?>



<div class="cf-page-wrap">



<!-- ═══════════════════════════════════════════════════

     HERO

══════════════════════════════════════════════════════ -->

<div class="cf-page-hero cf-android-hero">

	<div class="cf-container">

		<div class="cf-android-hero-inner">

			<div>

				<div class="cf-page-hero-label">Android App</div>

				<h1>Cellfire for <span class="cf-accent">Android</span></h1>

				<p>The most capable cellular RF scanner you can hold in your hand. Real-time multi-carrier scanning, GPS drive testing, deep cell analysis — all from your pocket.</p>

				<div class="cf-page-hero-actions">

					<a href="/downloads" class="cf-btn cf-btn--primary">

						<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M3.18 23.76c.3.17.64.22.99.14l12.6-7.17-2.66-2.71-10.93 9.74zm-1.05-18.9c-.08.23-.13.49-.13.78v16.72c0 .29.05.55.13.78l.08.08 9.37-9.37v-.16L2.21 4.78l-.08.08zm19.61 8.77l-2.67-1.52-2.9 2.9 2.9 2.9 2.69-1.53c.76-.44.76-1.34-.02-1.75zm-17.95 9.35l10.61-10.61-2.66-2.66L3.13 5.06c-.1.1-.13.25-.05.37l.72.72z"/></svg>

						Get it on Google Play

					</a>

					<a href="/pricing" class="cf-btn cf-btn--ghost">View Plans</a>

				</div>

				<div class="cf-android-meta">

					<span class="cf-android-badge"><?= $appVersion ?> Stable</span>

					<span class="cf-android-badge">Android 10+</span>

					<span class="cf-android-badge">Cellfire</span>

				</div>

			</div>

		</div>

	</div>

</div>



<!-- ═══════════════════════════════════════════════════

     SLIDESHOW

══════════════════════════════════════════════════════ -->

<div class="cf-slideshow-section">

	<div class="cf-container">

		<div class="cf-slideshow-wrap">



			<!-- Left: text panel -->

			<div class="cf-slide-text-panel">

				<?php foreach ($slides as $i => $s) : ?>

				<div class="cf-slide-text <?= $i === 0 ? 'is-active' : ''; ?>" data-slide="<?= $i; ?>">

					<div class="cf-slide-tag"><?= $s['tag']; ?></div>

					<h2><?= $s['title']; ?></h2>

					<p><?= $s['body']; ?></p>

					<div class="cf-slide-stats">

						<?php foreach ($s['stats'] as $stat) : ?>

						<div class="cf-slide-stat">

							<div class="cf-slide-stat-val"><?= $stat[0]; ?></div>

							<div class="cf-slide-stat-lbl"><?= $stat[1]; ?></div>

						</div>

						<?php endforeach; ?>

					</div>

				</div>

				<?php endforeach; ?>



				<!-- Dot nav -->

				<div class="cf-slide-dots">

					<?php foreach ($slides as $i => $s) : ?>

					<button class="cf-slide-dot <?= $i === 0 ? 'is-active' : ''; ?>"

							data-slide="<?= $i; ?>" aria-label="Go to slide <?= $i + 1; ?>"></button>

					<?php endforeach; ?>

				</div>



				<!-- Progress bar -->

				<div class="cf-slide-progress"><div class="cf-slide-progress-bar"></div></div>

			</div>



			<!-- Right: phone mockup -->

			<div class="cf-phone-wrap">

				<div class="cf-phone-frame">

					<div class="cf-phone-notch"></div>

					<div class="cf-phone-screen">

						<?php foreach ($slides as $i => $s) : ?>

						<img class="cf-phone-img <?= $i === 0 ? 'is-active' : ''; ?>"

							 src="<?= $s['img']; ?>"

							 alt="Cellfire App - <?= htmlspecialchars($s['tag']); ?>"

							 data-slide="<?= $i; ?>"

							 loading="<?= $i === 0 ? 'eager' : 'lazy'; ?>">

						<?php endforeach; ?>

					</div>

					<div class="cf-phone-home"></div>

				</div>

				<!-- Glow under phone -->

				<div class="cf-phone-glow"></div>

			</div>



		</div>

	</div>

</div>



<!-- ═══════════════════════════════════════════════════

     HOW IT'S DIFFERENT

══════════════════════════════════════════════════════ -->

<div class="cf-page-section cf-page-section--alt">

	<div class="cf-container">

		<div class="cf-section-head">

			<div class="cf-section-label">Built Different</div>

			<h2>Your carrier has been hiding<br><span class="cf-accent">what your phone actually knows.</span></h2>

			<p>Every Android device has a full RF modem reporting dozens of real-time metrics. Bars are a marketing decision. Cellfire bypasses all of it and reads the raw data directly.</p>

		</div>

		<div class="cf-diff-vs">

			<div class="cf-diff-vs-label">VS</div>

			<div class="cf-diff-vs-body">

				<p>Most apps never touch your modem. They report one of two things:</p>

				<ol>

					<li><strong>Your registered carrier's signal</strong> — the number your carrier chooses to surface, optimized to look good</li>

					<li><strong>Crowdsourced data</strong> — what other users near your GPS location have reported, averaged and smoothed</li>

				</ol>

				<p>Neither is your phone's actual RF state. Neither shows what your modem sees right now.</p>

				<p class="cf-diff-vs-emphasis">Cellfire reads the modem directly. No middleman. No averages. No carrier filter.</p>

			</div>

		</div>

		<div class="cf-diff-cols">

			<div class="cf-diff-col cf-diff-col--bad">

				<div class="cf-diff-label cf-diff-label--bad">📵 Every Other Signal App</div>

				<ul class="cf-diff-list">

					<li>Shows you "bars" — a number your carrier controls, not your modem</li>

					<li>Only reports your serving cell — the tower the carrier picked for you</li>

					<li>Uploads your location and signal scans to their servers</li>

					<li>Sells your RF data to carriers and analytics companies</li>

					<li>Gives you 1 metric when your modem is producing 20+</li>

					<li>Requires root, a carrier deal, or expensive hardware to go deeper</li>

				</ul>

			</div>

			<div class="cf-diff-col cf-diff-col--good">

				<div class="cf-diff-label cf-diff-label--good">✅ Cellfire</div>

				<ul class="cf-diff-list">

					<li>Direct modem access: RSRP, RSRQ, SINR, CQI, ARFCN, PCI, TAC, timing advance — all of it, live</li>

					<li>Full neighbor cell list — <strong>every tower your phone hears</strong>, not just the one it's attached to</li>

					<li><strong>Nothing leaves your device.</strong> No uploads, no accounts required to collect data</li>

					<li>Your data is yours — export CSV, open in RF Studio, or share with your team</li>

					<li>Per-cell full panel: band, frequency, power, quality, and cell identity in one view</li>

					<li>Works on any unlocked Android — no root, no carrier permission, no expensive hardware</li>

				</ul>

			</div>

		</div>

		<div class="cf-diff-callout cf-animate">

			<div class="cf-diff-callout-icon">🔒</div>

			<div>

				<strong>Zero data harvesting. Ever.</strong>

				<span>Cellfire doesn't phone home with your scans, sell your location history, or aggregate your RF data. What you collect stays on your device — or goes exactly where <em>you</em> send it.</span>

			</div>

		</div>

	</div>

</div>



<!-- ═══════════════════════════════════════════════════

     WHO USES CELLFIRE

══════════════════════════════════════════════════════ -->

<div class="cf-page-section">

	<div class="cf-container">

		<div class="cf-section-head">

			<div class="cf-section-label">Who Uses Cellfire</div>

			<h2>The tool serious RF people<br><span class="cf-accent">actually reach for.</span></h2>

			<p>From DAS installs to carrier disputes — Cellfire gives you the ground truth that coverage maps and speed tests can't.</p>

		</div>

		<div class="cf-persona-grid cf-persona-grid--app">

			<div class="cf-persona-card cf-animate">

				<div class="cf-persona-header">

					<div class="cf-persona-icon">🏗️</div>

					<div>

						<div class="cf-persona-role">DAS Engineers</div>

						<div class="cf-persona-sub">In-building · Coverage Verification · Pre/Post Install</div>

					</div>

				</div>

				<p>Walk a building floor by floor and collect GPS-stamped RSRP, SINR, and serving cell data at every waypoint — all carriers at once. Pre-install surveys that produce real data. Post-install verification that holds up in a report.</p>

				<ul class="cf-persona-workflow">

					<li>Map signal penetration by floor, room, and carrier before a single piece of cable goes in</li>

					<li>See RSRP and SINR side-by-side — not just which bar you're at</li>

					<li>Post-install: compare before/after measurements with exportable, GPS-tagged data</li>

					<li>Feed measurements into RF Studio to compare against your propagation model</li>

				</ul>

			</div>

			<div class="cf-persona-card cf-animate">

				<div class="cf-persona-header">

					<div class="cf-persona-icon">🚗</div>

					<div>

						<div class="cf-persona-role">Drive Test Techs</div>

						<div class="cf-persona-sub">Coverage Surveys · Handoff Analysis · Network Audits</div>

					</div>

				</div>

				<p>Start a session, drive your route, done. GPS-tagged signal data every second — no laptop, no external hardware, no setup ritual. Your phone does the work of a $15K drive test rig.</p>

				<ul class="cf-persona-workflow">

					<li>Continuous RSRP, RSRQ, SINR, and band logging along your entire route</li>

					<li>PCI and ARFCN tracking to catch tower handoffs and coverage gaps in real time</li>

					<li>Export clean CSV that feeds directly into your existing analysis tools</li>

					<li>Cover more ground with less gear — your Android is your rig</li>

				</ul>

			</div>

			<div class="cf-persona-card cf-animate">

				<div class="cf-persona-header">

					<div class="cf-persona-icon">💼</div>

					<div>

						<div class="cf-persona-role">Enterprise &amp; Facilities</div>

						<div class="cf-persona-sub">Coverage Accountability · SLA Disputes · Dead Zone Hunting</div>

					</div>

				</div>

				<p>You're paying for enterprise wireless and your team is dropping calls in the warehouse. Cellfire walks your facility and produces a GPS-mapped dataset that tells you exactly where coverage fails — so you bring <em>evidence</em> to the carrier conversation, not complaints.</p>

				<ul class="cf-persona-workflow">

					<li>Walk every floor, wing, and loading dock — map actual RSRP levels, not carrier claims</li>

					<li>Identify whether dead zones are carrier-specific or affect everyone</li>

					<li>Build a coverage baseline to hold carriers accountable to their SLA commitments</li>

					<li>Show up to the meeting with GPS-tagged measurement data — not anecdotes</li>

				</ul>

			</div>

			<div class="cf-persona-card cf-animate">

				<div class="cf-persona-header">

					<div class="cf-persona-icon">🚐</div>

					<div>

						<div class="cf-persona-role">Remote Workers &amp; VanLife</div>

						<div class="cf-persona-sub">Campsite Signal · Carrier Comparison · Real Data</div>

					</div>

				</div>

				<p>Parked at a new campsite, debating which SIM to run? Cellfire shows every carrier's actual RSRP at your exact location — not marketing maps. Know whether you're on a strong LTE band or crawling off a distant tower before you commit.</p>

				<ul class="cf-persona-workflow">

					<li>See all carriers' RSRP simultaneously at your current location</li>

					<li>Check what band and frequency you're on — B12, B71, n41, or NR SA</li>

					<li>Identify the nearest tower PCIs to understand your actual options</li>

					<li>Make informed carrier vs. Starlink decisions with real RF data, not guesses</li>

				</ul>

			</div>

			<div class="cf-persona-card cf-animate">

				<div class="cf-persona-header">

					<div class="cf-persona-icon">🔧</div>

					<div>

						<div class="cf-persona-role">Builders &amp; Tinkerers</div>

						<div class="cf-persona-sub">Antenna Builds · RF Shielding · SDR · Signal Optimization</div>

					</div>

				</div>

				<p>Does your rooftop antenna build actually do anything? Cellfire shows RSRP live as you adjust orientation and height. Build, measure, iterate — with real numbers instead of speed test variance.</p>

				<ul class="cf-persona-workflow">

					<li>Watch RSRP change live as you rotate, tilt, or reposition an external antenna</li>

					<li>Test RF shielding effectiveness with before/after measurements</li>

					<li>Map signal leakage in your workspace to hunt down interference sources</li>

					<li>Compare antenna builds objectively — logged RSRP per position, exported to CSV</li>

				</ul>

			</div>

		</div>

	</div>

</div>



<!-- ═══════════════════════════════════════════════════

     CTA

══════════════════════════════════════════════════════ -->

<div class="cf-cta-band">

	<div class="cf-container">

		<h2>Your field kit just got a <span class="cf-accent">major upgrade.</span></h2>

		<p>Available with all Cellfire subscription plans. Download free, sign in, start scanning.</p>

		<div class="cf-hero-actions">

			<a href="/downloads" class="cf-btn cf-btn--primary">

				<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M3.18 23.76c.3.17.64.22.99.14l12.6-7.17-2.66-2.71-10.93 9.74zm-1.05-18.9c-.08.23-.13.49-.13.78v16.72c0 .29.05.55.13.78l.08.08 9.37-9.37v-.16L2.21 4.78l-.08.08zm19.61 8.77l-2.67-1.52-2.9 2.9 2.9 2.9 2.69-1.53c.76-.44.76-1.34-.02-1.75zm-17.95 9.35l10.61-10.61-2.66-2.66L3.13 5.06c-.1.1-.13.25-.05.37l.72.72z"/></svg>

				Google Play

			</a>

			<a href="/pricing" class="cf-btn cf-btn--ghost">View Plans</a>

			<a href="/studio" class="cf-btn cf-btn--ghost">Pair with RF Studio →</a>

		</div>

	</div>

</div>



<?php if (!empty($this->item->text)) : ?>

<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>

<?php endif; ?>



</div><!-- /.cf-page-wrap -->





<!-- ═══════════════════════════════════════════════════

     SLIDESHOW CSS

══════════════════════════════════════════════════════ -->

<style>

/* Hero tweak for android page */

.cf-android-hero-inner { max-width: 640px; }

.cf-android-meta {

	display: flex; gap: 8px; flex-wrap: wrap; margin-top: 20px;

}

.cf-android-badge {

	font-size: 11px; font-weight: 600; letter-spacing: .5px;

	color: var(--cf-text-2);

	background: var(--cf-bg-raised);

	border: 1px solid var(--cf-border-2);

	border-radius: var(--cf-r-full);

	padding: 3px 10px;

}



/* ── Slideshow section ── */

.cf-slideshow-section {

	padding: 80px 0 100px;

	background: var(--cf-bg);

	position: relative;

	overflow: hidden;

}

.cf-slideshow-section::before {

	content: '';

	position: absolute;

	inset: 0;

	background: radial-gradient(ellipse 60% 80% at 70% 50%, rgba(88,166,255,.05) 0%, transparent 70%);

	pointer-events: none;

}

.cf-slideshow-wrap {

	display: grid;

	grid-template-columns: 1fr 420px;

	gap: 64px;

	align-items: center;

	position: relative;

	z-index: 2;

}



/* ── Text panel ── */

.cf-slide-text-panel {

	position: relative;

	min-height: 340px;

	display: flex;

	flex-direction: column;

	justify-content: center;

}

.cf-slide-text {

	position: absolute;

	top: 0; left: 0; right: 0;

	opacity: 0;

	transform: translateY(12px);

	transition: opacity .5s ease, transform .5s ease;

	pointer-events: none;

}

.cf-slide-text.is-active {

	opacity: 1;

	transform: translateY(0);

	pointer-events: auto;

	position: relative;

}

.cf-slide-tag {

	font-size: 11px; font-weight: 700; letter-spacing: 1.2px;

	text-transform: uppercase; color: var(--cf-accent);

	margin-bottom: 14px;

}

.cf-slide-text h2 {

	font-size: clamp(24px, 3.5vw, 36px);

	font-weight: 700;

	letter-spacing: -.5px;

	line-height: 1.15;

	margin-bottom: 16px;

}

.cf-slide-text p {

	font-size: 16px;

	color: var(--cf-text-2);

	line-height: 1.75;

	max-width: 460px;

	margin-bottom: 28px;

}

.cf-slide-stats {

	display: flex; gap: 24px; margin-bottom: 36px;

}

.cf-slide-stat-val {

	font-size: 22px; font-weight: 700; color: var(--cf-accent);

	letter-spacing: -.5px; margin-bottom: 2px;

}

.cf-slide-stat-lbl {

	font-size: 11px; font-weight: 600; letter-spacing: .5px;

	text-transform: uppercase; color: var(--cf-text-3);

}



/* Dots */

.cf-slide-dots {

	display: flex; gap: 8px; align-items: center; margin-bottom: 16px;

}

.cf-slide-dot {

	width: 8px; height: 8px;

	border-radius: 50%;

	background: var(--cf-bg-raised);

	border: 1px solid var(--cf-border-2);

	cursor: pointer; padding: 0;

	transition: all .2s ease;

}

.cf-slide-dot.is-active {

	background: var(--cf-accent);

	border-color: var(--cf-accent);

	width: 24px;

	border-radius: 4px;

}



/* Progress bar */

.cf-slide-progress {

	height: 2px;

	background: var(--cf-border);

	border-radius: 2px;

	overflow: hidden;

	width: 200px;

}

.cf-slide-progress-bar {

	height: 100%;

	background: var(--cf-accent);

	width: 0%;

	transition: width linear;

}



/* ── Phone mockup ── */

.cf-phone-wrap {

	display: flex;

	flex-direction: column;

	align-items: center;

	position: relative;

}

.cf-phone-frame {

	width: 260px;

	background: #0e0e10;

	border-radius: 38px;

	border: 2px solid #2a2a2f;

	box-shadow:

		0 0 0 1px #111,

		0 32px 80px rgba(0,0,0,.8),

		inset 0 0 0 1px rgba(255,255,255,.04);

	position: relative;

	overflow: hidden;

	display: flex;

	flex-direction: column;

	align-items: center;

	padding: 16px 10px 18px;

	gap: 8px;

}

.cf-phone-notch {

	width: 80px; height: 22px;

	background: #0e0e10;

	border-radius: 0 0 16px 16px;

	border: 2px solid #2a2a2f;

	border-top: none;

	position: absolute;

	top: 0; left: 50%;

	transform: translateX(-50%);

	z-index: 10;

}

.cf-phone-screen {

	width: 100%;

	flex: 1;

	border-radius: 24px;

	overflow: hidden;

	background: #000;

	position: relative;

	min-height: 520px;

}

.cf-phone-img {

	position: absolute;

	inset: 0;

	width: 100%;

	height: 100%;

	object-fit: cover;

	object-position: top;

	opacity: 0;

	transition: opacity .55s ease;

}

.cf-phone-img.is-active { opacity: 1; }



.cf-phone-home {

	width: 80px; height: 4px;

	background: #2a2a2f;

	border-radius: 2px;

	margin-top: 2px;

}



/* Glow under phone */

.cf-phone-glow {

	position: absolute;

	bottom: -40px;

	width: 200px; height: 60px;

	background: radial-gradient(ellipse, rgba(88,166,255,.25) 0%, transparent 70%);

	filter: blur(16px);

	pointer-events: none;

}



/* Slide-in animation for phone on load */

@keyframes cf-phone-in {

	from { opacity: 0; transform: translateY(30px); }

	to   { opacity: 1; transform: translateY(0); }

}

.cf-phone-wrap { animation: cf-phone-in .7s ease both; }



/* ── Responsive ── */

@media (max-width: 900px) {

	.cf-slideshow-wrap {

		grid-template-columns: 1fr;

		gap: 48px;

	}

	.cf-phone-wrap { order: -1; }

	.cf-phone-frame { width: 220px; }

	.cf-phone-screen { min-height: 440px; }

	.cf-slide-text { position: relative; }

	.cf-slide-text-panel { min-height: auto; }

	.cf-diff-layout { grid-template-columns: 1fr; }

	.cf-diff-vs { display: none; }

	.cf-persona-grid--app { grid-template-columns: 1fr 1fr; }

}

@media (max-width: 480px) {

	.cf-phone-frame { width: 190px; }

	.cf-phone-screen { min-height: 380px; }

	.cf-persona-grid--app { grid-template-columns: 1fr; }

}



/* ── Differentiation section ── */

.cf-diff-layout {

	display: grid;

	grid-template-columns: 1fr auto 1fr;

	gap: 24px;

	align-items: start;

	margin-bottom: 32px;

}

.cf-diff-col {

	background: var(--cf-bg-2);

	border: 1px solid var(--cf-border);

	border-radius: var(--cf-r-xl);

	padding: 28px;

}

.cf-diff-col--bad  { border-color: rgba(248,81,73,.2);  background: rgba(248,81,73,.03); }

.cf-diff-col--good { border-color: rgba(63,185,80,.25); background: rgba(63,185,80,.04); }

.cf-diff-label {

	font-size: 12px;

	font-weight: 700;

	letter-spacing: .5px;

	margin-bottom: 18px;

	display: inline-block;

	padding: 5px 14px;

	border-radius: var(--cf-r-full);

}

.cf-diff-label--bad  { background: rgba(248,81,73,.12); color: #f85149; }

.cf-diff-label--good { background: rgba(63,185,80,.12);  color: var(--cf-success); }

.cf-diff-list {

	list-style: none;

	padding: 0; margin: 0;

	display: flex;

	flex-direction: column;

	gap: 10px;

}

.cf-diff-list li {

	font-size: 13px;

	color: var(--cf-text-2);

	padding-left: 20px;

	position: relative;

	line-height: 1.55;

}

.cf-diff-col--bad  .cf-diff-list li::before { content: '✗'; position:absolute; left:0; color:#f85149; font-weight:700; }

.cf-diff-col--good .cf-diff-list li::before { content: '✓'; position:absolute; left:0; color:var(--cf-success); font-weight:700; }

.cf-diff-vs {

	font-size: 14px;

	font-weight: 800;

	color: var(--cf-text-3);

	letter-spacing: 1px;

	display: flex;

	align-items: center;

	padding-top: 40px;

}

.cf-diff-callout {

	display: flex;

	gap: 16px;

	align-items: flex-start;

	background: rgba(88,166,255,.06);

	border: 1px solid rgba(88,166,255,.2);

	border-radius: var(--cf-r-lg);

	padding: 20px 24px;

}

.cf-diff-callout-icon { font-size: 28px; flex-shrink: 0; }

.cf-diff-callout strong {

	display: block;

	font-size: 15px;

	color: var(--cf-text);

	margin-bottom: 5px;

}

.cf-diff-callout span {

	font-size: 13px;

	color: var(--cf-text-2);

	line-height: 1.7;

}



/* ── App persona grid ── */

.cf-persona-grid--app {

	display: grid;

	grid-template-columns: repeat(3, 1fr);

	gap: 24px;

}

/* Persona card base (reuses cf-persona-card from studio page if loaded,

   but defined here for standalone use) */

.cf-persona-grid--app .cf-persona-card {

	background: var(--cf-bg-2);

	border: 1px solid var(--cf-border);

	border-radius: var(--cf-r-xl);

	padding: 26px;

	display: flex;

	flex-direction: column;

	gap: 14px;

	transition: border-color .2s;

}

.cf-persona-grid--app .cf-persona-card:hover { border-color: rgba(88,166,255,.3); }

.cf-persona-grid--app .cf-persona-header {

	display: flex; align-items: flex-start; gap: 14px;

}

.cf-persona-grid--app .cf-persona-icon {

	font-size: 26px; flex-shrink: 0;

	width: 50px; height: 50px;

	background: var(--cf-bg-3);

	border: 1px solid var(--cf-border);

	border-radius: var(--cf-r-lg);

	display: flex; align-items: center; justify-content: center;

}

.cf-persona-grid--app .cf-persona-role {

	font-size: 15px; font-weight: 700; color: var(--cf-text); line-height: 1.3;

}

.cf-persona-grid--app .cf-persona-sub {

	font-size: 11px; font-weight: 600; color: var(--cf-accent); margin-top: 3px;

}

.cf-persona-grid--app .cf-persona-card > p {

	font-size: 13px; color: var(--cf-text-2); line-height: 1.7;

}

.cf-persona-grid--app .cf-persona-workflow {

	list-style: none; padding: 0; margin: 0;

	display: flex; flex-direction: column; gap: 8px;

	border-top: 1px solid var(--cf-border); padding-top: 14px;

}

.cf-persona-grid--app .cf-persona-workflow li {

	font-size: 12px; color: var(--cf-text-2);

	padding-left: 18px; position: relative; line-height: 1.5;

}

.cf-persona-grid--app .cf-persona-workflow li::before {

	content: '→'; position: absolute; left: 0;

	color: var(--cf-accent); font-weight: 700;

}

</style>





<!-- ═══════════════════════════════════════════════════

     SLIDESHOW JS

══════════════════════════════════════════════════════ -->

<script>

(function () {

	var DURATION = 4500; // ms per slide

	var slides   = document.querySelectorAll('.cf-slide-text');

	var imgs     = document.querySelectorAll('.cf-phone-img');

	var dots     = document.querySelectorAll('.cf-slide-dot');

	var bar      = document.querySelector('.cf-slide-progress-bar');

	var current  = 0;

	var timer    = null;

	var barTimer = null;



	function goTo(n) {

		// Deactivate current

		slides[current].classList.remove('is-active');

		imgs[current].classList.remove('is-active');

		dots[current].classList.remove('is-active');



		current = (n + slides.length) % slides.length;



		// Activate next

		slides[current].classList.add('is-active');

		imgs[current].classList.add('is-active');

		dots[current].classList.add('is-active');



		// Reset progress bar

		if (bar) {

			bar.style.transition = 'none';

			bar.style.width = '0%';

			requestAnimationFrame(function () {

				requestAnimationFrame(function () {

					bar.style.transition = 'width ' + DURATION + 'ms linear';

					bar.style.width = '100%';

				});

			});

		}

	}



	function start() {

		timer = setInterval(function () { goTo(current + 1); }, DURATION);

	}

	function stop() { clearInterval(timer); }



	// Dot clicks

	dots.forEach(function (dot) {

		dot.addEventListener('click', function () {

			stop();

			goTo(parseInt(this.dataset.slide));

			start();

		});

	});



	// Pause on hover

	var section = document.querySelector('.cf-slideshow-section');

	if (section) {

		section.addEventListener('mouseenter', stop);

		section.addEventListener('mouseleave', start);

	}



	// Touch swipe on phone

	var phone = document.querySelector('.cf-phone-screen');

	if (phone) {

		var startX = 0;

		phone.addEventListener('touchstart', function (e) { startX = e.touches[0].clientX; }, { passive: true });

		phone.addEventListener('touchend', function (e) {

			var dx = e.changedTouches[0].clientX - startX;

			if (Math.abs(dx) > 40) {

				stop();

				goTo(current + (dx < 0 ? 1 : -1));

				start();

			}

		}, { passive: true });

	}



	// Kick off

	goTo(0);

	start();

})();

</script>

