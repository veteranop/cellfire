<?php
/**
 * Cellfire Template — Article Layout: Downloads
 * Assign this layout to a blank article, then set menu alias to "downloads"
 */
defined('_JEXEC') or die;

$apk_url = 'https://github.com/veteranop/Cellfire_app_public/releases/latest/download/cellfire-app.apk';
$releases_url = 'https://github.com/veteranop/Cellfire_app_public/releases/latest';
?>
<div class="cf-page-wrap">

	<!-- Hero -->
	<div class="cf-page-hero" style="padding-bottom:48px;">
		<div class="cf-container" style="text-align:center;max-width:680px;">
			<div class="cf-page-hero-label">Android · Sideload APK</div>
			<h1>Download <span class="cf-accent">Cellfire</span></h1>
			<p style="color:var(--cf-text-2);font-size:17px;line-height:1.6;margin-bottom:36px;">
				The Cellfire mobile app collects LTE &amp; 5G signal data with GPS tagging and syncs to the cloud in real time.
				Requires an active Cellfire subscription.
			</p>

			<!-- Primary download button -->
			<a href="<?= htmlspecialchars($apk_url) ?>" class="cf-btn cf-btn--primary" style="font-size:17px;padding:14px 36px;display:inline-flex;align-items:center;gap:10px;">
				<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zm-8 2V5h2v6h1.17L12 13.17 9.83 11H11zm-6 7h14v2H5v-2z"/></svg>
				Download APK
			</a>
			<p style="margin-top:14px;font-size:12px;color:var(--cf-text-3);">
				Android 10+ &nbsp;&middot;&nbsp; ARM64 / x86_64 &nbsp;&middot;&nbsp; ~15 MB
			</p>
		</div>
	</div>

	<!-- Install instructions -->
	<div class="cf-page-section" style="padding-top:0;">
		<div class="cf-container" style="max-width:720px;">
			<div class="cf-section-label" style="text-align:center;">Install Guide</div>
			<h2 style="text-align:center;font-size:clamp(22px,3vw,32px);font-weight:700;margin-bottom:36px;">
				Three steps to get running
			</h2>

			<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:24px;">

				<div style="background:var(--cf-surface-2);border:1px solid var(--cf-border);border-radius:12px;padding:24px;">
					<div style="font-size:28px;font-weight:800;color:var(--cf-accent);margin-bottom:10px;">1</div>
					<h3 style="font-size:15px;font-weight:700;margin-bottom:8px;">Enable Unknown Sources</h3>
					<p style="font-size:13px;color:var(--cf-text-2);line-height:1.55;">
						Go to <strong>Settings &rsaquo; Apps &rsaquo; Special app access &rsaquo; Install unknown apps</strong> and allow your browser.
					</p>
				</div>

				<div style="background:var(--cf-surface-2);border:1px solid var(--cf-border);border-radius:12px;padding:24px;">
					<div style="font-size:28px;font-weight:800;color:var(--cf-accent);margin-bottom:10px;">2</div>
					<h3 style="font-size:15px;font-weight:700;margin-bottom:8px;">Download &amp; Install</h3>
					<p style="font-size:13px;color:var(--cf-text-2);line-height:1.55;">
						Tap the Download button above, open the APK from your notifications or Downloads folder, then tap <strong>Install</strong>.
					</p>
				</div>

				<div style="background:var(--cf-surface-2);border:1px solid var(--cf-border);border-radius:12px;padding:24px;">
					<div style="font-size:28px;font-weight:800;color:var(--cf-accent);margin-bottom:10px;">3</div>
					<h3 style="font-size:15px;font-weight:700;margin-bottom:8px;">Sign In</h3>
					<p style="font-size:13px;color:var(--cf-text-2);line-height:1.55;">
						Open Cellfire, sign in with your account credentials, and start collecting. A valid subscription is required.
					</p>
				</div>

			</div>

			<!-- Release notes link -->
			<p style="text-align:center;margin-top:32px;font-size:13px;color:var(--cf-text-3);">
				View full
				<a href="<?= htmlspecialchars($releases_url) ?>" target="_blank" rel="noopener" style="color:var(--cf-accent);text-decoration:none;">release notes on GitHub</a>
				&nbsp;&middot;&nbsp; Updates are delivered automatically through the app.
			</p>
		</div>
	</div>

	<!-- No subscription callout -->
	<div class="cf-cta-band">
		<div class="cf-container">
			<h2>Don&rsquo;t have a subscription yet?</h2>
			<p>Get full platform access with a Cellfire Professional plan. Free trial, no credit card required.</p>
			<div class="cf-hero-actions">
				<a href="/pricing" class="cf-btn cf-btn--primary">See Pricing</a>
				<a href="/app" class="cf-btn cf-btn--ghost">Learn About the App</a>
			</div>
		</div>
	</div>

	<?php if (!empty($this->item->text)) : ?>
	<div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
	<?php endif; ?>

</div>
