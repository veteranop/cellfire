<?php
/**
 * Cellfire Template — Article Layout: Pricing
 * Checkout via com_cellfireapi stripe.createCheckout (server-side Stripe session).
 * Free Demo / Viewer Free plans link directly to /downloads or the Viewer URL.
 */
defined('_JEXEC') or die;

/* ── Per-product plans ───────────────────────────────────────────────────── */
$products = [

    /* ── APP ─────────────────────────────────────────────────────────────── */
    'app' => [
        'label' => 'Cellfire App',
        'sub'   => 'Android · Deep cellular signal scanning',
        'plans' => [
            [
                'name'       => 'Free Demo',
                'badge'      => '',
                'price'      => '0',
                'period'     => '',
                'price_note' => '30-day trial',
                'cta_url'    => '/downloads',
                'cta'        => 'Download Free',
                'popular'    => false,
                'features'   => [
                    '✅ 30 days full-featured',
                    '✅ Deep Scan — all carriers & bands',
                    '✅ Real-time signal map',
                    '✅ Full cell detail panel (RSRP/SINR/RSRQ/PCI/ARFCN)',
                    '✅ Rolling live signal graph',
                    '❌ No data export',
                    '❌ Expires after 30 days',
                    '📱 1 device',
                ],
            ],
            [
                'name'       => 'View Only',
                'badge'      => '',
                'price'      => '5',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeaXA6O9KKvnlYnx5OZxN3',
                'cta'        => 'Get View Only',
                'popular'    => false,
                'features'   => [
                    '✅ No expiration',
                    '✅ Live signal viewing (all carriers)',
                    '✅ Cell detail panel',
                    '✅ Signal map (view only)',
                    '❌ No logging',
                    '❌ No GPS drive-test recording',
                    '❌ No export',
                    '📱 1 seat',
                ],
            ],
            [
                'name'       => 'Full',
                'badge'      => 'Most Popular',
                'price'      => '10',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeaYA6O9KKvnlYthwiZkCj',
                'cta'        => 'Get Full Access',
                'popular'    => true,
                'features'   => [
                    '✅ Everything in View Only',
                    '✅ Full data logging',
                    '✅ GPS drive-test mapping',
                    '✅ PCI discovery (all carriers)',
                    '✅ CSV / KML export',
                    '✅ RF Studio sync-ready export',
                    '✅ Scan history',
                    '📱 1 seat',
                ],
            ],
            [
                'name'       => 'Full — 10 Seats',
                'badge'      => 'Team',
                'price'      => '79',
                'period'     => '/mo',
                'price_note' => 'up to 10 devices',
                'price_id'   => 'price_1TEeaZA6O9KKvnlYDYSGf5XJ',
                'cta'        => 'Get Team Access',
                'popular'    => false,
                'features'   => [
                    '✅ Everything in Full',
                    '✅ 10 seats / devices',
                    '✅ Centralized scan exports',
                    '✅ Team account dashboard',
                    '✅ Priority support',
                    '✅ RF Studio sync-ready export',
                    '✅ Bulk PCI reporting',
                    '📱 10 seats',
                ],
            ],
        ],
    ],

    /* ── STUDIO ──────────────────────────────────────────────────────────── */
    'studio' => [
        'label' => 'RF Studio',
        'sub'   => 'Windows & macOS · Professional RF propagation & coverage planning',
        'plans' => [
            [
                'name'       => 'Free Demo',
                'badge'      => '',
                'price'      => '0',
                'period'     => '',
                'price_note' => '30-day trial',
                'cta_url'    => '/downloads',
                'cta'        => 'Download Free',
                'popular'    => false,
                'features'   => [
                    '✅ 30 days — all features unlocked',
                    '✅ ITM / Longley-Rice propagation',
                    '✅ SRTM terrain data',
                    '✅ All technologies (FM/AM/P25/HAM…)',
                    '✅ AI Smart Import (Ollama)',
                    '✅ RF Chain Builder',
                    '❌ No export / no save',
                    '❌ Expires after 30 days',
                ],
            ],
            [
                'name'       => 'Standard',
                'badge'      => '',
                'price'      => '25',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeaZA6O9KKvnlYeVS1FtHm',
                'cta'        => 'Get Standard',
                'popular'    => false,
                'features'   => [
                    '✅ HAM / Amateur Radio analysis',
                    '✅ Meshtastic / LoRa (433/868/915 MHz)',
                    '✅ ITM / Longley-Rice propagation',
                    '✅ SRTM terrain data',
                    '✅ CSV / KMZ export',
                    '⚠️ 20-mile maximum analysis range',
                    '⚠️ Default antenna patterns only',
                    '❌ No AI Smart Import',
                    '❌ No Station Builder',
                    '❌ No FM / AM / TV / P25 / LTE',
                    '💻 1 seat',
                ],
            ],
            [
                'name'       => 'Full',
                'badge'      => 'Most Popular',
                'price'      => '60',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeaaA6O9KKvnlYbH3owXyc',
                'cta'        => 'Get Full Access',
                'popular'    => true,
                'features'   => [
                    '✅ All technologies (FM, AM, TV/DTV, P25, HAM, Meshtastic, LTE, EmComm)',
                    '✅ 🤖 AI Smart Import — drop any vendor PDF',
                    '✅ Station Builder — full RF chain editor',
                    '✅ Manufacturer catalogs (Nautel, BW Broadcast, Yaesu, L3 Harris…)',
                    '✅ Custom antenna patterns',
                    '✅ Unlimited analysis range',
                    '✅ PDF / CSV / KMZ export',
                    '✅ FCC-compliant coverage reports',
                    '✅ Offline terrain tile caching',
                    '💻 1 seat',
                ],
            ],
            [
                'name'       => 'Enterprise',
                'badge'      => 'Team',
                'price'      => '199',
                'period'     => '/mo',
                'price_note' => '10+ seats',
                'price_id'   => 'price_1TEeabA6O9KKvnlYAxPYbORu',
                'cta'        => 'Get Enterprise',
                'popular'    => false,
                'features'   => [
                    '✅ Everything in Full',
                    '✅ 10+ concurrent seats',
                    '✅ Site / organization licensing',
                    '✅ Shared component catalogs',
                    '✅ Centralized project storage',
                    '✅ Priority support & onboarding',
                    '✅ Custom integration support',
                    '💻 10+ seats',
                ],
            ],
        ],
    ],

    /* ── VIEWER ──────────────────────────────────────────────────────────── */
    'viewer' => [
        'label' => 'Cellfire Viewer',
        'sub'   => 'Browser-based · No install · Upload & visualize cellular field data',
        'plans' => [
            [
                'name'       => 'Free',
                'badge'      => '',
                'price'      => '0',
                'period'     => '',
                'price_note' => 'always free',
                'cta_url'    => 'https://indigo-licha-20.tiiny.site/?mode=suggestions',
                'cta'        => 'Launch Viewer',
                'popular'    => false,
                'features'   => [
                    '✅ 5 CSV uploads / day',
                    '✅ Interactive RSRP map',
                    '✅ PCI cluster visualization',
                    '✅ Cell detail panel',
                    '✅ Zero data retention',
                    '❌ No export',
                    '🌐 Personal use',
                ],
            ],
            [
                'name'       => 'Basic',
                'badge'      => '',
                'price'      => '5',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeacA6O9KKvnlYd6bOXTXG',
                'cta'        => 'Get Basic',
                'popular'    => false,
                'features'   => [
                    '✅ 20 CSV uploads / day',
                    '✅ Export enabled (PNG / CSV)',
                    '✅ Interactive RSRP map',
                    '✅ PCI cluster visualization',
                    '✅ Cell detail panel',
                    '✅ Zero data retention',
                    '🌐 1 user',
                ],
            ],
            [
                'name'       => 'Pro',
                'badge'      => 'Most Popular',
                'price'      => '15',
                'period'     => '/mo',
                'price_note' => 'billed monthly',
                'price_id'   => 'price_1TEeadA6O9KKvnlYGw7cmFqw',
                'cta'        => 'Get Pro',
                'popular'    => true,
                'features'   => [
                    '✅ 500 CSV uploads / day',
                    '✅ Full export (PNG / CSV / KML)',
                    '✅ Interactive RSRP map',
                    '✅ PCI cluster visualization',
                    '✅ Cell detail panel',
                    '✅ Priority processing',
                    '✅ Zero data retention',
                    '🌐 1 user',
                ],
            ],
            [
                'name'       => 'Enterprise',
                'badge'      => 'Team',
                'price'      => '45',
                'period'     => '/mo',
                'price_note' => '10+ seats',
                'price_id'   => 'price_1TEeadA6O9KKvnlYRTNmGErt',
                'cta'        => 'Get Enterprise',
                'popular'    => false,
                'features'   => [
                    '✅ Unlimited CSV / day',
                    '✅ Full export (PNG / CSV / KML)',
                    '✅ Interactive RSRP map',
                    '✅ PCI cluster visualization',
                    '✅ Team management dashboard',
                    '✅ Priority support',
                    '✅ Zero data retention',
                    '🌐 10+ seats',
                ],
            ],
        ],
    ],
];

/* ── Bundle packages ─────────────────────────────────────────────────────── */
$bundles = [
    [
        'name'     => 'Package 1',
        'sub'      => 'Essential Bundle',
        'price'    => '30',
        'period'   => '/mo',
        'save'     => 'Save $5/mo',
        'color'    => 'var(--cf-text-2)',
        'includes' => [
            ['product' => 'Cellfire App', 'tier' => 'View Only', 'icon' => '📱'],
            ['product' => 'RF Studio',    'tier' => 'Standard',  'icon' => '🖥️'],
            ['product' => 'Viewer',       'tier' => 'Basic',     'icon' => '🌐'],
        ],
        'features' => [
            'App: live viewing (1 device)',
            'Studio: HAM + Meshtastic, 20-mile range',
            'Viewer: 20 CSVs/day + export',
            'Single login across all products',
        ],
        'price_id' => 'price_1TEeaeA6O9KKvnlY5bEzodSi',
        'popular'  => false,
    ],
    [
        'name'     => 'Package 2',
        'sub'      => 'Professional Bundle',
        'price'    => '69',
        'period'   => '/mo',
        'save'     => 'Save $16/mo',
        'color'    => 'var(--cf-accent)',
        'includes' => [
            ['product' => 'Cellfire App', 'tier' => 'Full',    'icon' => '📱'],
            ['product' => 'RF Studio',    'tier' => 'Full',    'icon' => '🖥️'],
            ['product' => 'Viewer',       'tier' => 'Pro',     'icon' => '🌐'],
        ],
        'features' => [
            'App: full logging, GPS drive-test, export',
            'Studio: all tech, AI Import, Station Builder',
            'Viewer: 500 CSVs/day + full export',
            'Single login — 1 seat each product',
        ],
        'price_id' => 'price_1TEeafA6O9KKvnlYin6OGuIE',
        'popular'  => true,
    ],
    [
        'name'     => 'Package 3',
        'sub'      => 'Enterprise Bundle',
        'price'    => '299',
        'period'   => '/mo',
        'save'     => 'Save $24/mo',
        'color'    => '#a78bfa',
        'includes' => [
            ['product' => 'Cellfire App', 'tier' => 'Full — 10 Seats',  'icon' => '📱'],
            ['product' => 'RF Studio',    'tier' => 'Enterprise',        'icon' => '🖥️'],
            ['product' => 'Viewer',       'tier' => 'Enterprise',        'icon' => '🌐'],
        ],
        'features' => [
            'App: 10 seats, full logging + export',
            'Studio: 10+ seats, all tech, AI Import',
            'Viewer: unlimited CSVs, full export, 10+ seats',
            'Shared account management + priority support',
        ],
        'price_id' => 'price_1TEeagA6O9KKvnlYIp25hc99',
        'popular'  => false,
    ],
];
?>

<style>
/* ═══════════════════════════════════════════════════════════════════════════
   PRICING PAGE — Cellfire
═══════════════════════════════════════════════════════════════════════════ */
.cf-pricing-tabs { display:flex; gap:8px; justify-content:center; flex-wrap:wrap; margin-bottom:48px; }
.cf-pricing-tab-btn { background:var(--cf-bg-2); border:1px solid var(--cf-border); color:var(--cf-text-2); padding:10px 24px; border-radius:99px; font-size:14px; font-weight:600; cursor:pointer; transition:background .2s,color .2s,border-color .2s; font-family:inherit; }
.cf-pricing-tab-btn:hover { background:var(--cf-bg-3); color:var(--cf-text-1); }
.cf-pricing-tab-btn.active { background:var(--cf-accent); border-color:var(--cf-accent); color:#000; }
.cf-pricing-panel { display:none; }
.cf-pricing-panel.active { display:block; }
.cf-pricing-panel-head { text-align:center; margin-bottom:40px; }
.cf-pricing-panel-head h2 { margin:0 0 8px; font-size:30px; }
.cf-pricing-panel-head p  { color:var(--cf-text-2); margin:0; font-size:15px; }
.cf-price-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:20px; align-items:start; }
.cf-price-card { background:var(--cf-bg-2); border:1px solid var(--cf-border); border-radius:var(--cf-r-xl); padding:28px 24px 24px; position:relative; display:flex; flex-direction:column; transition:border-color .25s,transform .2s; }
.cf-price-card:hover { border-color:rgba(88,166,255,.4); transform:translateY(-3px); }
.cf-price-card--popular { border-color:var(--cf-accent); background:var(--cf-bg-3); }
.cf-price-popular-badge { position:absolute; top:-13px; left:50%; transform:translateX(-50%); background:var(--cf-accent); color:#000; font-size:11px; font-weight:700; letter-spacing:.06em; text-transform:uppercase; padding:4px 14px; border-radius:99px; white-space:nowrap; }
.cf-price-team-badge { position:absolute; top:-13px; left:50%; transform:translateX(-50%); background:rgba(167,139,250,.2); border:1px solid rgba(167,139,250,.5); color:#a78bfa; font-size:11px; font-weight:700; letter-spacing:.06em; text-transform:uppercase; padding:4px 14px; border-radius:99px; white-space:nowrap; }
.cf-price-card-name { font-size:16px; font-weight:700; color:var(--cf-text-1); margin:0 0 16px; }
.cf-price-amount-row { display:flex; align-items:flex-end; gap:3px; margin-bottom:4px; }
.cf-price-dollar { font-size:18px; font-weight:700; color:var(--cf-text-2); line-height:1; padding-bottom:4px; }
.cf-price-number { font-size:42px; font-weight:800; color:var(--cf-text-1); line-height:1; }
.cf-price-period { font-size:15px; color:var(--cf-text-2); padding-bottom:6px; }
.cf-price-note { font-size:12px; color:var(--cf-text-3); margin-bottom:20px; }
.cf-price-cta { display:block; text-align:center; padding:11px 16px; border-radius:var(--cf-r-md); font-size:14px; font-weight:700; text-decoration:none; margin-bottom:22px; transition:opacity .2s,transform .15s; border:1px solid transparent; cursor:pointer; font-family:inherit; width:100%; box-sizing:border-box; }
.cf-price-cta:hover { opacity:.88; transform:translateY(-1px); }
.cf-price-cta--primary { background:var(--cf-accent); color:#000; }
.cf-price-cta--ghost { background:transparent; border-color:var(--cf-border); color:var(--cf-text-1); }
.cf-price-cta:disabled { opacity:.6; cursor:wait; transform:none; }
.cf-price-divider { height:1px; background:var(--cf-border); margin-bottom:18px; }
.cf-price-features { list-style:none; margin:0; padding:0; flex:1; }
.cf-price-features li { font-size:13px; color:var(--cf-text-2); padding:5px 0; display:flex; align-items:flex-start; gap:6px; line-height:1.4; }
.cf-price-features li:first-child { padding-top:0; }
/* Bundle */
.cf-bundle-intro { text-align:center; margin-bottom:44px; }
.cf-bundle-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:24px; align-items:start; }
.cf-bundle-card { background:var(--cf-bg-2); border:1px solid var(--cf-border); border-radius:var(--cf-r-xl); padding:30px 24px 24px; position:relative; display:flex; flex-direction:column; transition:border-color .25s,transform .2s; }
.cf-bundle-card:hover { transform:translateY(-3px); border-color:rgba(88,166,255,.4); }
.cf-bundle-card--popular { border-color:var(--cf-accent); background:var(--cf-bg-3); }
.cf-bundle-popular-badge { position:absolute; top:-13px; left:50%; transform:translateX(-50%); background:var(--cf-accent); color:#000; font-size:11px; font-weight:700; letter-spacing:.06em; text-transform:uppercase; padding:4px 14px; border-radius:99px; white-space:nowrap; }
.cf-bundle-name { font-size:13px; font-weight:700; letter-spacing:.08em; text-transform:uppercase; color:var(--cf-accent); margin-bottom:4px; }
.cf-bundle-sub  { font-size:18px; font-weight:700; color:var(--cf-text-1); margin-bottom:16px; }
.cf-bundle-price-row { display:flex; align-items:flex-end; gap:3px; margin-bottom:4px; }
.cf-bundle-dollar { font-size:16px; font-weight:700; color:var(--cf-text-2); line-height:1; padding-bottom:4px; }
.cf-bundle-number { font-size:38px; font-weight:800; color:var(--cf-text-1); line-height:1; }
.cf-bundle-period { font-size:14px; color:var(--cf-text-2); padding-bottom:5px; }
.cf-bundle-save   { font-size:12px; color:#34d399; font-weight:700; margin-bottom:20px; }
.cf-bundle-includes { display:flex; flex-direction:column; gap:8px; margin-bottom:20px; padding:14px; background:var(--cf-bg-1); border-radius:var(--cf-r-md); border:1px solid var(--cf-border); }
.cf-bundle-include-row { display:flex; align-items:center; gap:8px; font-size:13px; }
.cf-bundle-include-icon { font-size:15px; flex-shrink:0; }
.cf-bundle-include-prod { color:var(--cf-text-2); min-width:90px; }
.cf-bundle-include-tier { color:var(--cf-text-1); font-weight:600; }
.cf-bundle-features { list-style:none; margin:0 0 22px; padding:0; }
.cf-bundle-features li { font-size:13px; color:var(--cf-text-2); padding:4px 0; padding-left:18px; position:relative; }
.cf-bundle-features li::before { content:'✓'; position:absolute; left:0; color:#34d399; font-weight:700; }
.cf-bundle-cta { display:block; text-align:center; padding:12px; border-radius:var(--cf-r-md); font-size:14px; font-weight:700; text-decoration:none; transition:opacity .2s,transform .15s; border:1px solid transparent; margin-top:auto; cursor:pointer; font-family:inherit; width:100%; box-sizing:border-box; }
.cf-bundle-cta:hover { opacity:.88; transform:translateY(-1px); }
.cf-bundle-cta--primary { background:var(--cf-accent); color:#000; }
.cf-bundle-cta--ghost   { background:transparent; border-color:var(--cf-border); color:var(--cf-text-1); }
.cf-bundle-cta:disabled { opacity:.6; cursor:wait; transform:none; }
/* FAQ */
.cf-faq-list { max-width:720px; margin:0 auto; }
.cf-faq-item { border-bottom:1px solid var(--cf-border); padding:20px 0; }
.cf-faq-item:last-child { border-bottom:none; }
.cf-faq-q { font-size:15px; font-weight:700; color:var(--cf-text-1); cursor:pointer; display:flex; justify-content:space-between; align-items:center; gap:12px; user-select:none; }
.cf-faq-q .cf-faq-arrow { color:var(--cf-text-3); font-size:18px; flex-shrink:0; transition:transform .2s; }
.cf-faq-item.open .cf-faq-arrow { transform:rotate(180deg); }
.cf-faq-a { display:none; font-size:14px; color:var(--cf-text-2); padding-top:12px; line-height:1.65; }
.cf-faq-item.open .cf-faq-a { display:block; }
/* Checkout alert */
#cf-checkout-alert { display:none; position:fixed; bottom:24px; left:50%; transform:translateX(-50%); background:#1e1e1e; border:1px solid rgba(244,67,54,.4); color:#ef9a9a; padding:14px 22px; border-radius:10px; font-size:14px; z-index:9999; box-shadow:0 8px 32px rgba(0,0,0,.5); max-width:420px; text-align:center; }
/* Responsive */
@media (max-width:1200px) { .cf-price-grid { grid-template-columns:repeat(2,1fr); } }
@media (max-width:860px)  { .cf-bundle-grid { grid-template-columns:1fr; } }
@media (max-width:640px)  { .cf-price-grid { grid-template-columns:1fr; } .cf-price-number { font-size:36px; } }
</style>

<div class="cf-page-wrap">

<!-- ═══ HERO ══════════════════════════════════════════════════════════════ -->
<div class="cf-page-hero">
	<div class="cf-container">
		<div class="cf-page-hero-label">Pricing</div>
		<h1>Simple, transparent <span class="cf-accent">pricing</span></h1>
		<p style="max-width:560px;margin:0 auto 24px;">Pick the product and tier that fits your work. No hidden fees, no cloud lock-in — all plans include a 30-day free trial. Mix and match or save with a bundle.</p>
		<div style="display:flex;gap:12px;justify-content:center;flex-wrap:wrap;font-size:13px;color:var(--cf-text-2);">
			<span>✅ 30-day free trial on paid plans</span>
			<span>✅ Cancel anytime</span>
			<span>✅ Your data never leaves your device</span>
		</div>
	</div>
</div>

<!-- ═══ PRODUCT TABS + CARDS ══════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">

		<div class="cf-pricing-tabs" id="cfPricingTabs">
			<button class="cf-pricing-tab-btn active" data-panel="app">📱 Cellfire App</button>
			<button class="cf-pricing-tab-btn" data-panel="studio">🖥️ RF Studio</button>
			<button class="cf-pricing-tab-btn" data-panel="viewer">🌐 Viewer</button>
		</div>

		<?php foreach ($products as $key => $product): ?>
		<div class="cf-pricing-panel <?= $key === 'app' ? 'active' : '' ?>" id="cf-panel-<?= $key ?>">
			<div class="cf-pricing-panel-head">
				<h2><?= $product['label'] ?></h2>
				<p><?= $product['sub'] ?></p>
			</div>
			<div class="cf-price-grid">
			<?php foreach ($product['plans'] as $plan): ?>
				<div class="cf-price-card <?= $plan['popular'] ? 'cf-price-card--popular' : '' ?>">
					<?php if ($plan['badge'] === 'Most Popular'): ?><div class="cf-price-popular-badge">Most Popular</div>
					<?php elseif ($plan['badge'] === 'Team'): ?><div class="cf-price-team-badge">Team</div>
					<?php endif; ?>
					<div class="cf-price-card-name"><?= $plan['name'] ?></div>
					<div class="cf-price-amount-row">
						<?php if ($plan['price'] !== '0'): ?>
							<span class="cf-price-dollar">$</span>
							<span class="cf-price-number"><?= $plan['price'] ?></span>
							<span class="cf-price-period"><?= $plan['period'] ?></span>
						<?php else: ?>
							<span class="cf-price-number" style="font-size:32px;">Free</span>
						<?php endif; ?>
					</div>
					<div class="cf-price-note"><?= $plan['price_note'] ?></div>
					<?php if (!empty($plan['price_id'])): ?>
						<button onclick="cfCheckout(this,'<?= $plan['price_id'] ?>')"
						        class="cf-price-cta <?= $plan['popular'] ? 'cf-price-cta--primary' : 'cf-price-cta--ghost' ?>">
							<?= $plan['cta'] ?>
						</button>
					<?php else: ?>
						<a href="<?= $plan['cta_url'] ?>" class="cf-price-cta cf-price-cta--ghost"><?= $plan['cta'] ?></a>
					<?php endif; ?>
					<div class="cf-price-divider"></div>
					<ul class="cf-price-features">
						<?php foreach ($plan['features'] as $feature): ?><li><?= htmlspecialchars($feature) ?></li><?php endforeach; ?>
					</ul>
				</div>
			<?php endforeach; ?>
			</div>
		</div>
		<?php endforeach; ?>

	</div>
</div>

<!-- ═══ BUNDLE PACKAGES ═══════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-bundle-intro">
			<div class="cf-section-label">Bundle &amp; Save</div>
			<h2>Get all three products — <span class="cf-accent">at a discount</span></h2>
			<p style="color:var(--cf-text-2);max-width:520px;margin:0 auto;">
				Cellfire is designed as an end-to-end workflow: scan in the field with the App, model with RF Studio, share results with the Viewer. Buy them together and save every month.
			</p>
		</div>
		<div class="cf-bundle-grid">
		<?php foreach ($bundles as $bundle): ?>
			<div class="cf-bundle-card <?= $bundle['popular'] ? 'cf-bundle-card--popular' : '' ?>">
				<?php if ($bundle['popular']): ?><div class="cf-bundle-popular-badge">Best Value</div><?php endif; ?>
				<div class="cf-bundle-name"><?= $bundle['name'] ?></div>
				<div class="cf-bundle-sub"><?= $bundle['sub'] ?></div>
				<div class="cf-bundle-price-row">
					<span class="cf-bundle-dollar">$</span>
					<span class="cf-bundle-number"><?= $bundle['price'] ?></span>
					<span class="cf-bundle-period"><?= $bundle['period'] ?></span>
				</div>
				<div class="cf-bundle-save"><?= $bundle['save'] ?></div>
				<div class="cf-bundle-includes">
					<?php foreach ($bundle['includes'] as $inc): ?>
					<div class="cf-bundle-include-row">
						<span class="cf-bundle-include-icon"><?= $inc['icon'] ?></span>
						<span class="cf-bundle-include-prod"><?= $inc['product'] ?></span>
						<span class="cf-bundle-include-tier"><?= $inc['tier'] ?></span>
					</div>
					<?php endforeach; ?>
				</div>
				<ul class="cf-bundle-features">
					<?php foreach ($bundle['features'] as $f): ?><li><?= htmlspecialchars($f) ?></li><?php endforeach; ?>
				</ul>
				<button onclick="cfCheckout(this,'<?= $bundle['price_id'] ?>')"
				        class="cf-bundle-cta <?= $bundle['popular'] ? 'cf-bundle-cta--primary' : 'cf-bundle-cta--ghost' ?>">
					<?= $bundle['popular'] ? 'Get Package 2 →' : 'Get ' . $bundle['name'] . ' →' ?>
				</button>
			</div>
		<?php endforeach; ?>
		</div>
	</div>
</div>

<!-- ═══ COMPARE TABLE ═════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container">
		<div class="cf-section-head" style="text-align:center;margin-bottom:40px;">
			<div class="cf-section-label">Compare</div>
			<h2>Key restrictions <span class="cf-accent">at a glance</span></h2>
		</div>
		<div style="overflow-x:auto;">
		<table style="width:100%;border-collapse:collapse;font-size:13px;min-width:600px;">
			<thead>
				<tr style="border-bottom:2px solid var(--cf-accent);">
					<th style="text-align:left;padding:10px 14px;color:var(--cf-text-2);font-weight:600;">Feature</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">App Free</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">App Full</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">Studio Std</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">Studio Full</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">Viewer Free</th>
					<th style="text-align:center;padding:10px 14px;color:var(--cf-text-1);">Viewer Pro</th>
				</tr>
			</thead>
			<tbody>
				<?php
				$rows = [
					['Export / Save',         '❌','✅','✅','✅','❌','✅'],
					['Unlimited duration',     '❌','✅','✅','✅','✅','✅'],
					['AI Smart Import',        '—', '—', '❌','✅','—', '—' ],
					['All technologies',       '—', '—', '❌','✅','—', '—' ],
					['Max range',              '—', '—', '20 mi','Unlimited','—','—'],
					['Station Builder',        '—', '—', '❌','✅','—', '—' ],
					['CSV uploads/day',        '—', '—', '—', '—', '5',  '500'],
					['GPS drive-test logging', '✅','✅','—', '—', '—', '—' ],
					['Multi-seat',             '❌','❌','❌','❌','❌','❌'],
				];
				foreach ($rows as $i => $row):
					$bg = $i % 2 === 0 ? 'background:var(--cf-bg-2)' : '';
				?>
				<tr style="border-bottom:1px solid var(--cf-border);<?= $bg ?>">
					<td style="padding:10px 14px;color:var(--cf-text-2);font-weight:500;"><?= $row[0] ?></td>
					<?php for ($c = 1; $c < count($row); $c++): ?>
					<td style="padding:10px 14px;text-align:center;color:<?= $row[$c]==='✅'?'#34d399':($row[$c]==='❌'?'#f87171':'var(--cf-text-2)') ?>;">
						<?= $row[$c] ?>
					</td>
					<?php endfor; ?>
				</tr>
				<?php endforeach; ?>
			</tbody>
		</table>
		</div>
		<p style="font-size:12px;color:var(--cf-text-3);margin-top:12px;text-align:center;">
			Multi-seat plans available on App Full (10-seat), Studio Enterprise, and Viewer Enterprise tiers.
		</p>
	</div>
</div>

<!-- ═══ FAQ ═══════════════════════════════════════════════════════════════ -->
<div class="cf-page-section cf-page-section--alt">
	<div class="cf-container">
		<div class="cf-section-head" style="text-align:center;margin-bottom:40px;">
			<div class="cf-section-label">FAQ</div>
			<h2>Common <span class="cf-accent">questions</span></h2>
		</div>
		<div class="cf-faq-list" id="cfFaqList">
			<div class="cf-faq-item open">
				<div class="cf-faq-q">Does the app upload my data anywhere?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">No. Cellfire never uploads your scan data. All signal readings, GPS tracks, and CSV exports stay on your device. The only network communication is license verification — one lightweight ping to confirm your subscription is active.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">What's the difference between Studio Standard and Studio Full?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a"><strong>Standard</strong> is limited to HAM and Meshtastic/LoRa analysis, has a 20-mile range limit, and excludes AI Smart Import and Station Builder.<br><br><strong>Full</strong> unlocks all technologies (FM, AM, TV/DTV, P25, LTE, EmComm), removes range limits, adds AI Smart Import and the Station Builder with manufacturer catalogs.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">How does the AI Smart Import work?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">AI Smart Import runs entirely locally using <strong>Ollama</strong>. Install Ollama once, pull a model (e.g. Llama 3), and RF Studio sends vendor PDFs to it locally. Nothing leaves your machine — no API key, no cloud subscription.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">Can I use the same account on the Android app and RF Studio?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">Yes. Your Cellfire account works across all products. Log in with your username and password — each product verifies your subscription tier and activates the appropriate features automatically.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">What happens when my 30-day free trial ends?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">The product drops to its free tier (App: view-only; Studio: no export/save; Viewer: 5 CSV/day). Your data is never deleted — you lose access to paid features until you subscribe.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">Can I cancel my subscription anytime?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">Yes. Cancel anytime from your account page. Paid features stay active until the end of the billing period, then roll to the free tier. No cancellation fees.</div>
			</div>
			<div class="cf-faq-item">
				<div class="cf-faq-q">Do bundle packages require buying all three products?<span class="cf-faq-arrow">▾</span></div>
				<div class="cf-faq-a">Yes. If you only need one or two products, subscribe individually — bundles are the best value when you use all three.</div>
			</div>
		</div>
	</div>
</div>

<!-- ═══ BOTTOM CTA ════════════════════════════════════════════════════════ -->
<div class="cf-page-section">
	<div class="cf-container" style="text-align:center;max-width:600px;">
		<div class="cf-section-label">Get Started</div>
		<h2>Not sure which plan is right<span class="cf-accent"> for you?</span></h2>
		<p style="color:var(--cf-text-2);margin-bottom:32px;">
			Every paid plan starts with a free trial. Download the app or RF Studio for free and try everything — no credit card needed for the trial period.
		</p>
		<div class="cf-page-hero-actions" style="justify-content:center;">
			<a href="/downloads" class="cf-btn cf-btn--primary">Download Free →</a>
			<a href="/contact"   class="cf-btn cf-btn--ghost">Talk to Us</a>
		</div>
	</div>
</div>

</div><!-- .cf-page-wrap -->

<div id="cf-checkout-alert"></div>

<script>
(function(){
	/* ── Tab switcher ─────────────────────────────────────────────────── */
	var tabs = document.querySelectorAll('.cf-pricing-tab-btn');
	tabs.forEach(function(btn){
		btn.addEventListener('click', function(){
			tabs.forEach(function(b){ b.classList.remove('active'); });
			document.querySelectorAll('.cf-pricing-panel').forEach(function(p){ p.classList.remove('active'); });
			btn.classList.add('active');
			var panel = document.getElementById('cf-panel-' + btn.dataset.panel);
			if (panel) panel.classList.add('active');
		});
	});
	/* ── FAQ accordion ────────────────────────────────────────────────── */
	document.querySelectorAll('.cf-faq-q').forEach(function(q){
		q.addEventListener('click', function(){
			var item = q.closest('.cf-faq-item');
			var wasOpen = item.classList.contains('open');
			document.querySelectorAll('.cf-faq-item').forEach(function(i){ i.classList.remove('open'); });
			if (!wasOpen) item.classList.add('open');
		});
	});
})();

function cfShowAlert(msg) {
	var el = document.getElementById('cf-checkout-alert');
	el.textContent = msg;
	el.style.display = 'block';
	setTimeout(function(){ el.style.display = 'none'; }, 6000);
}

async function cfCheckout(btn, priceId) {
	var original = btn.textContent;
	btn.textContent = 'Loading\u2026';
	btn.disabled = true;
	try {
		var resp = await fetch(
			'/index.php?option=com_cellfireapi&task=stripe.createCheckout&format=json',
			{ method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({price_id:priceId}) }
		);
		var data = await resp.json();
		if (data.success && data.data && data.data.url) {
			window.location.href = data.data.url;
			return;
		}
		cfShowAlert(data.message || 'Checkout unavailable. Please try again shortly.');
	} catch(e) {
		cfShowAlert('Network error — please check your connection and try again.');
	}
	btn.textContent = original;
	btn.disabled = false;
}
</script>
