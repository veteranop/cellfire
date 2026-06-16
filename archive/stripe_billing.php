<?php
/**
 * stripe_billing.php — Cellfire Stripe Customer Portal redirect
 *
 * Deploy to: cellfire.io/stripe-billing
 * (upload to /home1/veterap2/public_html/website_9695cd55/stripe-billing.php
 *  or configure a Joomla redirect from /stripe-billing to this file)
 *
 * Flow:
 *   1. User clicks "Manage Billing" on /account
 *   2. This script reads their Firebase ID token from the request
 *   3. Verifies the token via Firebase REST API
 *   4. Looks up / creates their Stripe customer by email
 *   5. Creates a Stripe Billing Portal session
 *   6. Redirects the user to the portal URL
 *
 * SETUP REQUIRED:
 *   - Set STRIPE_SECRET_KEY below (found in Stripe Dashboard → Developers → API Keys)
 *   - Set FIREBASE_PROJECT_ID below
 *   - Configure the Customer Portal in Stripe Dashboard → Settings → Billing → Customer Portal
 *   - Optional: store stripe_customer_id in your Firebase user records for faster lookup
 */

define('STRIPE_SECRET_KEY', '');          // ← ADD YOUR STRIPE SECRET KEY (sk_live_...)
define('FIREBASE_PROJECT_ID', 'cellfire'); // ← verify this matches your Firebase project
define('RETURN_URL', 'https://cellfire.io/account');

// ── Helpers ──────────────────────────────────────────────────────────────────

function stripe_api(string $method, string $endpoint, array $params = []): array {
    $ch = curl_init('https://api.stripe.com/v1/' . ltrim($endpoint, '/'));
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_USERPWD        => STRIPE_SECRET_KEY . ':',
        CURLOPT_HTTPHEADER     => ['Content-Type: application/x-www-form-urlencoded'],
    ]);
    if ($method === 'POST') {
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($params));
    }
    $body   = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    $data = json_decode($body, true);
    if ($status >= 400) {
        throw new RuntimeException('Stripe error: ' . ($data['error']['message'] ?? $body));
    }
    return $data;
}

function verify_firebase_token(string $id_token): ?array {
    // Verify via Firebase REST API (tokeninfo endpoint)
    $url = 'https://www.googleapis.com/identitytoolkit/v3/relyingparty/getAccountInfo?key=';
    // We use the public verify endpoint — no server key needed for basic verification
    $ch = curl_init(
        'https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=' .
        get_firebase_web_api_key()
    );
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => json_encode(['idToken' => $id_token]),
        CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
    ]);
    $body = curl_exec($ch);
    curl_close($ch);
    $data = json_decode($body, true);
    if (!empty($data['users'][0])) {
        return $data['users'][0]; // { localId, email, ... }
    }
    return null;
}

function get_firebase_web_api_key(): string {
    // Read from a local config file (keep outside public_html for security)
    // e.g. /home1/veterap2/cellfire_config.php with: define('FIREBASE_WEB_API_KEY','AIza...');
    $config = dirname(__DIR__, 2) . '/cellfire_config.php';
    if (file_exists($config)) {
        require_once $config;
        return defined('FIREBASE_WEB_API_KEY') ? FIREBASE_WEB_API_KEY : '';
    }
    return '';
}

function get_or_create_stripe_customer(string $email, string $uid): string {
    // Search for existing customer by email
    $result = stripe_api('GET', 'customers/search?query=' . urlencode("email:'$email'") . '&limit=1');
    if (!empty($result['data'][0]['id'])) {
        return $result['data'][0]['id'];
    }
    // Create new customer
    $customer = stripe_api('POST', 'customers', [
        'email'    => $email,
        'metadata' => ['firebase_uid' => $uid],
    ]);
    return $customer['id'];
}

// ── Main ─────────────────────────────────────────────────────────────────────

header('Content-Type: text/html; charset=utf-8');

if (empty(STRIPE_SECRET_KEY)) {
    http_response_code(503);
    echo '<p style="font-family:sans-serif;color:#f44;padding:40px;">
        Billing portal not yet configured. Please contact
        <a href="mailto:support@cellfire.io">support@cellfire.io</a>.
    </p>';
    exit;
}

// Get Firebase ID token — passed as ?token=... from the account page JS
$id_token = $_GET['token'] ?? $_POST['token'] ?? '';

if (empty($id_token)) {
    // Redirect to account page with error
    header('Location: ' . RETURN_URL . '?billing_error=missing_token');
    exit;
}

try {
    // 1. Verify Firebase token
    $user = verify_firebase_token($id_token);
    if (!$user) {
        header('Location: ' . RETURN_URL . '?billing_error=auth_failed');
        exit;
    }

    $email = $user['email'] ?? '';
    $uid   = $user['localId'] ?? '';

    if (empty($email)) {
        throw new RuntimeException('No email on Firebase account');
    }

    // 2. Get or create Stripe customer
    $customer_id = get_or_create_stripe_customer($email, $uid);

    // 3. Create billing portal session
    $session = stripe_api('POST', 'billing_portal/sessions', [
        'customer'   => $customer_id,
        'return_url' => RETURN_URL,
    ]);

    // 4. Redirect to portal
    header('Location: ' . $session['url']);
    exit;

} catch (Throwable $e) {
    error_log('stripe_billing.php error: ' . $e->getMessage());
    header('Location: ' . RETURN_URL . '?billing_error=server_error');
    exit;
}
