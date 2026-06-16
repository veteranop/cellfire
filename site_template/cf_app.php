<?php
/**
 * Cellfire Template — Article Layout: Cellfire App
 * Assign this layout to a blank article, then set menu alias to "app"
 */
defined('_JEXEC') or die;
?>
<div class="cf-page-wrap">

	<!-- Hero -->
	<div class="cf-page-hero">
		<div class="cf-container">
			<div class="cf-split" style="gap:56px;align-items:center;">
				<div class="cf-split-text" style="max-width:520px;">
					<div class="cf-page-hero-label">Mobile · iOS &amp; Android</div>
					<h1>Cellfire <span class="cf-accent">App</span></h1>
					<p>Collect, analyze, and share cellular RF data directly from your mobile device. Real-time signal monitoring with GPS tagging and cloud sync — wherever the field takes you.</p>
					<div class="cf-page-hero-actions" style="margin-bottom:28px;">
						<a href="#download" class="cf-btn cf-btn--primary">Download the App</a>
						<a href="/pricing" class="cf-btn cf-btn--ghost">View Plans</a>
					</div>
					<!-- Inline trust signals -->
					<div style="display:flex;gap:20px;flex-wrap:wrap;">
						<div style="display:flex;align-items:center;gap:7px;font-size:12px;color:var(--cf-text-2);">
							<span style="color:var(--cf-signal);font-weight:700;">✓</span> LTE &amp; 5G
						</div>
						<div style="display:flex;align-items:center;gap:7px;font-size:12px;color:var(--cf-text-2);">
							<span style="color:var(--cf-signal);font-weight:700;">✓</span> GPS-tagged
						</div>
						<div style="display:flex;align-items:center;gap:7px;font-size:12px;color:var(--cf-text-2);">
							<span style="color:var(--cf-signal);font-weight:700;">✓</span> Cloud sync
						</div>
						<div style="display:flex;align-items:center;gap:7px;font-size:12px;color:var(--cf-text-2);">
							<span style="color:var(--cf-signal);font-weight:700;">✓</span> Battery optimized
						</div>
					</div>
				</div>

				<!-- Phone mockup -->
				<div class="cf-phone-wrap">
					<div class="cf-phone-frame">
						<div class="cf-phone-screen">
							<!-- Status bar -->
							<div class="cf-phone-statusbar">
								<span>9:41</span>
								<div style="display:flex;align-items:center;gap:6px;">
									<div class="cf-signal-bars">
										<span></span><span></span><span></span><span></span><span></span>
									</div>
									<span style="font-size:9px;color:var(--cf-signal);">5G</span>
								</div>
							</div>
							<!-- App header -->
							<div style="padding:4px 14px 10px;border-bottom:1px solid var(--cf-border);">
								<div style="font-size:13px;font-weight:700;color:var(--cf-accent);">Cellfire</div>
								<div style="font-size:9px;color:var(--cf-text-3);font-weight:600;letter-spacing:.5px;text-transform:uppercase;">Live Collection</div>
							</div>
							<!-- Content -->
							<div class="cf-phone-content">
								<!-- Map -->
								<div class="cf-phone-map">
									<div class="cf-phone-map-label">GPS Active</div>
								</div>
								<!-- Metrics -->
								<div class="cf-phone-metric">
									<div>
										<div class="cf-phone-metric-label">RSRP</div>
										<div class="cf-phone-metric-val cf-phone-metric-val--green">−78 dBm</div>
									</div>
									<div style="text-align:right;">
										<div class="cf-phone-metric-label">PCI</div>
										<div class="cf-phone-metric-val cf-phone-metric-val--cyan">247</div>
									</div>
								</div>
								<div class="cf-phone-metric">
									<div>
										<div class="cf-phone-metric-label">RSRQ</div>
										<div class="cf-phone-metric-val cf-phone-metric-val--cyan">−9 dB</div>
									</div>
									<div style="text-align:right;">
										<div class="cf-phone-metric-label">SINR</div>
										<div class="cf-phone-metric-val cf-phone-metric-val--green">18 dB</div>
									</div>
								</div>
								<div class="cf-phone-metric">
									<div>
										<div class="cf-phone-metric-label">Samples</div>
										<div class="cf-phone-metric-val" style="color:var(--cf-text);">1,284</div>
									</div>
									<div style="text-align:right;">
										<div class="cf-phone-metric-label">Band</div>
										<div class="cf-phone-metric-val cf-phone-metric-val--yellow">B66</div>
									</div>
								</div>
								<!-- Session button -->
								<div style="background:var(--cf-signal);border-radius:8px;padding:10px;text-align:center;font-size:11px;font-weight:700;color:#000;letter-spacing:.5px;box-shadow:0 0 16px rgba(0,230,118,.3);">
									● RECORDING
								</div>
							</div>
						</div>
					</div>
					<!-- Floating badge -->
					<div class="cf-phone-badge" style="top:30px;right:-24px;">
						<div class="cf-phone-badge-val">−78</div>
						<div class="cf-phone-badge-lbl">dBm RSRP</div>
					</div>
					<div class="cf-phone-badge" style="bottom:60px;left:-24px;animation-delay:1.5s;">
						<div class="cf-phone-badge-val" style="color:var(--cf-accent);">1,284</div>
						<div class="cf-phone-badge-lbl">Samples</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Features -->
	<div class="cf-page-section">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Features</div>
				<h2>Everything you need <span class="cf-accent">in your pocket</span></h2>
				<p>Built for RF professionals who work in the field every day.</p>
			</div>
			<div class="cf-icon-grid">
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📡</div>
					<h4>Real-Time Signal Monitoring</h4>
					<p>Live RSRP, RSRQ, SINR, and RSSI readings. Watch signal conditions change as you move through coverage areas.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📍</div>
					<h4>GPS-Tagged Data Collection</h4>
					<p>Every measurement is stamped with precise GPS coordinates. Build accurate drive test datasets with zero manual effort.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">☁️</div>
					<h4>Cloud Sync &amp; Sharing</h4>
					<p>Measurements sync instantly to your Cellfire account. Share datasets with team members or export to RF Studio for deep analysis.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📊</div>
					<h4>On-Device Visualization</h4>
					<p>View signal heat maps and coverage overlays directly on your phone. No laptop required for quick field assessments.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🔋</div>
					<h4>Battery-Optimized</h4>
					<p>Engineered for long field sessions. Adaptive polling rates keep data collection running all day without draining your battery.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📶</div>
					<h4>Multi-Carrier Support</h4>
					<p>Monitor LTE, 5G NSA, and 5G SA simultaneously. Switch between carriers and bands with a single tap.</p>
				</div>
			</div>
		</div>
	</div>

	<!-- How it works -->
	<div class="cf-page-section cf-page-section--alt">
		<div class="cf-container">
			<div class="cf-split">
				<div class="cf-split-text">
					<div class="cf-section-label">How It Works</div>
					<h2>From field to <span class="cf-accent">insight</span> in minutes</h2>
					<p>The Cellfire App streamlines the entire RF data collection workflow so you can spend less time processing and more time analyzing.</p>
					<div class="cf-steps">
						<div class="cf-step">
							<div class="cf-step-num">1</div>
							<div class="cf-step-body">
								<h4>Open &amp; Connect</h4>
								<p>Launch the app and it automatically begins pulling live signal data from your device's modem.</p>
							</div>
						</div>
						<div class="cf-step">
							<div class="cf-step-num">2</div>
							<div class="cf-step-body">
								<h4>Walk or Drive</h4>
								<p>GPS stamps every measurement as you move. Start a session and just drive — the app handles the rest.</p>
							</div>
						</div>
						<div class="cf-step">
							<div class="cf-step-num">3</div>
							<div class="cf-step-body">
								<h4>Review &amp; Export</h4>
								<p>When your session ends, review on-device maps or sync to the cloud. Export CSV or open in RF Studio for full analysis.</p>
							</div>
						</div>
					</div>
				</div>
				<!-- Second phone view — session summary -->
				<div class="cf-phone-wrap" style="justify-content:flex-end;">
					<div class="cf-phone-frame" style="width:240px;">
						<div class="cf-phone-screen" style="min-height:420px;">
							<div class="cf-phone-statusbar">
								<span>9:41</span>
								<div class="cf-signal-bars"><span></span><span></span><span></span><span></span><span></span></div>
							</div>
							<div style="padding:10px 14px 14px;display:flex;flex-direction:column;gap:10px;">
								<div style="font-size:12px;font-weight:700;color:var(--cf-text);padding-bottom:8px;border-bottom:1px solid var(--cf-border);">Session Summary</div>
								<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
									<div style="background:var(--cf-bg-2);border:1px solid var(--cf-border);border-radius:8px;padding:10px;text-align:center;">
										<div style="font-size:18px;font-weight:700;color:var(--cf-signal);">1,284</div>
										<div style="font-size:8px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-top:2px;">Samples</div>
									</div>
									<div style="background:var(--cf-bg-2);border:1px solid var(--cf-border);border-radius:8px;padding:10px;text-align:center;">
										<div style="font-size:18px;font-weight:700;color:var(--cf-accent);">12.3</div>
										<div style="font-size:8px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-top:2px;">km driven</div>
									</div>
									<div style="background:var(--cf-bg-2);border:1px solid var(--cf-border);border-radius:8px;padding:10px;text-align:center;">
										<div style="font-size:18px;font-weight:700;color:var(--cf-text);">−82</div>
										<div style="font-size:8px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-top:2px;">Avg RSRP</div>
									</div>
									<div style="background:var(--cf-bg-2);border:1px solid var(--cf-border);border-radius:8px;padding:10px;text-align:center;">
										<div style="font-size:18px;font-weight:700;color:var(--cf-warning);">4</div>
										<div style="font-size:8px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-top:2px;">PCIs seen</div>
									</div>
								</div>
								<div style="background:var(--cf-accent);border-radius:8px;padding:10px;text-align:center;font-size:11px;font-weight:700;color:#000;">
									Export CSV
								</div>
								<div style="background:var(--cf-bg-2);border:1px solid var(--cf-border-2);border-radius:8px;padding:10px;text-align:center;font-size:11px;font-weight:600;color:var(--cf-accent);">
									Open in RF Studio →
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Built Different -->
	<div class="cf-page-section cf-page-section--alt">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Built Different</div>
				<h2>Your phone knows more than your carrier<br>is <span class="cf-accent">willing to tell you.</span></h2>
				<p>Every Android device has a full RF modem reporting dozens of real-time metrics. Carriers hide it behind signal bars. Cellfire reads it directly.</p>
			</div>
			<div class="cf-diff-layout">
				<div class="cf-diff-col cf-diff-col--bad cf-animate">
					<div class="cf-diff-label cf-diff-label--bad">📵 Every Other App</div>
					<ul class="cf-diff-list">
						<li>Shows "bars" — a carrier-engineered number designed to look good, not be accurate</li>
						<li>Locks you to the serving cell only — the tower the carrier chose, not all towers in range</li>
						<li>Uploads your location and scan data to their servers — and sells it</li>
						<li>Gives you one signal metric when your modem is producing 20+</li>
						<li>Requires a carrier partnership or rooted device to see anything real</li>
						<li>Designed for casual users — useless for field work</li>
					</ul>
				</div>
				<div class="cf-diff-vs">VS</div>
				<div class="cf-diff-col cf-diff-col--good cf-animate">
					<div class="cf-diff-label cf-diff-label--good">✅ Cellfire</div>
					<ul class="cf-diff-list">
						<li>Reads directly from your modem: RSRP, RSRQ, SINR, CQI, ARFCN, PCI, TAC, timing advance, and more</li>
						<li>Shows the full neighbor cell list — every tower your phone can hear, simultaneously</li>
						<li>Nothing uploaded. Nothing sold. Your data stays on your device, period.</li>
						<li>Full metric panel per cell: frequency, band, power, quality — all of it, live</li>
						<li>No carrier involvement. No special permissions. Works on any unlocked Android.</li>
						<li>Built for RF professionals — by people who do RF work</li>
					</ul>
				</div>
			</div>
			<div class="cf-diff-footnote">
				What you see is what your modem is reporting — raw, unfiltered, and completely yours.
			</div>
		</div>
	</div>

	<!-- Why Pros Choose Cellfire -->
	<div class="cf-page-section">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Who Uses Cellfire</div>
				<h2>The tool serious RF people<br><span class="cf-accent">actually reach for.</span></h2>
				<p>From DAS installs to carrier disputes, Cellfire gives you the ground truth that spreadsheets and coverage maps can't.</p>
			</div>
			<div class="cf-persona-grid cf-persona-grid--app">

				<div class="cf-persona-card cf-animate">
					<div class="cf-persona-header">
						<div class="cf-persona-icon">🏗️</div>
						<div>
							<div class="cf-persona-role">DAS &amp; In-Building Engineers</div>
							<div class="cf-persona-sub">Pre-install surveys · Post-install verification · Coverage documentation</div>
						</div>
					</div>
					<p>Walk a building floor by floor and get GPS-stamped RSRP, SINR, and serving cell data at every waypoint — all carriers, all at once. Compare pre- and post-install measurements with data that holds up in a report.</p>
					<ul class="cf-persona-workflow">
						<li>Map existing signal penetration by floor, room, and carrier before touch-down</li>
						<li>See RSRP and SINR simultaneously — not just what bar you're at</li>
						<li>Export GPS-tagged measurements directly into RF Studio for comparison against your propagation model</li>
						<li>Generate install documentation backed by actual modem data, not carrier coverage maps</li>
					</ul>
				</div>

				<div class="cf-persona-card cf-animate">
					<div class="cf-persona-header">
						<div class="cf-persona-icon">🚗</div>
						<div>
							<div class="cf-persona-role">Drive Test &amp; Field Technicians</div>
							<div class="cf-persona-sub">Coverage surveys · Network audits · Handoff analysis</div>
						</div>
					</div>
					<p>Start a session, drive your route, and let Cellfire collect everything. GPS-tagged signal data at every second — no laptop, no external hardware, no setup. Just your phone doing the work of a dedicated drive test rig.</p>
					<ul class="cf-persona-workflow">
						<li>Continuous RSRP, RSRQ, SINR, and band logging along your entire route</li>
						<li>PCI and ARFCN tracking to identify tower handoffs and coverage gaps</li>
						<li>Export clean CSV data that feeds directly into your existing analysis tools</li>
						<li>Cover more ground with less gear — no more $15K drive test hardware for basic surveys</li>
					</ul>
				</div>

				<div class="cf-persona-card cf-animate">
					<div class="cf-persona-header">
						<div class="cf-persona-icon">📡</div>
						<div>
							<div class="cf-persona-role">Network &amp; Telecom Engineers</div>
							<div class="cf-persona-sub">Site audits · Interference hunting · Carrier benchmarking</div>
						</div>
					</div>
					<p>When you need to know what's actually happening at the RF layer — not what the carrier claims — Cellfire gives you the raw data. Neighbor cell lists, timing advance, CQI, and band info that carrier tools deliberately hide.</p>
					<ul class="cf-persona-workflow">
						<li>Full neighbor cell list: see every tower your device hears, not just the one it's attached to</li>
						<li>Band and ARFCN data to identify spectrum usage and carrier aggregation behavior</li>
						<li>Timing advance to estimate distance from the serving cell tower</li>
						<li>Side-by-side carrier comparison: run two devices, collect simultaneously, compare results</li>
					</ul>
				</div>

				<div class="cf-persona-card cf-animate">
					<div class="cf-persona-header">
						<div class="cf-persona-icon">💼</div>
						<div>
							<div class="cf-persona-role">Enterprise &amp; Facilities Managers</div>
							<div class="cf-persona-sub">Coverage accountability · SLA verification · Carrier disputes</div>
						</div>
					</div>
					<p>You're paying for enterprise wireless coverage. Cellfire tells you exactly what signal is available at every desk, conference room, and loading dock — with data you can put in front of a carrier and demand answers.</p>
					<ul class="cf-persona-workflow">
						<li>Document real RSRP and SINR levels across your facility — not carrier-provided maps</li>
						<li>Identify dead zones and weak spots before they become a support ticket</li>
						<li>Build a measured coverage baseline to hold carriers accountable to their SLAs</li>
						<li>Evidence-backed carrier disputes: this is what your signal actually is, here's the GPS proof</li>
					</ul>
				</div>

			</div>
		</div>
	</div>

	<!-- Download -->
	<div class="cf-page-section" id="download">
		<div class="cf-container" style="text-align:center;">
			<div class="cf-section-label">Download</div>
			<h2 style="font-size:clamp(28px,4vw,42px);font-weight:700;letter-spacing:-.5px;margin-bottom:14px;">
				Available on <span class="cf-accent">iOS &amp; Android</span>
			</h2>
			<p style="color:var(--cf-text-2);font-size:16px;margin-bottom:36px;max-width:440px;margin-left:auto;margin-right:auto;">
				Requires an active Cellfire subscription. Download free, sign in, and start collecting.
			</p>
			<div class="cf-store-badges" style="justify-content:center;margin-bottom:16px;">
				<a href="#" class="cf-store-badge" style="opacity:.45;cursor:not-allowed;" title="iOS — coming soon">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/></svg>
					App Store
				</a>
				<a href="/downloads" class="cf-store-badge">
					<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M3.18 23.76c.3.17.64.22.99.14l12.6-7.17-2.66-2.71-10.93 9.74zm-1.05-18.9c-.08.23-.13.49-.13.78v16.72c0 .29.05.55.13.78l.08.08 9.37-9.37v-.16L2.21 4.78l-.08.08zm19.61 8.77l-2.67-1.52-2.9 2.9 2.9 2.9 2.69-1.53c.76-.44.76-1.34-.02-1.75zm-17.95 9.35l10.61-10.61-2.66-2.66L3.13 5.06c-.1.1-.13.25-.05.37l.72.72z"/></svg>
					Android APK
				</a>
			</div>
			<p style="font-size:12px;color:var(--cf-text-3);">Android 10+ · iOS coming soon</p>
		</div>
	</div>

	<!-- CTA -->
	<div class="cf-cta-band">
		<div class="cf-container">
			<h2>Ready to take your RF work <span class="cf-accent">mobile?</span></h2>
			<p>Get full platform access with a Cellfire Professional subscription.</p>
			<div class="cf-hero-actions">
				<a href="/pricing" class="cf-btn cf-btn--primary">See Pricing</a>
				<a href="/about" class="cf-btn cf-btn--ghost">Learn More</a>
			</div>
		</div>
	</div>

	<?php if (!empty($this->item->text)) : ?>
	<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
	<?php endif; ?>

</div>
