<?php
/**
 * Cellfire Template — Article Layout: Downloads
 * Assign this layout to a blank article, then set menu alias to "downloads"
 */
defined('_JEXEC') or die;
$appVersion = '1.0.1.22'; // APP_VERSION
?>
<div class="cf-page-wrap">

  <!-- Hero -->
  <div class="cf-page-hero">
    <div class="cf-container">
      <div class="cf-page-hero-label">Downloads</div>
      <h1>Get <span class="cf-accent">Cellfire</span></h1>
      <p>Download RF Studio, grab the mobile app, or launch the browser tool — everything you need to start analyzing cellular RF data today.</p>
    </div>
  </div>

  <!-- Download cards -->
  <div class="cf-page-section">
    <div class="cf-container">
      <div class="cf-dl-grid">

        <!-- RF Studio -->
        <div class="cf-dl-card">
          <div class="cf-dl-card-icon">
            <svg viewBox="0 0 40 40" fill="none" width="28" height="28" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 28 L14 18 L20 22 L26 12 L34 16" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="6" cy="28" r="2" fill="#58a6ff"/>
              <circle cx="34" cy="16" r="2" fill="#58a6ff"/>
            </svg>
          </div>
          <h3>Cellfire RF Studio</h3>
          <p>Professional desktop RF analysis software. Drive test import, heat map generation, PCI analysis, and PDF reporting.</p>
          <div style="display:flex;flex-direction:column;gap:10px;margin-top:auto;">
            <a href="https://github.com/veteranop/Cellfire_Studio_Public/releases/latest/download/CellfireRFStudio-Setup.exe" class="cf-btn cf-btn--primary" download style="justify-content:center;">
              ⬇ Download for Windows
            </a>
            <span class="cf-btn cf-btn--ghost" style="justify-content:center;opacity:.4;cursor:default;pointer-events:none;">
              ⬇ Download for macOS &nbsp;<small style="font-weight:400;font-size:.78em;">(Coming Soon)</small>
            </span>
          </div>
          <div class="cf-dl-card-meta">Version 1.0 · Requires active Cellfire plan</div>
        </div>

        <!-- Mobile App -->
        <div class="cf-dl-card">
          <div class="cf-dl-card-icon">
            <svg viewBox="0 0 40 40" fill="none" width="28" height="28" xmlns="http://www.w3.org/2000/svg">
              <rect x="11" y="4" width="18" height="32" rx="3" stroke="#58a6ff" stroke-width="2.5"/>
              <circle cx="20" cy="31" r="1.5" fill="#58a6ff"/>
              <line x1="14" y1="9" x2="26" y2="9" stroke="#58a6ff" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3>Cellfire App</h3>
          <p>Mobile RF data collection for iOS and Android. Real-time signal monitoring, GPS tagging, and cloud sync to your Cellfire account.</p>
          <div style="display:flex;flex-direction:column;gap:10px;margin-top:auto;">
            <span class="cf-store-badge" style="justify-content:center;opacity:.4;cursor:default;pointer-events:none;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/></svg>
              App Store &nbsp;<small style="font-weight:400;font-size:.78em;">(Coming Soon)</small>
            </span>
            <a href="https://github.com/veteranop/Cellfire_app_public/releases/latest/download/cellfire-release.apk" class="cf-btn cf-btn--primary" download style="justify-content:center;">
              ⬇ Download APK (Android)
            </a>
            <small style="text-align:center;color:#8b949e;"><?= $appVersion ?> &bull; Direct install &bull; Enable &ldquo;Install unknown apps&rdquo; in Android settings</small>
          </div>
          <div class="cf-dl-card-meta">iOS 15+ · Android 10+ · All plans</div>
        </div>

        <!-- Browser Viewer -->
        <div class="cf-dl-card">
          <div class="cf-dl-card-icon">
            <svg viewBox="0 0 40 40" fill="none" width="28" height="28" xmlns="http://www.w3.org/2000/svg">
              <path d="M10 14 L5 20 L10 26" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M30 14 L35 20 L30 26" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="24" y1="8" x2="16" y2="32" stroke="#58a6ff" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3>Cellfire Viewer</h3>
          <p>Browser-based tower locator and PCI analysis. No download needed — runs entirely in your browser. Upload field data and instantly see coverage maps.</p>
          <div style="margin-top:auto;">
            <a href="https://indigo-licha-20.tiiny.site/?mode=suggestions" class="cf-btn cf-btn--primary" style="justify-content:center;width:100%;" target="_blank" rel="noopener noreferrer">
              Launch in Browser →
            </a>
          </div>
          <div class="cf-dl-card-meta">No install · Works in any modern browser · Free version available</div>
        </div>

      </div>
    </div>
  </div>

  <!-- Changelog / version notes -->
  <div class="cf-page-section cf-page-section--alt">
    <div class="cf-container" style="max-width:720px;margin:0 auto;">
      <div class="cf-section-head">
        <div class="cf-section-label">Release Notes</div>
        <h2>What's <span class="cf-accent">new</span></h2>
      </div>
      <div class="cf-account-card">
        <h3>RF Studio v1.0 — Initial Release</h3>
        <ul class="cf-feature-list" style="margin-top:0;">
          <li>RSRP / RSRQ / SINR analysis engine</li>
          <li>Interactive heat map renderer</li>
          <li>Cellfire App cloud import</li>
          <li>PDF and CSV export</li>
          <li>PCI conflict detection</li>
          <li>Windows and macOS builds</li>
        </ul>
      </div>
      <!-- APP_CHANGELOG_START -->
      <div class="cf-account-card">
        <h3>Cellfire App v<?= $appVersion ?></h3>
        <ul class="cf-feature-list" style="margin-top:0;">
          <li>Real-time signal monitoring (LTE / 5G NSA / 5G SA)</li>
          <li>GPS-tagged drive test sessions</li>
          <li>Cloud sync to Cellfire account</li>
          <li>On-device heat map preview</li>
          <li>Timing advance display for LTE / 5G</li>
        </ul>
      </div>
      <!-- APP_CHANGELOG_END -->
    </div>
  </div>

  <!-- CTA -->
  <div class="cf-cta-band">
    <div class="cf-container">
      <h2>Need a <span class="cf-accent">subscription</span> first?</h2>
      <p>All downloads require an active Cellfire plan. Get started from $4.99/month.</p>
      <div class="cf-hero-actions">
        <a href="/pricing" class="cf-btn cf-btn--primary">View Plans</a>
        <a href="/about" class="cf-btn cf-btn--ghost">Learn More</a>
      </div>
    </div>
  </div>

  <?php if (!empty($this->item->text)) : ?>
  <div class="cf-page-section"><div class="cf-container"><?= $this->item->text; ?></div></div>
  <?php endif; ?>

</div>