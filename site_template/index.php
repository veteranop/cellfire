<?php
/**
 * Cellfire Template — index.php
 * Dark high-tech portal template for cellfire.io
 */
defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Uri\Uri;
use Joomla\CMS\Language\Text;

/** @var Joomla\CMS\Document\HtmlDocument $this */
$app  = Factory::getApplication();
$wa   = $this->getWebAssetManager();
$menu = $app->getMenu();

// Template assets — relative path from Joomla root (most reliable across versions)
$tpl = $this->template; // 'cellfire'
$wa->registerAndUseStyle('template.cellfire', 'templates/' . $tpl . '/css/template.css');
$wa->registerAndUseScript('template.cellfire', 'templates/' . $tpl . '/js/template.js', ['defer' => true]);

// Google Fonts — Inter (plain addStyleSheet, no WAM wrapping needed for external URLs)
$this->addStyleSheet('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

// Is this the home/front page?
$isHome = ($menu->getActive() == $menu->getDefault());

// Template params
$siteTagline = $this->params->get('siteTagline', 'RF Intelligence Platform');
?>
<!DOCTYPE html>
<html lang="<?= $this->language; ?>" dir="<?= $this->direction; ?>">
<head><meta charset="utf-8">
	
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<jdoc:include type="metas" />

	<!-- ── SEO: Open Graph / Twitter Card ── -->
	<?php $canonicalUrl = rtrim(Uri::root(), '/') . Uri::getInstance()->toString(['path', 'query', 'fragment']); ?>
	<link rel="canonical" href="<?= htmlspecialchars($canonicalUrl); ?>" />
	<meta property="og:site_name"   content="Cellfire" />
	<meta property="og:type"        content="website" />
	<meta property="og:url"         content="<?= htmlspecialchars($canonicalUrl); ?>" />
	<meta property="og:title"       content="<?= htmlspecialchars($this->getTitle()); ?>" />
	<meta property="og:description" content="<?= htmlspecialchars($this->getMetaData('description') ?: 'Crowd-sourced cellular tower mapping and RF intelligence — T-Mobile, Verizon, AT&T and more. Analyze, visualize, and understand cellular coverage data.'); ?>" />
	<meta property="og:image"       content="<?= Uri::root(); ?>images/cellfire-social.png" />
	<meta property="og:image:width"  content="1200" />
	<meta property="og:image:height" content="630" />
	<meta name="twitter:card"        content="summary_large_image" />
	<meta name="twitter:title"       content="<?= htmlspecialchars($this->getTitle()); ?>" />
	<meta name="twitter:description" content="<?= htmlspecialchars($this->getMetaData('description') ?: 'Crowd-sourced cellular tower mapping and RF intelligence — T-Mobile, Verizon, AT&T and more.'); ?>" />
	<meta name="twitter:image"       content="<?= Uri::root(); ?>images/cellfire-social.png" />

	<!-- ── SEO: JSON-LD Structured Data ── -->
	<script type="application/ld+json">
	{
		"@context": "https://schema.org",
		"@graph": [
			{
				"@type": "WebSite",
				"@id": "https://cellfire.io/#website",
				"url": "https://cellfire.io/",
				"name": "Cellfire",
				"description": "Cellular RF intelligence platform — crowd-sourced tower mapping, signal analysis, and coverage visualization.",
				"potentialAction": {
					"@type": "SearchAction",
					"target": { "@type": "EntryPoint", "urlTemplate": "https://cellfire.io/?q={search_term_string}" },
					"query-input": "required name=search_term_string"
				}
			},
			{
				"@type": "Organization",
				"@id": "https://cellfire.io/#organization",
				"name": "Cellfire",
				"url": "https://cellfire.io/",
				"logo": {
					"@type": "ImageObject",
					"url": "https://cellfire.io/images/cellfire-social.png"
				}
			},
			{
				"@type": "SoftwareApplication",
				"name": "Cellfire App",
				"operatingSystem": "Android, iOS",
				"applicationCategory": "UtilitiesApplication",
				"description": "Collect, analyze, and share cellular RF data from your mobile device. Real-time signal monitoring with GPS tagging and cloud sync.",
				"url": "https://cellfire.io/app",
				"offers": {
					"@type": "Offer",
					"price": "4.99",
					"priceCurrency": "USD"
				},
				"publisher": { "@id": "https://cellfire.io/#organization" }
			}
		]
	}
	</script>

	<!-- ── SEO: FAQ Schema — targets featured snippets for high-volume questions ── -->
	<?php if ($isHome) : ?>
	<script type="application/ld+json">
	{
		"@context": "https://schema.org",
		"@type": "FAQPage",
		"mainEntity": [
			{
				"@type": "Question",
				"name": "How do I find cell towers near me?",
				"acceptedAnswer": { "@type": "Answer", "text": "Cellfire provides a crowd-sourced interactive map showing cell tower locations for T-Mobile, Verizon, AT&T, FirstNet, and more. Browse the coverage map at cellfire.io to see towers near you, including their carrier, LTE/5G bands, and PCI identifiers." }
			},
			{
				"@type": "Question",
				"name": "What is a PCI number for a cell tower?",
				"acceptedAnswer": { "@type": "Answer", "text": "PCI stands for Physical Cell Identity — a number from 0 to 503 that uniquely identifies a cell tower sector within its local area. PCIs help RF engineers distinguish between overlapping towers during drive testing and signal analysis. Cellfire tracks PCI data for every recorded tower." }
			},
			{
				"@type": "Question",
				"name": "What LTE and 5G bands does T-Mobile use?",
				"acceptedAnswer": { "@type": "Answer", "text": "T-Mobile primarily uses LTE Bands 2, 4, 12, 25, 41, 66, and 71, and 5G NR bands n41, n71, n25, n66, and n261. Band 71 (600 MHz) is T-Mobile's main low-band coverage layer. Cellfire identifies carrier bands for every recorded tower." }
			},
			{
				"@type": "Question",
				"name": "What is RF drive testing?",
				"acceptedAnswer": { "@type": "Answer", "text": "RF drive testing is the process of collecting cellular signal measurements while driving through an area. Engineers record signal strength (RSRP), quality (RSRQ), PCI, TAC, and GPS coordinates to analyze coverage gaps, interference, and carrier performance. Cellfire RF Studio is purpose-built for drive test analysis." }
			},
			{
				"@type": "Question",
				"name": "What is RSRP in LTE?",
				"acceptedAnswer": { "@type": "Answer", "text": "RSRP (Reference Signal Received Power) is the average power of LTE reference signals measured at the receiver, expressed in dBm. It indicates signal strength from a specific tower. Values above -80 dBm are excellent, -80 to -100 dBm are good, and below -110 dBm indicate poor coverage." }
			},
			{
				"@type": "Question",
				"name": "Can I use Cellfire for HAM radio signal analysis?",
				"acceptedAnswer": { "@type": "Answer", "text": "Yes. Cellfire offers a HAM Operator plan starting at $4.99/month. The platform supports signal monitoring, frequency analysis, and coverage visualization useful for amateur radio operators and spectrum enthusiasts." }
			}
		]
	}
	</script>
	<?php endif; ?>

	<jdoc:include type="styles" />
	<jdoc:include type="scripts" />
	<!-- Google Analytics -->
	<script async src="https://www.googletagmanager.com/gtag/js?id=G-M2WLVB4756"></script>
	<script>
		window.dataLayer = window.dataLayer || [];
		function gtag(){dataLayer.push(arguments);}
		gtag('js', new Date());
		gtag('config', 'G-M2WLVB4756');
	</script>
</head>
<body class="cellfire-body<?= $isHome ? ' is-home' : ''; ?>">

	<!-- Skip to content -->
	<a class="skip-link" href="#main-content">Skip to main content</a>

	<!-- Top Banner (optional module) -->
	<?php if ($this->countModules('banner')) : ?>
	<div class="cf-banner">
		<div class="cf-container">
			<jdoc:include type="modules" name="banner" style="none" />
		</div>
	</div>
	<?php endif; ?>

	<!-- ===================== HEADER ===================== -->
	<header class="cf-header" id="site-header">
		<div class="cf-container">
			<nav class="cf-nav" aria-label="Primary Navigation">

				<!-- Logo -->
				<a href="<?= Uri::base(); ?>" class="cf-logo" aria-label="Cellfire Home">
					<?php if ($this->params->get('logoFile')) : ?>
						<img src="<?= Uri::root() . htmlspecialchars($this->params->get('logoFile')); ?>"
							 alt="Cellfire" class="cf-logo-img">
					<?php else : ?>
						<span class="cf-logo-text">
							<span class="cf-logo-cell">Cell</span><span class="cf-logo-fire">fire</span><span class="cf-logo-io">.io</span>
						</span>
					<?php endif; ?>
				</a>

				<!-- Main navigation menu -->
				<div class="cf-nav-menu" id="cf-nav-menu" role="navigation">
					<jdoc:include type="modules" name="menu" style="none" />
				</div>

				<!-- Right side: user area + search -->
				<div class="cf-nav-right">
					<?php if ($this->countModules('search')) : ?>
					<div class="cf-nav-search">
						<jdoc:include type="modules" name="search" style="none" />
					</div>
					<?php endif; ?>
					<div class="cf-nav-user">
						<jdoc:include type="modules" name="user" style="none" />
					</div>
				</div>

				<!-- Mobile hamburger -->
				<button class="cf-hamburger" id="cf-hamburger"
						aria-label="Toggle navigation" aria-expanded="false" aria-controls="cf-nav-menu">
					<span></span><span></span><span></span>
				</button>

			</nav>
		</div>
	</header>

	<!-- ===================== HERO MODULE (non-homepage) ===================== -->
	<?php if (!$isHome && $this->countModules('hero')) : ?>
	<section class="cf-hero" id="hero">
		<div class="cf-hero-grid" aria-hidden="true"></div>
		<div class="cf-glow cf-glow--1" aria-hidden="true"></div>
		<div class="cf-glow cf-glow--2" aria-hidden="true"></div>
		<jdoc:include type="modules" name="hero" style="none" />
	</section>
	<?php endif; ?>

	<!-- ===================== MAP HERO (homepage) ===================== -->
	<?php if ($isHome) : ?>
	<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
	<style>
		.cf-map-hero { position: relative; height: 560px; overflow: hidden; background: #0d0d1a; display: flex; }
		#cfm-map { flex: 1; height: 100%; }
		.cfm-stats { position: absolute; top: 12px; left: 50%; transform: translateX(-50%); z-index: 1000; background: rgba(13,13,26,.88); border: 1px solid rgba(88,166,255,.2); backdrop-filter: blur(8px); border-radius: 8px; padding: 8px 20px; display: none; gap: 24px; pointer-events: none; }
		.cfm-stat { text-align: center; }
		.cfm-stat-val { font-size: 22px; font-weight: 700; color: #fff; line-height: 1; font-family: 'Inter', sans-serif; }
		.cfm-stat-lbl { font-size: 10px; color: rgba(255,255,255,.45); text-transform: uppercase; letter-spacing: 1px; margin-top: 3px; }
		.cfm-legend { position: absolute; bottom: 32px; left: 12px; z-index: 1000; background: rgba(13,13,26,.88); border: 1px solid rgba(88,166,255,.12); backdrop-filter: blur(8px); border-radius: 6px; padding: 8px 12px; font-size: 11px; color: rgba(255,255,255,.65); font-family: 'Inter', sans-serif; }
		.cfml-row { display: flex; align-items: center; gap: 7px; margin-bottom: 5px; }
		.cfml-row:last-child { margin-bottom: 0; }
		.cfml-swatch { width: 12px; height: 12px; border-radius: 2px; flex-shrink: 0; }
		.cfm-panel { position: absolute; right: 0; top: 0; bottom: 0; width: 310px; background: rgba(10,10,20,.97); border-left: 1px solid rgba(88,166,255,.12); z-index: 1001; display: flex; flex-direction: column; transform: translateX(100%); transition: transform .22s ease; }
		.cfm-panel.open { transform: translateX(0); }
		.cfm-panel-close { position: absolute; top: 10px; right: 12px; background: none; border: none; color: rgba(255,255,255,.4); font-size: 22px; cursor: pointer; line-height: 1; padding: 4px 6px; }
		.cfm-panel-close:hover { color: #fff; }
		.cfm-panel-body { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
		.cfm-carrier-bar { margin-bottom: 7px; }
		.cfm-carrier-bar-label { display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 3px; font-family: 'Inter', sans-serif; }
		.cfm-bar-track { height: 3px; background: rgba(255,255,255,.07); border-radius: 2px; }
		.cfm-bar-fill { height: 3px; border-radius: 2px; }
		.cfm-pci-tbl { width: 100%; border-collapse: collapse; font-size: 11px; font-family: 'Inter', sans-serif; }
		.cfm-pci-tbl th { color: rgba(255,255,255,.35); font-weight: 600; text-align: left; padding: 5px 8px; border-bottom: 1px solid rgba(255,255,255,.06); letter-spacing: .5px; font-size: 10px; text-transform: uppercase; }
		.cfm-pci-tbl td { padding: 5px 8px; border-bottom: 1px solid rgba(255,255,255,.04); vertical-align: middle; }
		.cfm-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
		.cfm-loading { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); z-index: 999; color: rgba(255,255,255,.4); font-size: 13px; pointer-events: none; font-family: 'Inter', sans-serif; }
		.cfm-seo-hero { position: absolute; bottom: 80px; left: 24px; z-index: 1000; pointer-events: none; max-width: 480px; }
		.cfm-seo-hero h1 { margin: 0 0 6px; font-size: 22px; font-weight: 700; color: #fff; font-family: 'Inter', sans-serif; line-height: 1.25; text-shadow: 0 2px 8px rgba(0,0,0,.7); }
		.cfm-seo-hero p  { margin: 0; font-size: 13px; color: rgba(255,255,255,.55); font-family: 'Inter', sans-serif; text-shadow: 0 1px 4px rgba(0,0,0,.8); }
	</style>

	<section class="cf-map-hero" id="hero">
		<div id="cfm-map">
			<div class="cfm-loading" id="cfm-loading">Loading coverage data…</div>
		</div>

		<!-- SEO: visible h1 text overlay — crawlable, styled to sit over the map -->
		<div class="cfm-seo-hero">
			<h1>Cellular Tower Coverage Map</h1>
			<p>Crowd-sourced RF intelligence — T-Mobile, Verizon, AT&amp;T, FirstNet &amp; more</p>
		</div>

		<!-- SEO: noscript fallback — crawlable text for Googlebot (JS disabled) -->
		<noscript>
			<div style="padding:24px;color:#fff;background:#0d0d1a;font-family:sans-serif;max-width:800px">
				<h2>Crowd-Sourced Cell Tower Coverage Map</h2>
				<p>Cellfire maps LTE and 5G cell tower locations across the United States using crowd-sourced data from the Cellfire Android and iOS app. Browse tower coverage by carrier — T-Mobile, Verizon, AT&amp;T, FirstNet, US Cellular, and Dish Wireless.</p>
				<p>Each tower record includes the carrier name, LTE/5G band, Physical Cell ID (PCI), Tracking Area Code (TAC), GPS coordinates, and signal confidence score. Data is updated automatically every hour.</p>
				<ul>
					<li>T-Mobile LTE bands: 2, 4, 12, 25, 41, 66, 71 — 5G NR: n41, n71, n261</li>
					<li>Verizon LTE bands: 2, 4, 5, 13, 66 — 5G NR: n77, n260</li>
					<li>AT&amp;T LTE bands: 2, 4, 5, 12, 14, 17, 66 — 5G NR: n77</li>
					<li>FirstNet LTE Band 14 — nationwide public safety network</li>
				</ul>
				<p>Tools include the <a href="/app">Cellfire App</a> for mobile signal collection, <a href="/studio">Cellfire RF Studio</a> for drive test analysis, and the <a href="/viewer">Cellfire HTML Viewer</a> for browser-based PCI and RSRP visualization.</p>
			</div>
		</noscript>

		<div class="cfm-stats" id="cfm-stats">
			<div class="cfm-stat">
				<div class="cfm-stat-val" id="cfm-tile-count">—</div>
				<div class="cfm-stat-lbl">Tiles</div>
			</div>
			<div class="cfm-stat">
				<div class="cfm-stat-val" id="cfm-cell-count">—</div>
				<div class="cfm-stat-lbl">Cells</div>
			</div>
		</div>

		<div class="cfm-legend">
			<div class="cfml-row"><div class="cfml-swatch" style="background:#ff6b6b"></div>1–4 cells</div>
			<div class="cfml-row"><div class="cfml-swatch" style="background:#ffa94d"></div>5–14 cells</div>
			<div class="cfml-row"><div class="cfml-swatch" style="background:#ffd43b"></div>15–39 cells</div>
			<div class="cfml-row"><div class="cfml-swatch" style="background:#51cf66"></div>40+ cells</div>
		</div>

		<div class="cfm-panel" id="cfm-panel">
			<button class="cfm-panel-close" onclick="cfmClosePanel()" aria-label="Close">&times;</button>
			<div class="cfm-panel-body" id="cfm-panel-body">
				<div style="padding:48px 16px;text-align:center;color:rgba(255,255,255,.25);font-size:13px;font-family:'Inter',sans-serif;">
					Click a tile to explore
				</div>
			</div>
		</div>

	</section>

	<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
	<script>
	(function () {
		var TILE_INDEX = '/files/cellfire-db/tile_index.json';
		var TILES_BASE = '/files/cellfire-db/';

		var CARRIER_COLORS = {
			'T-Mobile':    '#e91e63',
			'Verizon':     '#f44336',
			'AT&T':        '#2196f3',
			'FirstNet':    '#1565c0',
			'US Cellular': '#9c27b0',
			'Dish':        '#ff9800',
			'Unknown':     '#555',
		};

		function carrierColor(c) {
			for (var k in CARRIER_COLORS) {
				if (c && c.indexOf(k) !== -1) return CARRIER_COLORS[k];
			}
			return '#777';
		}

		function tileStyle(count, selected) {
			if (selected) return { color: '#fff', fillColor: '#fff', fillOpacity: 0.6, weight: 2 };
			if (count === 0) return { color: '#333', fillColor: '#1a1a2e', fillOpacity: 0.4, weight: 1 };
			if (count < 5)   return { color: '#ff6b6b', fillColor: '#ff6b6b', fillOpacity: 0.35, weight: 1 };
			if (count < 15)  return { color: '#ffa94d', fillColor: '#ffa94d', fillOpacity: 0.45, weight: 1 };
			if (count < 40)  return { color: '#ffd43b', fillColor: '#ffd43b', fillOpacity: 0.50, weight: 1 };
			return { color: '#51cf66', fillColor: '#51cf66', fillOpacity: 0.55, weight: 1 };
		}

		function confColor(conf) {
			if (conf >= 100) return '#1b5e20';
			if (conf >= 75)  return '#4caf50';
			if (conf >= 40)  return '#ffd700';
			if (conf > 0)    return '#ff5252';
			return '#555';
		}

		var mapInst, tileData = [], tileRects = {}, selKey = null;

		mapInst = L.map('cfm-map', { preferCanvas: true }).setView([39.5, -98.5], 4);
		L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
			attribution: '&copy; <a href="https://openstreetmap.org">OSM</a> &copy; <a href="https://carto.com">CARTO</a>',
			subdomains: 'abcd', maxZoom: 19,
		}).addTo(mapInst);

		function renderRects() {
			Object.keys(tileRects).forEach(function (k) { tileRects[k].remove(); });
			tileRects = {};
			tileData.forEach(function (tile) {
				var bounds = [[tile.lat, tile.lon], [tile.lat + 0.5, tile.lon + 0.5]];
				var rect = L.rectangle(bounds, tileStyle(tile.cell_count, false)).addTo(mapInst);
				var cList = Object.keys(tile.carriers || {})
					.sort(function (a, b) { return tile.carriers[b] - tile.carriers[a]; })
					.map(function (c) { return c + ': ' + tile.carriers[c]; }).join(', ');
				rect.bindTooltip(
					'<b>' + tile.key + '</b><br>' + tile.cell_count + ' cells' +
					(cList ? '<br><span style="color:#aaa">' + cList + '</span>' : ''),
					{ sticky: true }
				);
				rect.on('click', function () { cfmSelectTile(tile); });
				tileRects[tile.key] = rect;
			});
		}

		function loadTiles() {
			fetch(TILE_INDEX + '?_=' + Date.now())
				.then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
				.then(function (data) {
					tileData = data.tiles || [];
					document.getElementById('cfm-loading').style.display = 'none';
					var stats = document.getElementById('cfm-stats');
					stats.style.display = 'flex';
					document.getElementById('cfm-tile-count').textContent = (data.total_tiles || tileData.length).toLocaleString();
					document.getElementById('cfm-cell-count').textContent = (data.total_cells || 0).toLocaleString();
					renderRects();
					if (tileData.length) {
						var coords = [];
						tileData.forEach(function (t) {
							coords.push([t.lat, t.lon]);
							coords.push([t.lat + 0.5, t.lon + 0.5]);
						});
						mapInst.fitBounds(coords, { padding: [30, 30] });
					}
				})
				.catch(function (e) {
					document.getElementById('cfm-loading').style.display = 'block';
				document.getElementById('cfm-loading').textContent = 'Error: ' + (e && e.message ? e.message : String(e));
				});
		}

		function fetchGzipJson(url) {
			return fetch(url).then(function (resp) {
				if (!resp.ok) throw new Error('HTTP ' + resp.status);
				if (typeof DecompressionStream !== 'undefined') {
					var ds = new DecompressionStream('gzip');
					return new Response(resp.body.pipeThrough(ds)).text().then(JSON.parse);
				}
				// Fallback: server may serve with Content-Encoding:gzip — try direct JSON parse
				return resp.text().then(JSON.parse);
			});
		}

		function cfmSelectTile(tile) {
			selKey = tile.key;
			Object.keys(tileRects).forEach(function (k) {
				var t = tileData.find(function (x) { return x.key === k; });
				tileRects[k].setStyle(tileStyle(t ? t.cell_count : 0, k === selKey));
			});
			var panel = document.getElementById('cfm-panel');
			var body  = document.getElementById('cfm-panel-body');
			panel.classList.add('open');
			body.innerHTML = '<div style="padding:16px;color:rgba(255,255,255,.35);font-size:12px;font-family:\'Inter\',sans-serif">Loading…</div>';
			fetchGzipJson(TILES_BASE + tile.key + '.json.gz')
				.then(function (records) { cfmRenderDetail(tile, records); })
				.catch(function (e) {
					body.innerHTML = '<div style="padding:16px;color:#ff6b6b;font-size:12px">Error: ' + e.message + '</div>';
				});
		}

		function cfmClosePanel() {
			document.getElementById('cfm-panel').classList.remove('open');
			selKey = null;
			Object.keys(tileRects).forEach(function (k) {
				var t = tileData.find(function (x) { return x.key === k; });
				tileRects[k].setStyle(tileStyle(t ? t.cell_count : 0, false));
			});
		}
		window.cfmClosePanel = cfmClosePanel;

		function cfmRenderDetail(tile, records) {
			var byCarrier = {};
			records.forEach(function (r) {
				var c = r.carrier || 'Unknown';
				byCarrier[c] = (byCarrier[c] || 0) + 1;
			});
			var sorted = Object.keys(byCarrier)
				.map(function (c) { return [c, byCarrier[c]]; })
				.sort(function (a, b) { return b[1] - a[1]; });
			var maxN = sorted.length ? sorted[0][1] : 1;

			var bars = sorted.map(function (entry) {
				var c = entry[0], n = entry[1];
				var pct = Math.round(n / records.length * 100);
				var wpx = Math.round(n / maxN * 100);
				var col = carrierColor(c);
				return '<div class="cfm-carrier-bar">' +
					'<div class="cfm-carrier-bar-label">' +
						'<span style="color:' + col + ';font-weight:600">' + c + '</span>' +
						'<span style="color:rgba(255,255,255,.35)">' + n + ' (' + pct + '%)</span>' +
					'</div>' +
					'<div class="cfm-bar-track"><div class="cfm-bar-fill" style="width:' + wpx + '%;background:' + col + '"></div></div>' +
				'</div>';
			}).join('');

			var rows = records.map(function (r) {
				var col   = carrierColor(r.carrier || 'Unknown');
				var bands = (r.possible_bands || []).map(function (b) { return 'B' + b; }).join(' ') || '—';
				return '<tr>' +
					'<td><span class="cfm-dot" style="background:' + confColor(r.conf || 0) + '"></span>' + (r.conf || 0) + '</td>' +
					'<td style="font-weight:600">' + r.pci + '</td>' +
					'<td style="color:rgba(255,255,255,.35)">' + (r.tac || 0) + '</td>' +
					'<td style="color:' + col + ';font-weight:500">' + (r.carrier || 'Unknown') + '</td>' +
					'<td style="color:rgba(255,255,255,.35);font-size:10px">' + bands + '</td>' +
				'</tr>';
			}).join('');

			document.getElementById('cfm-panel-body').innerHTML =
				'<div style="padding:10px 14px 8px;border-bottom:1px solid rgba(255,255,255,.06);flex-shrink:0">' +
					'<div style="font-size:12px;font-weight:700;color:#fff;word-break:break-all;font-family:\'Inter\',sans-serif">' + tile.key + '</div>' +
					'<div style="font-size:11px;color:rgba(255,255,255,.35);margin-top:3px">' + records.length + ' cells · avg conf ' + (tile.avg_conf || 0) + '</div>' +
				'</div>' +
				'<div style="padding:10px 14px;border-bottom:1px solid rgba(255,255,255,.06);flex-shrink:0">' +
					'<div style="font-size:10px;text-transform:uppercase;letter-spacing:1px;color:rgba(255,255,255,.3);margin-bottom:8px">Carriers</div>' +
					bars +
				'</div>' +
				'<div style="flex:1;overflow-y:auto">' +
					'<table class="cfm-pci-tbl">' +
						'<thead><tr><th>Conf</th><th>PCI</th><th>TAC</th><th>Carrier</th><th>Bands</th></tr></thead>' +
						'<tbody>' + rows + '</tbody>' +
					'</table>' +
				'</div>';
		}

		loadTiles();
	})();
	</script>
	<?php endif; ?>

	<!-- ===================== GALLERY / SCREENSHOTS (homepage) ===================== -->
	<?php if ($isHome) : ?>
	<section class="cf-section cf-gallery" id="gallery">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Screenshots</div>
				<h2>See It in <span class="cf-accent">Action</span></h2>
				<p>Real screenshots from the tools — click a tab to explore.</p>
			</div>
			<div class="cf-gtabs">
				<button class="cf-gtab active" data-target="gallery-app">App</button>
				<button class="cf-gtab" data-target="gallery-studio">Studio</button>
				<button class="cf-gtab" data-target="gallery-viewer">Viewer</button>
			</div>

			<?php
			$galleries = [
				'app'    => ['folder' => 'Cellfire_app', 'files' => [
					'Screenshot 2026-03-01 190817.png',
					'Screenshot 2026-03-01 190853.png',
					'Screenshot 2026-03-01 191142.png',
					'Screenshot 2026-03-01 191234.png',
				]],
				'studio' => ['folder' => 'Studio', 'files' => [
					'Plot_20260130_072226.jpg',
					'Plot_20260202_082111.jpg',
					'Screenshot 2026-03-01 200640.png',
					'Screenshot 2026-03-01 200732.png',
					'Screenshot 2026-03-01 200934.png',
					'Screenshot 2026-03-01 200948.png',
				]],
				'viewer' => ['folder' => 'Viewer', 'files' => []],
			];
			?>

			<?php foreach ($galleries as $key => $gallery) : ?>
			<div class="cf-slideshow<?= $key === 'app' ? ' active' : ''; ?>" id="gallery-<?= $key; ?>">
				<?php if (!empty($gallery['files'])) : ?>
				<button class="cf-slide-arrow cf-slide-prev" aria-label="Previous">&#8592;</button>
				<div class="cf-slide-track">
					<?php foreach ($gallery['files'] as $img) : ?>
					<div class="cf-slide">
						<img src="/images/<?= $gallery['folder']; ?>/<?= rawurlencode($img); ?>" alt="<?= ucfirst($key); ?> screenshot" loading="lazy">
					</div>
					<?php endforeach; ?>
				</div>
				<button class="cf-slide-arrow cf-slide-next" aria-label="Next">&#8594;</button>
				<div class="cf-slide-dots"></div>
				<?php else : ?>
				<div class="cf-gallery-empty">
					<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect x="6" y="10" width="36" height="28" rx="3" stroke="#484f58" stroke-width="2"/><circle cx="16" cy="20" r="3" stroke="#484f58" stroke-width="1.5"/><path d="M6 32l9-8 7 6 5-4 9 8" stroke="#484f58" stroke-width="1.5" stroke-linejoin="round"/></svg>
					<p>Viewer screenshots coming soon — upload to <code>images/Viewer/</code></p>
				</div>
				<?php endif; ?>
			</div>
			<?php endforeach; ?>
		</div>
	</section>
	<?php endif; ?>

	<!-- ===================== PRODUCTS (homepage) ===================== -->
	<?php if ($isHome) : ?>
	<section class="cf-section cf-products" id="products">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Platform</div>
				<h2>Three Tools. <span class="cf-accent">One Subscription.</span></h2>
				<p>Everything you need to analyze, visualize, and understand cellular RF data.</p>
			</div>

			<div class="cf-products-grid">

				<!-- RF Studio -->
				<div class="cf-product-card">
					<div class="cf-product-icon">
						<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
							<path d="M8 34 L18 20 L26 26 L34 14 L42 19" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
							<circle cx="8"  cy="34" r="2.5" fill="#58a6ff"/>
							<circle cx="42" cy="19" r="2.5" fill="#58a6ff"/>
							<line x1="8"  y1="40" x2="42" y2="40" stroke="#30363d" stroke-width="1.5"/>
						</svg>
					</div>
					<div class="cf-product-tag">Desktop App</div>
					<h3>Cellfire RF Studio</h3>
					<p>Advanced signal analysis and network intelligence software. Visualize, analyze, and report on cellular coverage data with professional precision.</p>
					<ul class="cf-feature-list">
						<li>Multi-carrier signal analysis</li>
						<li>Coverage heat mapping</li>
						<li>Drive test data import</li>
						<li>PDF/CSV reporting</li>
					</ul>
					<a href="/studio" class="cf-product-link">Learn more →</a>
				</div>

				<!-- Cellfire App — featured -->
				<div class="cf-product-card cf-product-card--featured">
					<div class="cf-product-accent-bar"></div>
					<div class="cf-product-icon">
						<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
							<rect x="14" y="6" width="20" height="36" rx="4" stroke="#58a6ff" stroke-width="2.5"/>
							<circle cx="24" cy="36" r="2" fill="#58a6ff"/>
							<line x1="18" y1="12" x2="30" y2="12" stroke="#58a6ff" stroke-width="1.5" stroke-linecap="round"/>
							<line x1="18" y1="18" x2="28" y2="18" stroke="#58a6ff" stroke-width="1" stroke-linecap="round" opacity=".5"/>
							<line x1="18" y1="23" x2="26" y2="23" stroke="#58a6ff" stroke-width="1" stroke-linecap="round" opacity=".5"/>
						</svg>
					</div>
					<div class="cf-product-tag cf-product-tag--accent">Mobile</div>
					<h3>Cellfire App</h3>
					<p>Collect, analyze, and share cellular RF data from your mobile device. Real-time signal monitoring with GPS tagging and cloud sync.</p>
					<ul class="cf-feature-list">
						<li>Real-time signal monitoring</li>
						<li>GPS-tagged data collection</li>
						<li>Cloud sync &amp; team sharing</li>
						<li>iOS &amp; Android</li>
					</ul>
					<a href="/app" class="cf-product-link">Learn more →</a>
				</div>

				<!-- HTML Tool -->
				<div class="cf-product-card">
					<div class="cf-product-icon">
						<svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
							<path d="M12 16 L6 24 L12 32" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
							<path d="M36 16 L42 24 L36 32" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
							<line x1="28" y1="10" x2="20" y2="38" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round"/>
						</svg>
					</div>
					<div class="cf-product-tag">Browser Tool</div>
					<h3>Cellfire HTML Tool</h3>
					<p>Browser-based tower locator and PCI analysis. Upload field data and instantly visualize RSRP coverage on an interactive map — no install needed.</p>
					<ul class="cf-feature-list">
						<li>Interactive map visualization</li>
						<li>PCI cluster analysis</li>
						<li>RSRP gradient mapping</li>
						<li>Works in any browser</li>
					</ul>
					<a href="/viewer" class="cf-product-link">Learn more →</a>
				</div>

			</div>
		</div>
	</section>

	<!-- ===================== PRICING ===================== -->
<section class='cf-section cf-pricing' id='pricing'>
				<div class='cf-container'>
					<div class='cf-section-head'>
						<div class='cf-section-label'>Pricing</div>
						<h2>Plans for Every Use Case</h2>
						<p>From hobbyist HAM radio operators to enterprise deployments. All plans include a 30-day free trial.</p>
					</div>
					<div class='hp-price-cards'>
						<div class='hp-pc'>
							<div class='hp-pc-name'>HAM Operator</div>
							<div class='hp-pc-price'>$4.99<span>/mo</span></div>
							<div class='hp-pc-yr'>or $50/year</div>
						</div>
						<div class='hp-pc'>
							<div class='hp-pc-name'>Standard</div>
							<div class='hp-pc-price'>$9.99<span>/mo</span></div>
							<div class='hp-pc-yr'>or $100/year</div>
						</div>
						<div class='hp-pc hp-pc-hot'>
							<div class='hp-pc-badge'>MOST POPULAR</div>
							<div class='hp-pc-name'>Premium</div>
							<div class='hp-pc-price'>$19.99<span>/mo</span></div>
							<div class='hp-pc-yr'>or $200/year</div>
						</div>
						<div class='hp-pc'>
							<div class='hp-pc-name'>Amateur Club</div>
							<div class='hp-pc-price'>$34.99<span>/mo</span></div>
							<div class='hp-pc-yr'>or $350/year</div>
						</div>
						<div class='hp-pc'>
							<div class='hp-pc-name'>Enterprise</div>
							<div class='hp-pc-price'>$200<span>/mo</span></div>
							<div class='hp-pc-yr'>or $1,980/year</div>
						</div>
					</div>
					<div class='hp-price-cta'>
						<a href='/pricing' class='cf-btn cf-btn-primary'>View All Plans &amp; Features &rarr;</a>
						<p class='hp-price-note'>Lifetime licenses available &mdash; <a href='/pricing'>see pricing page</a> for full details.</p>
					</div>
				</div>
			</section>
	<?php endif; ?>

	<!-- ===================== OPTIONAL FEATURE MODULES ===================== -->
	<?php if ($this->countModules('feature-1') || $this->countModules('feature-2') || $this->countModules('feature-3')) : ?>
	<section class="cf-section cf-features">
		<div class="cf-container">
			<div class="cf-features-grid">
				<?php if ($this->countModules('feature-1')) : ?>
				<div class="cf-feature-block">
					<jdoc:include type="modules" name="feature-1" style="none" />
				</div>
				<?php endif; ?>
				<?php if ($this->countModules('feature-2')) : ?>
				<div class="cf-feature-block">
					<jdoc:include type="modules" name="feature-2" style="none" />
				</div>
				<?php endif; ?>
				<?php if ($this->countModules('feature-3')) : ?>
				<div class="cf-feature-block">
					<jdoc:include type="modules" name="feature-3" style="none" />
				</div>
				<?php endif; ?>
			</div>
		</div>
	</section>
	<?php endif; ?>

	<!-- ===================== MAIN CONTENT ===================== -->
	<main id="main-content" class="cf-main">
		<div class="cf-container">

			<?php if ($this->countModules('breadcrumbs')) : ?>
			<nav class="cf-breadcrumbs" aria-label="Breadcrumb">
				<jdoc:include type="modules" name="breadcrumbs" style="none" />
			</nav>
			<?php endif; ?>

			<?php $hasSidebar = $this->countModules('sidebar'); ?>
			<div class="cf-content-layout<?= $hasSidebar ? ' cf-content-layout--sidebar' : ''; ?>">

				<div class="cf-content-area">
					<jdoc:include type="message" />
					<jdoc:include type="component" />
				</div>

				<?php if ($hasSidebar) : ?>
				<aside class="cf-sidebar" aria-label="Sidebar">
					<jdoc:include type="modules" name="sidebar" style="none" />
				</aside>
				<?php endif; ?>

			</div>
		</div>
	</main>

	<!-- ===================== FOOTER ===================== -->
	<footer class="cf-footer">
		<div class="cf-container">

			<?php if ($this->countModules('footer-1') || $this->countModules('footer-2') || $this->countModules('footer-3')) : ?>
			<div class="cf-footer-grid">
				<?php if ($this->countModules('footer-1')) : ?>
				<div class="cf-footer-col">
					<jdoc:include type="modules" name="footer-1" style="none" />
				</div>
				<?php endif; ?>
				<?php if ($this->countModules('footer-2')) : ?>
				<div class="cf-footer-col">
					<jdoc:include type="modules" name="footer-2" style="none" />
				</div>
				<?php endif; ?>
				<?php if ($this->countModules('footer-3')) : ?>
				<div class="cf-footer-col">
					<jdoc:include type="modules" name="footer-3" style="none" />
				</div>
				<?php endif; ?>
			</div>
			<hr class="cf-footer-rule">
			<?php endif; ?>

			<div class="cf-footer-bottom">
				<div class="cf-footer-logo">
					<span class="cf-logo-cell">Cell</span><span class="cf-logo-fire">fire</span><span class="cf-logo-io">.io</span>
				</div>
				<div class="cf-footer-copy">
					&copy; <?= date('Y'); ?> Cellfire. All rights reserved.
				</div>
				<div class="cf-footer-legal">
					<a href="/privacy">Privacy Policy</a>
					<a href="/terms">Terms of Service</a>
				</div>
			</div>

		</div>
	</footer>

	<!-- Debug output -->
	<jdoc:include type="modules" name="debug" style="none" />
	<jdoc:include type="scripts" />


<script src="<?php echo $this->baseurl; ?>/templates/cellfire/js/pricing.js"></script>
</body>
</html>