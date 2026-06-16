<?php
/**
 * Cellfire Template — Article Layout: About
 * Assign this layout to a blank article, then set menu alias to "about"
 */
defined('_JEXEC') or die;
?>
<div class="cf-page-wrap">

	<!-- Hero -->
	<div class="cf-page-hero">
		<div class="cf-container">
			<div class="cf-page-hero-label">About</div>
			<h1>Built by RF engineers, <br><span class="cf-accent">for RF engineers</span></h1>
			<p>Cellfire was created because the tools we needed didn't exist. We built them — and now we're making them available to the entire industry.</p>
		</div>
	</div>

	<!-- Mission -->
	<div class="cf-page-section">
		<div class="cf-container" style="max-width:800px;margin:0 auto;text-align:center;">
			<div class="cf-section-label">Mission</div>
			<h2 style="font-size:clamp(26px,4vw,38px);font-weight:700;letter-spacing:-.5px;margin-bottom:20px;">
				Making cellular RF intelligence <span class="cf-accent">accessible</span>
			</h2>
			<p style="font-size:17px;color:var(--cf-text-2);line-height:1.8;">
				RF analysis has historically required expensive software licenses, proprietary hardware, and deep institutional knowledge. Cellfire changes that. We deliver professional-grade tools at a price point that works for independent engineers, small teams, and enterprise organizations alike.
			</p>
		</div>
	</div>

	<!-- Stats -->
	<div class="cf-page-section cf-page-section--alt">
		<div class="cf-container">
			<div class="cf-about-stat-grid">
				<div class="cf-about-stat cf-animate">
					<div class="cf-about-stat-value">3</div>
					<div class="cf-about-stat-label">Platform tools</div>
				</div>
				<div class="cf-about-stat cf-animate">
					<div class="cf-about-stat-value">5G</div>
					<div class="cf-about-stat-label">Ready — NR &amp; NSA support</div>
				</div>
				<div class="cf-about-stat cf-animate">
					<div class="cf-about-stat-value">∞</div>
					<div class="cf-about-stat-label">Data points you can collect</div>
				</div>
			</div>

			<div class="cf-split">
				<div class="cf-split-text">
					<div class="cf-section-label">The Platform</div>
					<h2>Three tools. <span class="cf-accent">One workflow.</span></h2>
					<p>Cellfire RF Studio handles desktop analysis and reporting. The Cellfire App handles field data collection. The Cellfire Viewer handles quick browser-based review. Together they cover the complete RF intelligence workflow — from drive test to deliverable.</p>
					<a href="/downloads" class="cf-btn cf-btn--primary" style="margin-top:8px;">Get Started</a>
				</div>
				<div class="cf-split-visual"><span style="opacity:.4;">Platform Diagram</span></div>
			</div>
		</div>
	</div>

	<!-- Values -->
	<div class="cf-page-section">
		<div class="cf-container">
			<div class="cf-section-head">
				<div class="cf-section-label">Values</div>
				<h2>What we <span class="cf-accent">stand for</span></h2>
			</div>
			<div class="cf-icon-grid cf-icon-grid--3">
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🔬</div>
					<h4>Accuracy First</h4>
					<p>We never sacrifice data fidelity for convenience. Every metric we display is sourced directly from the modem or the measurement file.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">🔒</div>
					<h4>Privacy by Design</h4>
					<p>Your RF data belongs to you. The Viewer processes everything in your browser. Studio keeps data local. Nothing is shared without your explicit action.</p>
				</div>
				<div class="cf-icon-feature cf-animate">
					<div class="cf-icon-feature-icon">⚡</div>
					<h4>Built for Speed</h4>
					<p>Field engineers don't have time to wait. Every part of Cellfire is engineered to be fast — from app launch to map render to report export.</p>
				</div>
			</div>
		</div>
	</div>

	<!-- Contact -->
	<div class="cf-cta-band">
		<div class="cf-container">
			<h2>Questions? <span class="cf-accent">Get in touch.</span></h2>
			<p>We're a small team that actually responds to emails.</p>
			<div class="cf-hero-actions">
				<a href="mailto:hello@cellfire.io" class="cf-btn cf-btn--primary">hello@cellfire.io</a>
				<a href="/pricing" class="cf-btn cf-btn--ghost">View Plans</a>
			</div>
		</div>
	</div>

	<?php if (!empty($this->item->text)) : ?>
	<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
	<?php endif; ?>

</div>
