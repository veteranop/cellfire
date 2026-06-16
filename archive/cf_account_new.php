<?php
/**
 * Cellfire Template — Article Layout: Account
 * Shows real license data from josbf_cellfire_licenses.
 * Manage Billing calls stripe.portal API (JWT or Joomla session).
 */
defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Uri\Uri;

$app    = Factory::getApplication();
$user   = $app->getIdentity();
$isGuest = $user->guest;

// Load license data for logged-in users
$license = null;
if (!$isGuest) {
    $db = Factory::getDbo();
    $license = $db->setQuery(
        $db->getQuery(true)
            ->select(['plan_type', 'is_active', 'expires_at', 'stripe_status', 'stripe_customer_id'])
            ->from('#__cellfire_licenses')
            ->where('user_id = ' . (int)$user->id)
    )->loadObject();
}

// Plan display helpers
function cfPlanLabel($plan) {
    $map = [
        'demo'               => 'Free Trial',
        'app_view'           => 'App — View Only',
        'app_full'           => 'App — Full',
        'app_team'           => 'App — Team (10 seats)',
        'studio_standard'    => 'Studio Standard',
        'studio_full'        => 'Studio Full',
        'studio_enterprise'  => 'Studio Enterprise',
        'viewer_basic'       => 'Viewer Basic',
        'viewer_pro'         => 'Viewer Pro',
        'viewer_enterprise'  => 'Viewer Enterprise',
        'bundle_starter'     => 'Bundle — Starter',
        'bundle_pro'         => 'Bundle — Professional',
        'bundle_enterprise'  => 'Bundle — Enterprise',
        'pro'                => 'Professional',
        'basic'              => 'Basic',
        'enterprise'         => 'Enterprise',
    ];
    return $map[$plan] ?? ucfirst(str_replace('_', ' ', $plan));
}
?>

<div class="cf-page-wrap">

<!-- Hero -->
<div class="cf-page-hero">
    <div class="cf-container">
        <div class="cf-page-hero-label">Account</div>
        <?php if ($isGuest): ?>
            <h1>Sign in to <span class="cf-accent">Cellfire</span></h1>
            <p>Access your subscription, manage your devices, and download your tools.</p>
            <div class="cf-page-hero-actions">
                <a href="<?= Uri::base() ?>index.php?option=com_users&view=login" class="cf-btn cf-btn--primary">Sign In</a>
                <a href="/register" class="cf-btn cf-btn--ghost">Create Account</a>
            </div>
        <?php else: ?>
            <h1>Welcome back, <span class="cf-accent"><?= htmlspecialchars($user->name) ?></span></h1>
            <p>Manage your Cellfire subscription, downloads, and account settings.</p>
        <?php endif; ?>
    </div>
</div>

<?php if ($isGuest): ?>
<div class="cf-page-section">
    <div class="cf-container" style="max-width:460px;margin:0 auto;">
        <div class="cf-account-card" style="text-align:center;">
            <h3 style="border:none;padding:0;margin-bottom:12px;">Sign in to your account</h3>
            <p style="color:var(--cf-text-2);font-size:14px;margin-bottom:24px;">Sign in to manage your Cellfire subscription and downloads.</p>
            <a href="<?= Uri::base() ?>index.php?option=com_users&view=login" class="cf-btn cf-btn--primary" style="width:100%;justify-content:center;margin-bottom:16px;">Sign In</a>
            <p style="font-size:13px;color:var(--cf-text-2);">No account? <a href="/register">Register free →</a></p>
        </div>
    </div>
</div>

<?php else: ?>
<div class="cf-page-section">
    <div class="cf-container">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;">

            <!-- Subscription card -->
            <div class="cf-account-card">
                <h3>Subscription</h3>
                <?php if ($license): ?>
                    <?php
                    $isActive  = (int)$license->is_active;
                    $planLabel = cfPlanLabel($license->plan_type);
                    $isDemo    = ($license->plan_type === 'demo');
                    $expiry    = $license->expires_at ? date('M j, Y', strtotime($license->expires_at)) : null;
                    $hasPaid   = !empty($license->stripe_customer_id);
                    ?>
                    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
                        <div>
                            <div style="font-size:20px;font-weight:700;"><?= htmlspecialchars($planLabel) ?></div>
                            <?php if ($isDemo && $expiry): ?>
                                <div style="font-size:13px;color:var(--cf-text-2);">Trial expires <?= $expiry ?></div>
                            <?php elseif ($expiry): ?>
                                <div style="font-size:13px;color:var(--cf-text-2);">Renews <?= $expiry ?></div>
                            <?php endif; ?>
                        </div>
                        <?php if ($isActive): ?>
                            <div style="background:rgba(63,185,80,.12);border:1px solid rgba(63,185,80,.3);color:#3fb950;font-size:11px;font-weight:700;letter-spacing:.5px;text-transform:uppercase;padding:4px 12px;border-radius:999px;">Active</div>
                        <?php else: ?>
                            <div style="background:rgba(248,81,73,.12);border:1px solid rgba(248,81,73,.3);color:#f85149;font-size:11px;font-weight:700;letter-spacing:.5px;text-transform:uppercase;padding:4px 12px;border-radius:999px;">Inactive</div>
                        <?php endif; ?>
                    </div>
                    <?php if ($isDemo || !$hasPaid): ?>
                        <a href="/pricing" class="cf-btn cf-btn--primary" style="font-size:13px;margin-bottom:10px;">Upgrade Plan →</a>
                    <?php else: ?>
                        <button onclick="cfOpenBilling(this)" class="cf-btn cf-btn--ghost" style="font-size:13px;cursor:pointer;background:transparent;">Manage Billing →</button>
                    <?php endif; ?>
                <?php else: ?>
                    <div style="color:var(--cf-text-2);font-size:14px;margin-bottom:20px;">No active plan found.</div>
                    <a href="/pricing" class="cf-btn cf-btn--primary" style="font-size:13px;">Get a Plan →</a>
                <?php endif; ?>
            </div>

            <!-- Account info -->
            <div class="cf-account-card">
                <h3>Account Details</h3>
                <table class="cf-req-table">
                    <tr><td>Name</td><td><?= htmlspecialchars($user->name) ?></td></tr>
                    <tr><td>Username</td><td><?= htmlspecialchars($user->username) ?></td></tr>
                    <tr><td>Email</td><td><?= htmlspecialchars($user->email) ?></td></tr>
                    <tr><td>Member since</td><td><?= date('M Y', strtotime($user->registerDate)) ?></td></tr>
                </table>
                <a href="<?= Uri::base() ?>index.php?option=com_users&view=profile&layout=edit" class="cf-btn cf-btn--ghost" style="font-size:13px;margin-top:16px;">Edit Profile →</a>
            </div>

            <!-- Quick links -->
            <div class="cf-account-card">
                <h3>Your Tools</h3>
                <div style="display:flex;flex-direction:column;gap:10px;">
                    <a href="/downloads" class="cf-btn cf-btn--ghost" style="justify-content:flex-start;gap:12px;font-size:14px;"><span>💻</span> Download RF Studio</a>
                    <a href="/app"       class="cf-btn cf-btn--ghost" style="justify-content:flex-start;gap:12px;font-size:14px;"><span>📱</span> Get the Mobile App</a>
                    <a href="https://indigo-licha-20.tiiny.site/?mode=suggestions" class="cf-btn cf-btn--ghost" style="justify-content:flex-start;gap:12px;font-size:14px;" target="_blank" rel="noopener noreferrer"><span>🌐</span> Launch Cellfire Viewer</a>
                </div>
            </div>

            <!-- Session -->
            <div class="cf-account-card">
                <h3>Session</h3>
                <p style="font-size:14px;color:var(--cf-text-2);margin-bottom:20px;">Signed in as <strong style="color:var(--cf-text);"><?= htmlspecialchars($user->username) ?></strong></p>
                <a href="<?= Uri::base() ?>index.php?option=com_users&task=user.logout&<?= \Joomla\CMS\Session\Session::getFormToken() ?>=1"
                   class="cf-btn cf-btn--ghost" style="color:var(--cf-danger);border-color:rgba(248,81,73,.3);">Sign Out</a>
            </div>

        </div>
    </div>
</div>

<!-- Billing portal toast -->
<div id="cf-billing-alert" style="display:none;position:fixed;bottom:24px;left:50%;transform:translateX(-50%);background:#1e1e1e;border:1px solid rgba(244,67,54,.4);color:#ef9a9a;padding:14px 22px;border-radius:10px;font-size:14px;z-index:9999;box-shadow:0 8px 32px rgba(0,0,0,.5);max-width:400px;text-align:center;"></div>

<script>
async function cfOpenBilling(btn) {
    var orig = btn.textContent;
    btn.textContent = 'Loading\u2026';
    btn.disabled = true;
    try {
        var resp = await fetch('/index.php?option=com_cellfireapi&task=stripe.portal&format=json');
        var data = await resp.json();
        if (data.success && data.data && data.data.url) {
            window.location.href = data.data.url;
            return;
        }
        var el = document.getElementById('cf-billing-alert');
        el.textContent = data.message || 'Billing portal unavailable. Please try again.';
        el.style.display = 'block';
        setTimeout(function(){ el.style.display='none'; }, 6000);
    } catch(e) {
        var el = document.getElementById('cf-billing-alert');
        el.textContent = 'Network error — please try again.';
        el.style.display = 'block';
        setTimeout(function(){ el.style.display='none'; }, 6000);
    }
    btn.textContent = orig;
    btn.disabled = false;
}
</script>
<?php endif; ?>

<?php if (!empty($this->item->text)): ?>
<div class="cf-page-section"><div class="cf-container"><?= $this->item->text ?></div></div>
<?php endif; ?>

</div>
