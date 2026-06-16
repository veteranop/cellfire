<?php
/**
 * Cellfire Email Verification
 * Standalone — no Joomla bootstrap needed.
 * URL: https://cellfire.io/verify_email.php?token=<hex>
 */

define('DB_HOST', 'localhost');
define('DB_NAME', 'veterap2_joom819');
define('DB_USER', 'veterap2_joom819');
define('DB_PASS', 'B1S!wp4K)6');
define('TBL_PFX', 'josbf_');

function page(string $title, string $icon, string $heading, string $body, bool $success = true): void
{
    $accentHex = '#FF6B1A';
    $iconColor = $success ? $accentHex : '#EB5757';
    ?>
<!DOCTYPE html>
<html lang="en">
<head><meta charset="utf-8">
  
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><?= htmlspecialchars($title) ?> — Cellfire</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0d0d0d; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
           min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; }
    .card { background: #1a1a1a; border-radius: 16px; max-width: 420px; width: 100%;
            padding: 40px 32px; text-align: center; box-shadow: 0 8px 32px rgba(0,0,0,0.5); }
    .brand { color: #FF6B1A; font-size: 22px; font-weight: 800; letter-spacing: 1px; margin-bottom: 32px; }
    .icon  { font-size: 56px; margin-bottom: 20px; }
    h1     { color: #fff; font-size: 22px; font-weight: 700; margin-bottom: 12px; }
    p      { color: #aaa; font-size: 15px; line-height: 1.6; margin-bottom: 8px; }
    .btn   { display: inline-block; margin-top: 28px; background: #FF6B1A; color: #000;
             font-weight: 700; font-size: 15px; text-decoration: none;
             padding: 14px 32px; border-radius: 8px; }
    .sub   { color: #555; font-size: 12px; margin-top: 20px; }
  </style>
</head>
<body>
  <div class="card">
    <div class="brand">CELLFIRE</div>
    <div class="icon"><?= $icon ?></div>
    <h1><?= htmlspecialchars($heading) ?></h1>
    <?= $body ?>
  </div>
</body>
</html>
<?php
    exit;
}

// ── Validate token ────────────────────────────────────────────────────────────
$token = trim($_GET['token'] ?? '');

if (!$token || !preg_match('/^[a-f0-9]{64}$/', $token)) {
    page(
        'Invalid Link', '⚠️', 'Invalid verification link',
        '<p>This link is malformed or incomplete. Please use the link from your verification email, or request a new one from the Cellfire app.</p>',
        false
    );
}

// ── DB connection ─────────────────────────────────────────────────────────────
try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER, DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );
} catch (Exception $e) {
    page('Error', '⚠️', 'Temporarily unavailable',
        '<p>Could not connect to the database. Please try again in a moment.</p>', false);
}

// ── Look up the token ─────────────────────────────────────────────────────────
$stmt = $pdo->prepare('SELECT id, username, activation FROM ' . TBL_PFX . 'users WHERE activation = ? LIMIT 1');
$stmt->execute([$token]);
$user = $stmt->fetch(PDO::FETCH_OBJ);

if (!$user) {
    // Check if token was already used (activation cleared)
    page(
        'Already Verified', '✅', 'Already verified',
        '<p>This link has already been used, or it has expired.</p>
         <p style="margin-top:8px">If you haven\'t verified yet, open the Cellfire app and tap <strong style="color:#FF6B1A">Resend verification email</strong>.</p>
         <a class="btn" href="cellfire://login">Open Cellfire App</a>',
        true
    );
}

// ── Verify the account ────────────────────────────────────────────────────────
$pdo->prepare('UPDATE ' . TBL_PFX . "users SET activation = '' WHERE id = ?")
    ->execute([$user->id]);

// Update license expiry: 30 days from NOW (not from signup) so spam accounts that
// never verify don't consume trial time, and legitimate users get their full trial.
$pdo->prepare('UPDATE ' . TBL_PFX . "cellfire_licenses "
    . "SET expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) "
    . "WHERE user_id = ? AND plan_type = 'demo'")
    ->execute([$user->id]);

$username = htmlspecialchars($user->username);

page(
    'Email Verified', '🎉', "You're verified, {$username}!",
    "<p>Your Cellfire account is confirmed and your 30-day free trial starts now.</p>
     <p style='margin-top:8px;'>Open the Cellfire app and sign in to get started.</p>
     <a class='btn' href='cellfire://login'>Open Cellfire App</a>
     <p class='sub'>Don't have the app yet? <a href='https://cellfire.io/app' style='color:#FF6B1A;'>Download it here</a>.</p>"
);
