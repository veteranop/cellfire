<?php
/**
 * Cellfire Template — Article Layout: Cellfire HTML Tool (Viewer)
 * Assign this layout to a blank article, then set menu alias to "viewer"
 */
defined('_JEXEC') or die;
?>
<div class="cf-page-wrap">

	<!-- Hero -->
	<div class="cf-page-hero">
		<div class="cf-container">
			<div class="cf-split" style="gap:56px;align-items:center;">
				<div class="cf-split-text" style="max-width:480px;">
					<div class="cf-page-hero-label">Browser Tool · No Install Required</div>
					<h1>Cellfire <span class="cf-accent">Viewer</span></h1>
					<p>Browser-based cellular tower locator and PCI analysis tool. Upload your field data and instantly visualize RSRP coverage on an interactive map.</p>
					<div class="cf-page-hero-actions" style="margin-bottom:24px;">
						<a href="https://indigo-licha-20.tiiny.site/?mode=suggestions" class="cf-btn cf-btn--primary" target="_blank" rel="noopener noreferrer">Launch Viewer →</a>
						<a href="/pricing" class="cf-btn cf-btn--ghost">Get Full Access</a>
					</div>
					<div style="font-size:12px;color:var(--cf-text-3);">Works in Chrome, Firefox, Edge &nbsp;·&nbsp; No account needed for free version</div>
				</div>

				<!-- Browser mockup -->
				<div style="flex:1;min-width:0;">
					<div class="cf-browser-frame">
						<div class="cf-browser-bar">
							<div class="cf-browser-dots">
								<div class="cf-browser-dot cf-browser-dot--r"></div>
								<div class="cf-browser-dot cf-browser-dot--y"></div>
								<div class="cf-browser-dot cf-browser-dot--g"></div>
							</div>
							<div class="cf-browser-url">cellfire.io/viewer · Cellfire Viewer</div>
						</div>
						<div class="cf-browser-content" style="min-height:300px;">
							<div class="cf-browser-mapgrid"></div>
							<!-- Heatmap blobs -->
							<div class="cf-heatmap-dot cf-heatmap-dot--1"></div>
							<div class="cf-heatmap-dot cf-heatmap-dot--2"></div>
							<div class="cf-heatmap-dot cf-heatmap-dot--3"></div>
							<div class="cf-heatmap-dot cf-heatmap-dot--4"></div>
							<!-- Tower pins -->
							<div class="cf-tower-pin cf-tower-pin--1"></div>
							<div class="cf-tower-pin cf-tower-pin--2"></div>
							<div class="cf-tower-pin cf-tower-pin--3"></div>
							<!-- PCI labels -->
							<div class="cf-pci-label cf-pci-label--1">PCI 247</div>
							<div class="cf-pci-label cf-pci-label--2">PCI 183</div>
							<div class="cf-pci-label cf-pci-label--3">PCI 91</div>
							<!-- Sidebar panel -->
							<div class="cf-viewer-sidebar">
								<div class="cf-viewer-sidebar-title">PCI 247</div>
								<div class="cf-viewer-stat">
									<div class="cf-viewer-stat-label">Avg RSRP</div>
									<div class="cf-viewer-stat-val cf-viewer-stat-val--g">−74 dBm</div>
								</div>
								<div class="cf-viewer-stat">
									<div class="cf-viewer-stat-label">Samples</div>
									<div class="cf-viewer-stat-val">384</div>
								</div>
								<div class="cf-viewer-stat">
									<div class="cf-viewer-stat-label">Best RSRP</div>
									<div class="cf-viewer-stat-val cf-viewer-stat-val--g">−58 dBm</div>
								</div>
								<div class="cf-viewer-stat">
									<div class="cf-viewer-stat-label">Coverage</div>
									<div class="cf-viewer-stat-val cf-viewer-stat-val--c">Strong</div>
								</div>
								<!-- RSRP gradient bar -->
								<div style="margin-top:4px;">
									<div style="font-size:9px;color:var(--cf-text-3);text-transform:uppercase;letter-spacing:.5px;margin-bottom:4px;">Signal Range</div>
									<div class="cf-rsrp-bar"></div>
									<div class="cf-rsrp-labels"><span>Weak</span><span>Strong</span></div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Launch banner -->
	<div class="cf-page-section" style="padding:48px 0;">
		<div class="cf-container" style="text-align:center;">
			<div style="background:var(--cf-bg-2);border:1px solid rgba(0,200,255,.25);border-radius:var(--cf-r-xl);padding:40px;display:inline-block;max-width:640px;box-shadow:0 0 40px rgba(0,200,255,.07);">
				<div style="font-size:13px;color:var(--cf-text-2);margin-bottom:16px;">Works in Chrome, Firefox, Edge — no install, no account needed for the free version</div>
				<a href="https://indigo-licha-20.tiiny.site/?mode=suggestions" class="cf-btn cf-btn--primary" target="_blank" rel="noopener noreferrer" style="font-size:16px;padding:14px 36px;">
					Open Cellfire Viewer
				</a>
				<div style="font-size:11px;color:var(--cf-text-3);margin-top:14px;">Opens in a new tab · No sign-in required</div>
			</div>
		</div>
	</div>

	<!-- What you can do -->
	<div class="cf-page-section cf-page-section--alt">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Capabilities</div>
				<h2>Powerful analysis <span class="cf-accent">in your browser</span></h2>
			</div>
			<div class="cf-icon-grid cf-icon-grid--3">
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🗺️</div>
					<h4>Interactive Map</h4>
					<p>Plot tower locations and signal samples on a zoomable, pannable map with satellite or street view basemaps.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📶</div>
					<h4>RSRP Gradient</h4>
					<p>Color-coded signal strength overlay from deep red (weak) through yellow to bright green (strong, −70 dBm+).</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🔢</div>
					<h4>PCI Cluster Analysis</h4>
					<p>Identify Physical Cell ID clusters, spot conflicts, and visualize neighbor relationships spatially.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">📂</div>
					<h4>File Upload</h4>
					<p>Drag and drop your CSV or compatible field data. The viewer parses and renders it instantly with no server upload.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🔍</div>
					<h4>PCI Detail Panel</h4>
					<p>Click any cell to open a detail panel with RSRP stats, sample count, and per-PCI signal distribution.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">💾</div>
					<h4>Zero Data Retention</h4>
					<p>All processing happens in your browser. Your data never leaves your machine — fully private by design.</p>
				</div>
			</div>
		</div>
	</div>

	<!-- How to use -->
	<div class="cf-page-section">
		<div class="cf-container" style="max-width:760px;">
			<div class="cf-section-head">
				<div class="cf-section-label">Getting Started</div>
				<h2>Up and running in <span class="cf-accent">60 seconds</span></h2>
			</div>
			<div class="cf-steps">
				<div class="cf-step cf-animate">
					<div class="cf-step-num">1</div>
					<div class="cf-step-body">
						<h4>Open the Viewer</h4>
						<p>Click "Launch Viewer" above. No sign-in required for the free version.</p>
					</div>
				</div>
				<div class="cf-step cf-animate">
					<div class="cf-step-num">2</div>
					<div class="cf-step-body">
						<h4>Upload Your Data</h4>
						<p>Drag your CSV file onto the drop zone, or click to browse. Supports Cellfire App exports and standard drive test formats.</p>
					</div>
				</div>
				<div class="cf-step cf-animate">
					<div class="cf-step-num">3</div>
					<div class="cf-step-body">
						<h4>Explore the Map</h4>
						<p>Zoom and pan to inspect signal coverage. Click towers or sample clusters to drill into PCI detail panels.</p>
					</div>
				</div>
				<div class="cf-step cf-animate">
					<div class="cf-step-num">4</div>
					<div class="cf-step-body">
						<h4>Export or Share</h4>
						<p>Screenshot the map view or export the processed data. Subscribers can save sessions and share direct links.</p>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- CTA -->
	<div class="cf-cta-band">
		<div class="cf-container">
			<h2>Want more? Get the <span class="cf-accent">full platform</span></h2>
			<p>A Cellfire subscription unlocks RF Studio, the mobile app, and advanced viewer features.</p>
			<div class="cf-hero-actions">
				<a href="/pricing" class="cf-btn cf-btn--primary">See Plans</a>
				<a href="https://indigo-licha-20.tiiny.site/?mode=suggestions" class="cf-btn cf-btn--ghost" target="_blank" rel="noopener noreferrer">Try Free Viewer</a>
			</div>
		</div>
	</div>

	<?php if (!empty($this->item->text)) : ?>
	<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
	<?php endif; ?>

</div>
