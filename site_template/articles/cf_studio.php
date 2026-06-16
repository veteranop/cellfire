<?php
/**
 * Cellfire Template — Article Layout: RF Studio
 * Menu alias: "studio"
 */
defined('_JEXEC') or die;
?>
<div class="cf-page-wrap">

<!-- ═══════════════════════════════════════════════════
     HERO
══════════════════════════════════════════════════════ -->
<div class="cf-page-hero cf-studio-hero">
	<div class="cf-container">
		<div class="cf-studio-hero-inner">
			<div class="cf-studio-hero-text">
				<div class="cf-page-hero-label">Desktop Software · Windows &amp; macOS</div>
				<h1>Cellfire <span class="cf-accent">RF Studio</span></h1>
				<p>Professional RF propagation analysis and coverage planning software. FCC-compliant Longley-Rice modeling, terrain-aware diffraction, real-time interactive coverage maps, and publication-ready PDF reports — built for engineers who need accuracy, not approximations.</p>
				<div class="cf-page-hero-actions">
					<a href="/downloads" class="cf-btn cf-btn--primary">Download RF Studio</a>
					<a href="/pricing" class="cf-btn cf-btn--ghost">View Plans</a>
				</div>
				<!-- Tech badges -->
				<div class="cf-studio-badges">
					<span class="cf-studio-badge">ITM / Longley-Rice</span>
					<span class="cf-studio-badge">FCC Compliant</span>
					<span class="cf-studio-badge">SRTM Terrain</span>
					<span class="cf-studio-badge">3600-Azimuth Sampling</span>
					<span class="cf-studio-badge">Land Cover Analysis</span>
					<span class="cf-studio-badge cf-studio-badge--ai">🤖 Ollama AI Import</span>
					<span class="cf-studio-badge">Auto-Updating</span>
				</div>
			</div>

			<!-- Real coverage map screenshot -->
			<div class="cf-studio-hero-viz">
				<div class="cf-hero-plot-wrap">
					<img src="https://cellfire.io/images/Studio/Plot_20260130_072226.jpg"
					     alt="RF Studio terrain-aware coverage heatmap"
					     class="cf-hero-plot-img">
					<div class="cf-studio-readout cf-studio-readout--tl">
						<div class="cf-readout-label">Signal Strength</div>
						<div class="cf-readout-val">−58 → −124 dBm</div>
					</div>
					<div class="cf-studio-readout cf-studio-readout--br">
						<div class="cf-readout-label">Terrain</div>
						<div class="cf-readout-val">SRTM 30 m</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     PROPAGATION ENGINE
══════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">
		<div class="cf-split">
			<div class="cf-split-text">
				<div class="cf-section-label">Propagation Engine</div>
				<h2>FCC-grade modeling. <span class="cf-accent">Not shortcuts.</span></h2>
				<p>RF Studio uses the <strong>ITM (Longley-Rice)</strong> propagation model — the same algorithm accepted by the FCC for broadcast licensing. Unlike simplified circle-based tools, RF Studio accounts for terrain, diffraction, atmospheric refractivity, and frequency-specific propagation characteristics.</p>
				<p>Three critical algorithmic improvements eliminate common artifacts seen in competing tools:</p>
				<div class="cf-studio-algo-list">
					<div class="cf-algo-item">
						<div class="cf-algo-num">01</div>
						<div>
							<strong>Segment-by-segment diffraction</strong>
							<span>Prevents "shadow tunneling" — terrain obstructions are properly modeled at every interval along the path, not just at endpoints.</span>
						</div>
					</div>
					<div class="cf-algo-item">
						<div class="cf-algo-num">02</div>
						<div>
							<strong>3600-azimuth sampling at 0.1° resolution</strong>
							<span>Eliminates radial streak artifacts on coverage maps. Every degree of bearing is computed, not interpolated.</span>
						</div>
					</div>
					<div class="cf-algo-item">
						<div class="cf-algo-num">03</div>
						<div>
							<strong>360° Fresnel zone clearance analysis</strong>
							<span>First Fresnel zone is checked at every terrain point in every direction, giving you accurate line-of-sight analysis across the full coverage area.</span>
						</div>
					</div>
				</div>
			</div>
			<div class="cf-studio-spec-panel">
				<div class="cf-spec-header">Propagation Models</div>
				<div class="cf-spec-row">
					<span class="cf-spec-name">ITM / Longley-Rice</span>
					<span class="cf-spec-check">✓ FCC</span>
				</div>
				<div class="cf-spec-row">
					<span class="cf-spec-name">Free Space Path Loss</span>
					<span class="cf-spec-check">✓</span>
				</div>
				<div class="cf-spec-row">
					<span class="cf-spec-name">Two-Ray Ground Reflection</span>
					<span class="cf-spec-check">✓</span>
				</div>
				<div class="cf-spec-row">
					<span class="cf-spec-name">Terrain Diffraction</span>
					<span class="cf-spec-check">✓</span>
				</div>
				<div class="cf-spec-divider"></div>
				<div class="cf-spec-header">Technology Modes</div>
				<div class="cf-spec-row"><span class="cf-spec-name">FM Broadcast</span><span class="cf-spec-check">✓</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">AM Broadcast</span><span class="cf-spec-check">✓</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">TV / DTV</span><span class="cf-spec-check">✓</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">LTE / Cellular</span><span class="cf-spec-check">✓</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">LoRa / IoT</span><span class="cf-spec-check">✓</span></div>
				<div class="cf-spec-divider"></div>
				<div class="cf-spec-header">Terrain Data</div>
				<div class="cf-spec-row"><span class="cf-spec-name">SRTM (30m resolution)</span><span class="cf-spec-check">✓ AWS</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">Elevation tile caching</span><span class="cf-spec-check">✓ Offline</span></div>
				<div class="cf-spec-row"><span class="cf-spec-name">HAAT calculation</span><span class="cf-spec-check">✓ FCC</span></div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     LIVE COVERAGE MAP
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-split cf-split--flip">
			<div class="cf-split-text">
				<div class="cf-section-label">Interactive Coverage Map</div>
				<h2>Drag, adjust, and see coverage <span class="cf-accent">update live.</span></h2>
				<p>Place your transmitter anywhere on the map. Adjust power, antenna height, or frequency — and the coverage heatmap recalculates in real time. Hover anywhere on the map to probe signal strength at that exact location.</p>
				<ul class="cf-feature-list" style="margin-top:0;">
					<li>Color-gradient heatmap (Blue → Cyan → Green → Yellow → Red)</li>
					<li>Adjustable coverage transparency and threshold</li>
					<li>Live mouse-over signal strength probing (dBm &amp; dBu)</li>
					<li>Shadow zone visualization — terrain obstructions shown clearly</li>
					<li>Multiple basemaps: Esri WorldImagery, OpenStreetMap, satellite</li>
					<li>Terrain quality modes: Low / Medium / High / Ultra</li>
					<li>Zoom-aware resolution scaling for speed vs. detail tradeoff</li>
				</ul>
			</div>
			<div class="cf-split-visual" style="flex-direction:column;gap:0;padding:0;overflow:hidden;">
				<div style="background:var(--cf-bg-3);padding:10px 14px;border-bottom:1px solid var(--cf-border);display:flex;align-items:center;gap:8px;">
					<div style="width:10px;height:10px;border-radius:50%;background:#f85149;"></div>
					<div style="width:10px;height:10px;border-radius:50%;background:#e3b341;"></div>
					<div style="width:10px;height:10px;border-radius:50%;background:#3fb950;"></div>
					<span style="font-size:12px;color:var(--cf-text-3);margin-left:6px;">Cellfire RF Studio — Coverage Map</span>
				</div>
				<img src="https://cellfire.io/images/Studio/Screenshot%202026-03-01%20200640.png"
				     alt="Cellfire RF Studio application showing live coverage map"
				     style="width:100%;display:block;object-fit:cover;">
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     TERRAIN & PATH ANALYSIS
══════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">
		<div class="cf-split">
			<div class="cf-split-text">
				<div class="cf-section-label">Terrain &amp; Path Analysis</div>
				<h2>See exactly what the terrain <span class="cf-accent">does to your signal.</span></h2>
				<p>Click any point on the map and RF Studio renders a full elevation profile between your transmitter and that point. Fresnel zone clearance, line-of-sight, and terrain obstruction data are calculated and displayed instantly.</p>
				<ul class="cf-feature-list" style="margin-top:0;">
					<li>Elevation profile graph with Fresnel zone visualization</li>
					<li>Line-of-sight clearance analysis with obstruction detection</li>
					<li>Path loss breakdown: free space + terrain diffraction loss (dB)</li>
					<li>HAAT (Height Above Average Terrain) per FCC rules</li>
					<li>Automatically downloaded SRTM 30m terrain tiles</li>
					<li>Offline tile caching — no internet needed after first download</li>
					<li>Land cover classification (urban / suburban / rural) for terrain-type-aware propagation modeling</li>
				</ul>
			</div>
			<div class="cf-split-visual">
				<!-- Simulated elevation profile -->
				<div style="width:100%;padding:16px;">
					<div style="font-size:11px;color:var(--cf-text-3);margin-bottom:8px;text-transform:uppercase;letter-spacing:.5px;">Elevation Profile — TX to probe point</div>
					<svg viewBox="0 0 300 120" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:100%;height:auto;">
						<!-- Ground -->
						<path d="M0 90 L30 85 L60 70 L80 72 L100 60 L120 78 L150 80 L170 65 L190 75 L220 85 L260 88 L300 90 L300 120 L0 120 Z" fill="rgba(0,200,255,0.08)" stroke="rgba(0,200,255,0.4)" stroke-width="1.5"/>
						<!-- Fresnel zone -->
						<path d="M0 45 Q150 20 300 50" stroke="rgba(0,200,255,0.25)" stroke-width="8" stroke-linecap="round"/>
						<!-- LOS line -->
						<line x1="0" y1="60" x2="300" y2="70" stroke="#58a6ff" stroke-width="1.5" stroke-dasharray="6,3" opacity=".7"/>
						<!-- TX marker -->
						<circle cx="0" cy="60" r="4" fill="#58a6ff"/>
						<!-- Labels -->
						<text x="4" y="56" fill="#58a6ff" font-size="8">TX</text>
						<text x="4" y="100" fill="#484f58" font-size="7">0 km</text>
						<text x="270" y="100" fill="#484f58" font-size="7">42 km</text>
						<text x="240" y="56" fill="#8b949e" font-size="7">−82 dBm</text>
					</svg>
				</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     RF CHAIN BUILDER
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-section-head">
			<div class="cf-section-label">Station Builder</div>
			<h2>Build your complete <span class="cf-accent">RF chain.</span></h2>
			<p>Model your entire transmitter system — from transmitter output through every cable, connector, filter, and antenna — with real manufacturer specifications.</p>
		</div>

		<div class="cf-rf-chain-demo" style="padding:0;overflow:hidden;">
			<div style="background:var(--cf-bg-3);padding:10px 14px;border-bottom:1px solid var(--cf-border);display:flex;align-items:center;gap:8px;">
				<div style="width:10px;height:10px;border-radius:50%;background:#f85149;"></div>
				<div style="width:10px;height:10px;border-radius:50%;background:#e3b341;"></div>
				<div style="width:10px;height:10px;border-radius:50%;background:#3fb950;"></div>
				<span style="font-size:12px;color:var(--cf-text-3);margin-left:6px;">RF Studio — Station Builder · RF Chain</span>
			</div>
			<img src="https://cellfire.io/images/Studio/Screenshot%202026-03-01%20200948.png"
			     alt="RF Studio Station tab showing Flexiva FAX250 → LDF4-50A cable → Combiner → LDF4-50A → DS7A12P90U-N antenna chain with system totals"
			     style="width:100%;display:block;">
			<div style="background:var(--cf-bg-3);padding:10px 16px;border-top:1px solid var(--cf-border);font-size:12px;color:var(--cf-text-2);">
				Flexiva FAX250 → Andrew LDF4-50A cable → Starpoint Combiner → LDF4-50A → DS7A12P90U-N antenna &nbsp;·&nbsp; <span style="color:var(--cf-success);font-weight:700;">Net: +9.94 dB</span>
			</div>
		</div>

		<div class="cf-icon-grid cf-icon-grid--4" style="margin-top:40px;">
			<div class="cf-icon-feature cf-animate" style="grid-column:span 2;">
				<div class="cf-icon-feature-icon">🔧</div>
				<h4>Real Manufacturer Catalogs</h4>
				<p>Pre-loaded with actual specifications from the equipment you already use in the field — browse by manufacturer and click to add directly to your RF chain.</p>
				<div class="cf-mfr-grid" style="margin-bottom:16px;">
					<span>Andrew / CommScope</span>
					<span>Bird Technologies</span>
					<span>BW Broadcast</span>
					<span>Harris Broadcast</span>
					<span>L3 Harris</span>
					<span>JAMPRO</span>
					<span>Shively Labs</span>
					<span>Decibel Products</span>
					<span>Times Microwave</span>
					<span>Polyphaser</span>
					<span>Nautel</span>
					<span>Yaesu</span>
					<span>Meshtastic</span>
					<span>RELL</span>
				</div>
				<div class="cf-screenshot-frame">
					<img src="https://cellfire.io/images/Studio/Screenshot%202026-03-01%20200732.png"
					     alt="Browse Transmitters dialog showing manufacturer tree with L3 Harris Master-V selected"
					     style="width:100%;border-radius:var(--cf-r-md);display:block;">
				</div>
			</div>
			<div class="cf-icon-feature cf-animate">
				<div class="cf-icon-feature-icon">📐</div>
				<h4>Full Antenna Patterns</h4>
				<p>Import complete azimuth and elevation antenna patterns from XML files. Gain is interpolated for every bearing and mechanical downtilt angle — not just boresight.</p>
				<div style="margin-top:16px;padding:14px;background:var(--cf-bg-raised);border:1px solid var(--cf-border);border-radius:var(--cf-r-md);">
					<div style="font-size:11px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-bottom:8px;">Supported pattern formats</div>
					<div style="font-size:13px;color:var(--cf-text-2);line-height:2;">XML · MSI / Planet · .ANT · Manual entry</div>
				</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     AI SMART IMPORT  (Ollama)
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-ai-import-section">
	<div class="cf-container">

		<div class="cf-ai-import-header">
			<div class="cf-ai-badge-row">
				<span class="cf-ai-label-pill">🤖 Industry First</span>
			</div>
			<div class="cf-section-label">AI-Powered Component Import</div>
			<h2>Drop in <em>any</em> datasheet.<br><span class="cf-accent">AI does the rest.</span></h2>
			<p class="cf-ai-lead">No other RF planning software does this. RF Studio runs a local <strong>Ollama</strong> AI model that reads any manufacturer PDF — spec sheets, datasheets, install guides — and automatically extracts and creates the component file. No formatting required. No waiting on an engineer to e-mail you the right file. No hunting through databases hoping your exact model is listed.</p>
		</div>

		<div class="cf-ai-import-layout">
			<!-- Left: feature detail -->
			<div class="cf-ai-import-text">
				<div class="cf-ai-flow">
					<div class="cf-ai-step">
						<div class="cf-ai-step-num">1</div>
						<div class="cf-ai-step-body">
							<strong>Drop a PDF, paste a URL, or search a model number</strong>
							<span>Any manufacturer datasheet — doesn't matter if it's a 200-page catalog or a single-page spec sheet. If the data is in there, the AI finds it.</span>
						</div>
					</div>
					<div class="cf-ai-step">
						<div class="cf-ai-step-num">2</div>
						<div class="cf-ai-step-body">
							<strong>Ollama AI reads and classifies the document</strong>
							<span>The local AI model determines whether it's an antenna, cable, transmitter, combiner, or amplifier — then extracts frequency range, power ratings, gain, loss, impedance, and every other relevant parameter.</span>
						</div>
					</div>
					<div class="cf-ai-step">
						<div class="cf-ai-step-num">3</div>
						<div class="cf-ai-step-body">
							<strong>Component is created and added to your catalog</strong>
							<span>A properly formatted component file is generated instantly and appears in your manufacturer library — ready to drop into any RF chain.</span>
						</div>
					</div>
				</div>

				<div class="cf-ai-callout">
					<div class="cf-ai-callout-icon">🔒</div>
					<div>
						<strong>100% local — nothing leaves your machine</strong>
						<span>Ollama runs entirely on your local hardware. Your proprietary equipment configurations, client specs, and unpublished component data never touch a cloud server.</span>
					</div>
				</div>

				<div class="cf-ai-component-types">
					<div class="cf-ai-comp-type">📡 Antennas</div>
					<div class="cf-ai-comp-type">〰 Coax Cables</div>
					<div class="cf-ai-comp-type">📻 Transmitters</div>
					<div class="cf-ai-comp-type">⚡ Amplifiers</div>
					<div class="cf-ai-comp-type">⬡ Combiners</div>
					<div class="cf-ai-comp-type">⊕ Connectors</div>
				</div>
			</div>

			<!-- Right: screenshot -->
			<div class="cf-ai-import-visual">
				<div class="cf-screenshot-frame">
					<div class="cf-win-chrome">
						<div class="cf-win-dot cf-win-dot--r"></div>
						<div class="cf-win-dot cf-win-dot--y"></div>
						<div class="cf-win-dot cf-win-dot--g"></div>
						<span>Smart Import — PDF or URL</span>
					</div>
					<img src="https://cellfire.io/images/Studio/Screenshot%202026-03-01%20200934.png"
					     alt="RF Studio Smart Import dialog powered by Ollama AI — drop any PDF datasheet to auto-extract component specs"
					     style="width:100%;display:block;">
				</div>
				<div class="cf-ai-import-caption">
					Powered by <strong>Ollama</strong> — running locally on your machine. No API key, no subscription, no data sharing.
				</div>
			</div>
		</div>

	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     FCC INTEGRATION
══════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">
		<div class="cf-split">
			<div class="cf-split-text">
				<div class="cf-section-label">FCC Database Integration</div>
				<h2>Real-time FCC lookups. <span class="cf-accent">Inside the app.</span></h2>
				<p>RF Studio queries the FCC broadcast database directly. Search by coordinates and frequency to pull existing station data, find co-channel and adjacent-channel stations, and import facility information into your project without leaving the app.</p>
				<ul class="cf-feature-list" style="margin-top:0;">
					<li>Search FCC database by coordinates + frequency</li>
					<li>Automatic service type detection (FM, AM, TV, LoRa, LTE)</li>
					<li>Retrieve existing station facility data and callsigns</li>
					<li>View co-channel and adjacent-channel competition</li>
					<li>FCC filing data flows directly into PDF reports</li>
					<li>HAAT &amp; field strength compliance (dBu/dBμV/m)</li>
				</ul>
			</div>
			<div class="cf-split-visual" style="flex-direction:column;gap:0;padding:0;overflow:hidden;">
				<div style="background:var(--cf-bg-3);padding:10px 14px;border-bottom:1px solid var(--cf-border);font-size:12px;color:var(--cf-text-3);">
					FCC Station Query
				</div>
				<div style="flex:1;padding:16px;display:flex;flex-direction:column;gap:10px;min-height:200px;">
					<div class="cf-fcc-row" style="--i:0;">
						<span class="cf-fcc-call">KUER-FM</span>
						<span class="cf-fcc-freq">90.1 MHz</span>
						<span class="cf-fcc-erp">100 kW</span>
						<span class="cf-fcc-type">FM</span>
					</div>
					<div class="cf-fcc-row" style="--i:1;">
						<span class="cf-fcc-call">KBYU-FM</span>
						<span class="cf-fcc-freq">89.1 MHz</span>
						<span class="cf-fcc-erp">72 kW</span>
						<span class="cf-fcc-type">FM</span>
					</div>
					<div class="cf-fcc-row" style="--i:2;">
						<span class="cf-fcc-call">KCPX-FM</span>
						<span class="cf-fcc-freq">90.9 MHz</span>
						<span class="cf-fcc-erp">38 kW</span>
						<span class="cf-fcc-type">FM</span>
					</div>
					<div class="cf-fcc-row" style="--i:3;opacity:.5;">
						<span class="cf-fcc-call">+ 4 more...</span>
						<span></span><span></span><span></span>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     EXPORT & REPORTING
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-section-head">
			<div class="cf-section-label">Export &amp; Reporting</div>
			<h2>From analysis to <span class="cf-accent">deliverable.</span></h2>
			<p>Generate professional reports and export data in the formats your clients, regulators, and teammates need.</p>
		</div>
		<!-- Real exported coverage plot -->
		<div class="cf-screenshot-frame" style="margin-bottom:32px;max-width:860px;margin-left:auto;margin-right:auto;">
			<div style="background:var(--cf-bg-3);padding:10px 14px;border-bottom:1px solid var(--cf-border);display:flex;align-items:center;gap:8px;border-radius:var(--cf-r-xl) var(--cf-r-xl) 0 0;">
				<div style="width:10px;height:10px;border-radius:50%;background:#f85149;"></div>
				<div style="width:10px;height:10px;border-radius:50%;background:#e3b341;"></div>
				<div style="width:10px;height:10px;border-radius:50%;background:#3fb950;"></div>
				<span style="font-size:12px;color:var(--cf-text-3);margin-left:6px;">Exported Coverage Plot — JPEG · Terrain-aware ITM propagation</span>
			</div>
			<img src="https://cellfire.io/images/Studio/Plot_20260202_082111.jpg"
			     alt="Real exported RF Studio coverage map showing terrain-aware signal propagation across mountainous terrain"
			     style="width:100%;display:block;border-radius:0 0 var(--cf-r-xl) var(--cf-r-xl);">
		</div>

		<div class="cf-dl-grid">
			<div class="cf-dl-card cf-animate">
				<div class="cf-dl-card-icon" style="font-size:24px;width:48px;height:48px;background:var(--cf-accent-dim);border-radius:var(--cf-r-md);display:flex;align-items:center;justify-content:center;">📄</div>
				<h3>PDF Reports</h3>
				<p>Publication-ready technical reports with embedded multi-zoom coverage maps, RF chain specifications, FCC filing data, terrain profiles, and coverage statistics. Ready for regulatory submission.</p>
				<div class="cf-dl-card-meta">Includes: Station info · RF chain · Antenna specs · FCC data · Coverage maps at zoom 9–13</div>
			</div>
			<div class="cf-dl-card cf-animate">
				<div class="cf-dl-card-icon" style="font-size:24px;width:48px;height:48px;background:var(--cf-accent-dim);border-radius:var(--cf-r-md);display:flex;align-items:center;justify-content:center;">🌍</div>
				<h3>KML / Google Earth</h3>
				<p>Export coverage contours and transmitter placemarks as KML files. Drop directly into Google Earth, ArcGIS, or any GIS tool for further spatial analysis or client presentations.</p>
				<div class="cf-dl-card-meta">Compatible with Google Earth · QGIS · ArcGIS · GeoJSON workflows</div>
			</div>
			<div class="cf-dl-card cf-animate">
				<div class="cf-dl-card-icon" style="font-size:24px;width:48px;height:48px;background:var(--cf-accent-dim);border-radius:var(--cf-r-md);display:flex;align-items:center;justify-content:center;">🖼️</div>
				<h3>Coverage Map Images</h3>
				<p>Export high-resolution coverage map images at multiple zoom levels (9–13) as JPEG files. Terrain-aware heatmaps overlaid on satellite imagery — ready for reports or client presentations.</p>
				<div class="cf-dl-card-meta">Multi-zoom export · High-res JPEG · Satellite basemap included</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     TECHNOLOGIES
══════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">
		<div class="cf-section-head">
			<div class="cf-section-label">Supported Technologies</div>
			<h2>One tool. Every <span class="cf-accent">radio technology.</span></h2>
			<p>RF Studio speaks the same frequency language you do — whether you're planning a broadcast transmitter, a P25 public safety network, a Meshtastic mesh, or a HAM repeater site.</p>
		</div>
		<div class="cf-tech-grid">

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">📻</span>
					<div class="cf-tech-band">87.5 – 108 MHz</div>
				</div>
				<h4>FM Broadcast</h4>
				<p>FCC-compliant contour planning, HAAT calculation, co-channel and adjacent-channel analysis. Generate full technical showings ready for filing.</p>
				<div class="cf-tech-tags"><span>Class A/B/C</span><span>HAAT</span><span>FCC Part 73</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">📡</span>
					<div class="cf-tech-band">520 kHz – 1.7 MHz</div>
				</div>
				<h4>AM Broadcast</h4>
				<p>Ground-wave and sky-wave propagation modeling for AM stations. Interference analysis for day/night patterns and directional antenna arrays.</p>
				<div class="cf-tech-tags"><span>Ground Wave</span><span>Night Pattern</span><span>FCC Part 73</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">📺</span>
					<div class="cf-tech-band">54 – 806 MHz</div>
				</div>
				<h4>TV / DTV</h4>
				<p>Digital television coverage planning across VHF low, VHF high, and UHF bands. F(50,90) contour analysis and interference calculations per FCC rules.</p>
				<div class="cf-tech-tags"><span>ATSC</span><span>F(50,90)</span><span>FCC Part 73</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">🚔</span>
					<div class="cf-tech-band">136 – 870 MHz</div>
				</div>
				<h4>P25 / Public Safety</h4>
				<p>Plan P25 trunked and conventional radio coverage for first responder networks. Validate repeater site placement, portable-to-portable coverage, and in-building signal penetration estimates.</p>
				<div class="cf-tech-tags"><span>APCO P25</span><span>Phase 1 &amp; 2</span><span>FirstNet</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">🔭</span>
					<div class="cf-tech-band">VHF / UHF / HF</div>
				</div>
				<h4>Amateur Radio</h4>
				<p>Site-plan repeaters, link systems, and simplex coverage areas across any amateur band. HAAT analysis for license applications. Path analysis for linked repeater systems and EmComm nets.</p>
				<div class="cf-tech-tags"><span>Repeater Sites</span><span>Link Analysis</span><span>EmComm</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">🌐</span>
					<div class="cf-tech-band">433 / 868 / 915 MHz</div>
				</div>
				<h4>Meshtastic / LoRa</h4>
				<p>Model LoRa propagation for Meshtastic mesh networks and LoRaWAN gateway placement. Terrain-aware path analysis to predict node-to-node link viability before you deploy hardware.</p>
				<div class="cf-tech-tags"><span>LoRa</span><span>Meshtastic</span><span>LoRaWAN</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">📶</span>
					<div class="cf-tech-band">700 MHz – 3.7 GHz</div>
				</div>
				<h4>LTE / Cellular</h4>
				<p>Site selection and coverage modeling for private LTE, CBRS, and macro cellular deployments. Identify dead zones, predict handoff boundaries, and validate link budgets before tower construction.</p>
				<div class="cf-tech-tags"><span>CBRS</span><span>Private LTE</span><span>Band-aware</span></div>
			</div>

			<div class="cf-tech-card cf-animate">
				<div class="cf-tech-card-header">
					<span class="cf-tech-icon">🚨</span>
					<div class="cf-tech-band">Various UHF/VHF</div>
				</div>
				<h4>Emergency Communications</h4>
				<p>Verify coverage for critical infrastructure before deployment. Terrain-aware analysis shows exactly where dead zones will be — so responders aren't the ones who find out the hard way.</p>
				<div class="cf-tech-tags"><span>EMCOMM</span><span>ARES/RACES</span><span>FEMA</span></div>
			</div>

		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     WHO IT'S FOR
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-section-head">
			<div class="cf-section-label">Who Uses RF Studio</div>
			<h2>Built for every kind of <span class="cf-accent">RF professional</span></h2>
		</div>
		<div class="cf-persona-grid">

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">📻</div>
					<div>
						<div class="cf-persona-role">Broadcast Engineering</div>
						<div class="cf-persona-sub">FM · AM · TV · DTV</div>
					</div>
				</div>
				<p>You're applying for a construction permit, modifying an existing license, or running an interference study. RF Studio gives you ITM-modeled contours, FCC HAAT calculations, and PDF technical showings — all in one workflow.</p>
				<ul class="cf-persona-workflow">
					<li>Pull co-channel and adjacent-channel station data directly from the FCC database</li>
					<li>Build your complete RF chain — transmitter, combiners, transmission line, antenna — with real manufacturer specs</li>
					<li>Generate coverage contours at every zoom level and export to PDF, ready for FCC filing</li>
					<li>Export KML for client presentations in Google Earth</li>
				</ul>
			</div>

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">🔭</div>
					<div>
						<div class="cf-persona-role">HAM License Holders &amp; Site Admins</div>
						<div class="cf-persona-sub">Repeaters · Links · EmComm</div>
					</div>
				</div>
				<p>You're picking a repeater site, planning a linked system, or verifying coverage for your ARES net. RF Studio lets you model real terrain between sites — not circles on a map — and back it up with documented analysis.</p>
				<ul class="cf-persona-workflow">
					<li>Model repeater coverage across realistic terrain with ITM — see actual shadow zones</li>
					<li>Run path analysis between linked sites to verify link viability before climbing a tower</li>
					<li>Calculate HAAT per FCC rules for license applications</li>
					<li>Document your system RF chain for club records and trustee filings</li>
				</ul>
			</div>

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">🌐</div>
					<div>
						<div class="cf-persona-role">Meshtastic Deployers</div>
						<div class="cf-persona-sub">LoRa · 915 MHz · Mesh Networks</div>
					</div>
				</div>
				<p>You're standing up a community Meshtastic mesh and want to know if your gateway on the water tower actually reaches the valley. RF Studio tells you — with terrain elevation, Fresnel zone analysis, and the actual signal levels to expect.</p>
				<ul class="cf-persona-workflow">
					<li>Model 915 MHz / 433 MHz LoRa propagation with terrain-aware ITM</li>
					<li>Run node-to-node path analysis with Fresnel zone clearance checks</li>
					<li>Optimize gateway placement before you haul hardware up a mountain</li>
					<li>Generate coverage maps to share with the community and coordinate mesh expansion</li>
				</ul>
			</div>

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">🏢</div>
					<div>
						<div class="cf-persona-role">Radio System Administrators</div>
						<div class="cf-persona-sub">P25 · Trunked · Land Mobile</div>
					</div>
				</div>
				<p>You're managing a P25 trunked system, a multi-site conventional network, or a critical LMR deployment. RF Studio documents your infrastructure, models coverage, and gives you the analysis to justify system changes to decision-makers.</p>
				<ul class="cf-persona-workflow">
					<li>Model multi-site coverage and identify gaps in portable-to-portable coverage</li>
					<li>Document the RF chain of every site in your system with real specs</li>
					<li>Run interference studies when new sites are proposed near your infrastructure</li>
					<li>Generate PDF site documentation for your operations center</li>
				</ul>
			</div>

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">💼</div>
					<div>
						<div class="cf-persona-role">System Owners</div>
						<div class="cf-persona-sub">Verify · Document · Plan Expansion</div>
					</div>
				</div>
				<p>You paid for coverage. Now verify it. RF Studio lets you model what your system should be producing — and compare it against what you're actually getting in the field. Use it to hold vendors accountable and plan intelligent expansion.</p>
				<ul class="cf-persona-workflow">
					<li>Model your existing system's designed coverage footprint</li>
					<li>Compare design models against Cellfire App drive test measurements</li>
					<li>Identify whether coverage gaps are RF issues or equipment failures</li>
					<li>Build the technical case for new infrastructure with documentation</li>
				</ul>
			</div>

			<div class="cf-persona-card cf-animate">
				<div class="cf-persona-header">
					<div class="cf-persona-icon">🔬</div>
					<div>
						<div class="cf-persona-role">QA &amp; Field Verification</div>
						<div class="cf-persona-sub">Test · Measure · Report</div>
					</div>
				</div>
				<p>You're verifying that a newly deployed system performs to spec. RF Studio is your baseline — the model your field measurements should match. When they don't, you have the analysis to pinpoint exactly where the system falls short.</p>
				<ul class="cf-persona-workflow">
					<li>Generate the predicted coverage model as the QA benchmark</li>
					<li>Pair with Cellfire App for GPS-stamped field measurements</li>
					<li>Compare predicted vs. measured signal levels at each test point</li>
					<li>Deliver a complete technical report — model, measurements, and delta — to the client</li>
				</ul>
			</div>

		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     SYSTEM REQUIREMENTS
══════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container" style="max-width:780px;margin:0 auto;">
		<div class="cf-section-head">
			<div class="cf-section-label">Requirements</div>
			<h2>System <span class="cf-accent">Requirements</span></h2>
		</div>
		<p style="text-align:center;color:var(--cf-text-2);margin-bottom:28px;font-size:14px;">RF Studio includes a built-in auto-updater — it checks for new releases at launch and installs them silently in the background.</p>
		<div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;">
			<div class="cf-account-card">
				<h3>Windows</h3>
				<table class="cf-req-table">
					<tr><td>OS</td><td>Windows 10 / 11 (64-bit)</td></tr>
					<tr><td>RAM</td><td>8 GB minimum, 16 GB recommended</td></tr>
					<tr><td>Storage</td><td>2 GB + terrain tile cache</td></tr>
					<tr><td>Internet</td><td>Required for FCC lookups &amp; initial terrain download</td></tr>
				</table>
			</div>
			<div class="cf-account-card">
				<h3>macOS</h3>
				<table class="cf-req-table">
					<tr><td>OS</td><td>macOS 12 Monterey or later</td></tr>
					<tr><td>RAM</td><td>8 GB minimum, 16 GB recommended</td></tr>
					<tr><td>Storage</td><td>2 GB + terrain tile cache</td></tr>
					<tr><td>Chip</td><td>Intel x64 or Apple Silicon (M1/M2/M3)</td></tr>
				</table>
			</div>
		</div>
	</div>
</div>

<!-- ═══════════════════════════════════════════════════
     CTA
══════════════════════════════════════════════════════ -->
<div class="cf-cta-band">
	<div class="cf-container">
		<h2>Stop guessing. Start <span class="cf-accent">modeling.</span></h2>
		<p>RF Studio is included in Professional and Enterprise plans. Download and start your first coverage map today.</p>
		<div class="cf-hero-actions">
			<a href="/downloads" class="cf-btn cf-btn--primary">Download RF Studio</a>
			<a href="/pricing" class="cf-btn cf-btn--ghost">See Pricing</a>
			<a href="/viewer" class="cf-btn cf-btn--ghost">Try Browser Tool First →</a>
		</div>
	</div>
</div>

<?php if (!empty($this->item->text)) : ?>
<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
<?php endif; ?>

</div><!-- /.cf-page-wrap -->


<!-- Studio-specific CSS -->
<style>
/* Hero */
.cf-studio-hero-inner {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 64px;
	align-items: center;
}
/* Hero real plot image */
.cf-hero-plot-wrap {
	position: relative;
	border-radius: var(--cf-r-xl);
	overflow: hidden;
	border: 1px solid var(--cf-border-2);
	box-shadow: 0 24px 64px rgba(0,0,0,.6), 0 0 0 1px rgba(0,200,255,.15), 0 0 48px rgba(0,200,255,.06);
}
.cf-hero-plot-img {
	width: 100%;
	display: block;
	border-radius: var(--cf-r-xl);
}
/* Screenshot frame */
.cf-screenshot-frame {
	border-radius: var(--cf-r-xl);
	overflow: hidden;
	border: 1px solid var(--cf-border);
	box-shadow: 0 8px 32px rgba(0,0,0,.4);
}
/* 4-col icon grid */
.cf-icon-grid--4 {
	display: grid;
	grid-template-columns: 1fr 1fr 1fr 1fr;
	gap: 24px;
}
.cf-icon-grid--4 .cf-icon-feature[style*="grid-column:span 2"] {
	grid-column: span 2;
}
.cf-studio-badges {
	display: flex; gap: 8px; flex-wrap: wrap; margin-top: 20px;
}
.cf-studio-badge {
	font-size: 11px; font-weight: 600; letter-spacing: .4px;
	color: var(--cf-accent);
	background: var(--cf-accent-dim);
	border: 1px solid rgba(0,200,255,.22);
	border-radius: var(--cf-r-full);
	padding: 3px 10px;
}

/* Coverage map viz */
.cf-coverage-map {
	position: relative;
	width: 320px; height: 320px;
	display: flex; align-items: center; justify-content: center;
	margin: 0 auto;
}
.cf-map-grid {
	position: absolute; inset: 0;
	background-image:
		linear-gradient(rgba(0,200,255,.06) 1px, transparent 1px),
		linear-gradient(90deg, rgba(0,200,255,.06) 1px, transparent 1px);
	background-size: 32px 32px;
	border-radius: var(--cf-r-lg);
}
.cf-cov-ring {
	position: absolute;
	border-radius: 50%;
	animation: cf-cov-pulse 3s ease-in-out infinite alternate;
}
.cf-cov-ring--1 { width: 60px;  height: 60px;  background: radial-gradient(circle, rgba(248,81,73,.55)  0%, rgba(248,81,73,.0) 70%);   animation-delay: 0s; }
.cf-cov-ring--2 { width: 130px; height: 130px; background: radial-gradient(circle, rgba(227,179,65,.45) 0%, rgba(227,179,65,.0) 70%);  animation-delay: .3s; }
.cf-cov-ring--3 { width: 210px; height: 210px; background: radial-gradient(circle, rgba(0,200,255,.35) 0%, rgba(0,200,255,.0) 70%);  animation-delay: .6s; }
.cf-cov-ring--4 { width: 290px; height: 290px; background: radial-gradient(circle, rgba(0,230,118,.18)  0%, rgba(0,230,118,.0) 70%);   animation-delay: .9s; }
@keyframes cf-cov-pulse {
	from { transform: scale(.95); opacity: .8; }
	to   { transform: scale(1.05); opacity: 1; }
}
.cf-tx-marker {
	position: relative; z-index: 10;
	width: 16px; height: 16px;
	display: flex; align-items: center; justify-content: center;
}
.cf-tx-dot {
	width: 10px; height: 10px;
	background: #fff;
	border-radius: 50%;
	border: 2px solid var(--cf-accent);
	box-shadow: 0 0 12px rgba(0,200,255,.7);
	z-index: 2;
}
.cf-tx-pulse {
	position: absolute;
	width: 28px; height: 28px;
	border: 1.5px solid rgba(0,200,255,.5);
	border-radius: 50%;
	animation: cf-tx-blink 1.8s ease-out infinite;
}
@keyframes cf-tx-blink {
	0%   { transform: scale(.5); opacity: 1; }
	100% { transform: scale(2);  opacity: 0; }
}
.cf-studio-readout {
	position: absolute;
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border-2);
	border-radius: var(--cf-r-md);
	padding: 8px 14px;
	box-shadow: var(--cf-shadow-md);
	animation: cf-float 4s ease-in-out infinite;
}
.cf-studio-readout--tl { top: 16px; left: 8px; animation-delay: 0s; }
.cf-studio-readout--br { bottom: 16px; right: 8px; animation-delay: 2s; }
.cf-readout-label { font-size: 10px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: var(--cf-text-3); }
.cf-readout-val   { font-size: 16px; font-weight: 700; color: var(--cf-accent); }

/* Propagation spec panel */
.cf-studio-spec-panel {
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-xl);
	padding: 24px;
}
.cf-spec-header {
	font-size: 10px; font-weight: 700; letter-spacing: 1px;
	text-transform: uppercase; color: var(--cf-text-3);
	margin-bottom: 12px; margin-top: 4px;
}
.cf-spec-row {
	display: flex; justify-content: space-between; align-items: center;
	padding: 9px 0;
	border-bottom: 1px solid var(--cf-border);
	font-size: 13px;
}
.cf-spec-row:last-of-type { border-bottom: none; }
.cf-spec-name { color: var(--cf-text-2); }
.cf-spec-check { color: var(--cf-success); font-weight: 700; font-size: 12px; }
.cf-spec-divider { border: none; border-top: 1px solid var(--cf-border-2); margin: 12px 0; }

/* Algorithm list */
.cf-studio-algo-list {
	display: flex; flex-direction: column; gap: 16px; margin-top: 20px;
}
.cf-algo-item {
	display: flex; gap: 16px; align-items: flex-start;
	padding: 16px;
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-md);
}
.cf-algo-num {
	font-size: 11px; font-weight: 700; color: var(--cf-accent);
	letter-spacing: 1px; flex-shrink: 0; padding-top: 2px;
}
.cf-algo-item strong { display: block; font-size: 14px; margin-bottom: 4px; color: var(--cf-text); }
.cf-algo-item span   { font-size: 13px; color: var(--cf-text-2); line-height: 1.6; }

/* RF Chain */
.cf-rf-chain-demo {
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-xl);
	padding: 32px;
}
.cf-rf-chain {
	display: flex;
	align-items: center;
	gap: 8px;
	flex-wrap: wrap;
	justify-content: center;
}
.cf-rf-component {
	background: var(--cf-bg-3);
	border: 1px solid var(--cf-border-2);
	border-radius: var(--cf-r-md);
	padding: 14px 18px;
	text-align: center;
	min-width: 100px;
}
.cf-rf-component--tx  { border-color: rgba(0,200,255,.4); }
.cf-rf-component--erp { border-color: rgba(0,230,118,.4); background: rgba(0,230,118,.05); }
.cf-rf-comp-icon { font-size: 20px; margin-bottom: 6px; }
.cf-rf-comp-name { font-size: 11px; color: var(--cf-text-2); margin-bottom: 4px; }
.cf-rf-comp-val  { font-size: 14px; font-weight: 700; color: var(--cf-text); }
.cf-rf-loss      { color: var(--cf-danger); }
.cf-rf-gain      { color: var(--cf-success); }
.cf-rf-arrow     { font-size: 18px; color: var(--cf-text-3); }

/* FCC table rows */
.cf-fcc-row {
	display: grid;
	grid-template-columns: 1fr 1fr 1fr auto;
	gap: 8px;
	align-items: center;
	padding: 10px 12px;
	background: var(--cf-bg-3);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-md);
	font-size: 13px;
	animation: cf-fcc-in .4s ease both;
	animation-delay: calc(var(--i) * 0.1s);
}
@keyframes cf-fcc-in {
	from { opacity: 0; transform: translateX(-8px); }
	to   { opacity: 1; transform: translateX(0); }
}
.cf-fcc-call { font-weight: 700; color: var(--cf-accent); }
.cf-fcc-freq { color: var(--cf-text-2); }
.cf-fcc-erp  { color: var(--cf-text-2); }
.cf-fcc-type {
	font-size: 10px; font-weight: 700; letter-spacing: .5px;
	background: var(--cf-accent-dim); color: var(--cf-accent);
	border-radius: var(--cf-r-sm); padding: 2px 8px;
}

/* Mini coverage demo (in split-visual) */
.cf-mini-coverage-demo {
	position: relative; width: 160px; height: 160px;
	display: flex; align-items: center; justify-content: center;
}
.cf-mini-ring {
	position: absolute; border-radius: 50%;
	animation: cf-cov-pulse 2.5s ease-in-out infinite alternate;
}
.cf-mini-ring--1 { width: 36px;  height: 36px;  background: radial-gradient(circle, rgba(248,81,73,.6) 0%, transparent 70%); }
.cf-mini-ring--2 { width: 80px;  height: 80px;  background: radial-gradient(circle, rgba(227,179,65,.4) 0%, transparent 70%); animation-delay:.2s; }
.cf-mini-ring--3 { width: 140px; height: 140px; background: radial-gradient(circle, rgba(0,230,118,.25) 0%, transparent 70%); animation-delay:.4s; }
.cf-mini-dot { width: 8px; height: 8px; border-radius: 50%; background: #fff; border: 2px solid var(--cf-accent); box-shadow: 0 0 8px rgba(0,200,255,.8); z-index: 2; }

/* Manufacturer grid */
.cf-mfr-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 6px;
	margin-top: 12px;
}
.cf-mfr-grid span {
	font-size: 10px;
	font-weight: 600;
	color: var(--cf-text-3);
	background: var(--cf-bg-raised);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-sm);
	padding: 3px 8px;
	letter-spacing: .3px;
}

/* ── Technologies grid ─────────────────────────── */
.cf-tech-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 20px;
}
.cf-tech-card {
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-xl);
	padding: 22px;
	transition: border-color .2s, transform .2s;
}
.cf-tech-card:hover {
	border-color: rgba(0,200,255,.35);
	transform: translateY(-3px);
}
.cf-tech-card-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 12px;
}
.cf-tech-icon { font-size: 26px; }
.cf-tech-band {
	font-size: 10px;
	font-weight: 700;
	letter-spacing: .4px;
	color: var(--cf-accent);
	background: var(--cf-accent-dim);
	border: 1px solid rgba(0,200,255,.2);
	border-radius: var(--cf-r-full);
	padding: 3px 8px;
	white-space: nowrap;
}
.cf-tech-card h4 {
	font-size: 15px;
	font-weight: 700;
	margin-bottom: 8px;
	color: var(--cf-text);
}
.cf-tech-card p {
	font-size: 13px;
	color: var(--cf-text-2);
	line-height: 1.65;
	margin-bottom: 14px;
}
.cf-tech-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 5px;
}
.cf-tech-tags span {
	font-size: 10px;
	font-weight: 600;
	color: var(--cf-text-3);
	background: var(--cf-bg-3);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-sm);
	padding: 2px 7px;
}

/* ── Persona cards ─────────────────────────────── */
.cf-persona-grid {
	display: grid;
	grid-template-columns: 1fr 1fr 1fr;
	gap: 24px;
}
.cf-persona-card {
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-xl);
	padding: 28px;
	display: flex;
	flex-direction: column;
	gap: 14px;
}
.cf-persona-card:hover { border-color: rgba(0,200,255,.3); }
.cf-persona-header {
	display: flex;
	align-items: flex-start;
	gap: 14px;
}
.cf-persona-icon {
	font-size: 28px;
	flex-shrink: 0;
	width: 52px; height: 52px;
	background: var(--cf-bg-3);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-lg);
	display: flex; align-items: center; justify-content: center;
}
.cf-persona-role {
	font-size: 15px;
	font-weight: 700;
	color: var(--cf-text);
	line-height: 1.3;
}
.cf-persona-sub {
	font-size: 11px;
	font-weight: 600;
	color: var(--cf-accent);
	margin-top: 3px;
	letter-spacing: .3px;
}
.cf-persona-card > p {
	font-size: 13px;
	color: var(--cf-text-2);
	line-height: 1.7;
}
.cf-persona-workflow {
	list-style: none;
	padding: 0; margin: 0;
	display: flex;
	flex-direction: column;
	gap: 8px;
	border-top: 1px solid var(--cf-border);
	padding-top: 14px;
}
.cf-persona-workflow li {
	font-size: 12px;
	color: var(--cf-text-2);
	padding-left: 18px;
	position: relative;
	line-height: 1.5;
}
.cf-persona-workflow li::before {
	content: '→';
	position: absolute;
	left: 0;
	color: var(--cf-accent);
	font-weight: 700;
}

/* ── AI Import section ─────────────────────────── */
.cf-ai-import-section {
	background: linear-gradient(160deg, rgba(0,200,255,.05) 0%, transparent 60%),
	            var(--cf-bg);
	border-top: 1px solid rgba(0,200,255,.15);
	border-bottom: 1px solid rgba(0,200,255,.15);
}
.cf-ai-import-header {
	text-align: center;
	max-width: 720px;
	margin: 0 auto 48px;
}
.cf-ai-badge-row {
	margin-bottom: 12px;
}
.cf-ai-label-pill {
	display: inline-block;
	font-size: 11px;
	font-weight: 700;
	letter-spacing: .8px;
	text-transform: uppercase;
	color: #fff;
	background: linear-gradient(90deg, #58a6ff, #3fb950);
	border-radius: var(--cf-r-full);
	padding: 4px 14px;
}
.cf-ai-lead {
	font-size: 15px;
	line-height: 1.7;
	color: var(--cf-text-2);
	margin-top: 16px;
}
.cf-ai-import-layout {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 56px;
	align-items: start;
}
/* AI steps */
.cf-ai-flow {
	display: flex;
	flex-direction: column;
	gap: 0;
}
.cf-ai-step {
	display: flex;
	gap: 18px;
	align-items: flex-start;
	padding: 20px 0;
	border-bottom: 1px solid var(--cf-border);
}
.cf-ai-step:last-child { border-bottom: none; }
.cf-ai-step-num {
	flex-shrink: 0;
	width: 32px; height: 32px;
	border-radius: 50%;
	background: linear-gradient(135deg, #58a6ff, #1f6feb);
	color: #fff;
	font-size: 13px;
	font-weight: 700;
	display: flex; align-items: center; justify-content: center;
	box-shadow: 0 0 16px rgba(0,200,255,.3);
}
.cf-ai-step-body {
	display: flex;
	flex-direction: column;
	gap: 4px;
}
.cf-ai-step-body strong {
	font-size: 14px;
	color: var(--cf-text);
}
.cf-ai-step-body span {
	font-size: 13px;
	color: var(--cf-text-2);
	line-height: 1.6;
}
/* Callout */
.cf-ai-callout {
	display: flex;
	gap: 16px;
	align-items: flex-start;
	background: rgba(0,230,118,.07);
	border: 1px solid rgba(0,230,118,.25);
	border-radius: var(--cf-r-lg);
	padding: 18px 20px;
	margin-top: 24px;
}
.cf-ai-callout-icon { font-size: 24px; flex-shrink: 0; }
.cf-ai-callout strong {
	display: block;
	font-size: 14px;
	color: var(--cf-success);
	margin-bottom: 4px;
}
.cf-ai-callout span {
	font-size: 13px;
	color: var(--cf-text-2);
	line-height: 1.6;
}
/* Component type chips */
.cf-ai-component-types {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-top: 20px;
}
.cf-ai-comp-type {
	font-size: 12px;
	font-weight: 600;
	color: var(--cf-text-2);
	background: var(--cf-bg-2);
	border: 1px solid var(--cf-border);
	border-radius: var(--cf-r-full);
	padding: 5px 14px;
}
/* Visual side */
.cf-ai-import-visual {
	position: sticky;
	top: 100px;
}
.cf-ai-import-caption {
	text-align: center;
	font-size: 12px;
	color: var(--cf-text-3);
	margin-top: 12px;
	line-height: 1.6;
}
.cf-ai-import-caption strong {
	color: var(--cf-accent);
}
/* Window chrome */
.cf-win-chrome {
	background: var(--cf-bg-3);
	padding: 10px 14px;
	border-bottom: 1px solid var(--cf-border);
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
	color: var(--cf-text-3);
}
.cf-win-dot {
	width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
}
.cf-win-dot--r { background: #f85149; }
.cf-win-dot--y { background: #e3b341; }
.cf-win-dot--g { background: #3fb950; }
/* AI badge variant */
.cf-studio-badge--ai {
	background: linear-gradient(90deg, rgba(0,200,255,.15), rgba(0,230,118,.12));
	border-color: rgba(0,200,255,.4);
	color: #fff;
}

/* Responsive */
@media (max-width: 1200px) {
	.cf-tech-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
	.cf-studio-hero-inner { grid-template-columns: 1fr; }
	.cf-icon-grid--4 { grid-template-columns: 1fr 1fr; }
	.cf-icon-grid--4 .cf-icon-feature[style*="grid-column:span 2"] { grid-column: span 2; }
	.cf-ai-import-layout { grid-template-columns: 1fr; }
	.cf-ai-import-visual { position: static; }
	.cf-persona-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 600px) {
	.cf-icon-grid--4 { grid-template-columns: 1fr; }
	.cf-icon-grid--4 .cf-icon-feature[style*="grid-column:span 2"] { grid-column: span 1; }
	.cf-tech-grid { grid-template-columns: 1fr; }
	.cf-persona-grid { grid-template-columns: 1fr; }
}
</style>
