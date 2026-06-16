<?php
/**
 * Cellfire API - Auth Controller
 * Handles: login, register, refresh, logout, resend_verify
 */
defined('_JEXEC') or die;

require_once __DIR__ . '/../helpers/jwt.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../models/license.php';
require_once __DIR__ . '/../models/device.php';

/**
 * Returns the maximum number of devices allowed for a given plan type.
 * Plans containing 'enterprise' → 5 devices.
 * Plans containing 'team' or 'bundle_pro' → 3 devices.
 * All others → 1 device (existing single-MAC behaviour).
 */
function planMaxDevices(string $planType): int
{
    if (str_contains($planType, 'enterprise')) return 5;
    if (str_contains($planType, 'team'))       return 3;
    if ($planType === 'bundle_pro')             return 3;
    return 1;
}

class CellfireControllerAuth
{
    private string $secret;
    private int    $tokenTtl = 86400; // 24 hours

    public function __construct()
    {
        $params = \Joomla\CMS\Component\ComponentHelper::getParams('com_cellfireapi');
        $this->secret = $params->get('jwt_secret', '');
        if (empty($this->secret) || $this->secret === 'CHANGE_ME_TO_A_LONG_RANDOM_STRING_32CHARS') {
            CellfireResponse::error('Server misconfiguration: JWT secret not set.', 500);
        }
    }

    /**
     * POST: {username, password, mac_address} -> {token, expires_at, username, license}
     * 'username' accepts either a username or email address.
     */
    public function login(): void
    {
        $input      = json_decode(file_get_contents('php://input'), true) ?? [];
        $identifier = trim($input['username']    ?? '');
        $password   = trim($input['password']    ?? '');
        $macAddress = strtolower(trim($input['mac_address'] ?? ''));

        if (!$identifier || !$password || !$macAddress) {
            CellfireResponse::error('username (or email), password, and mac_address are required.');
        }

        $db    = \Joomla\CMS\Factory::getContainer()->get(\Joomla\Database\DatabaseInterface::class);
        $query = $db->getQuery(true)
            ->select(['id', 'username', 'email', 'password', 'block', 'activation'])
            ->from('#__users');

        if (str_contains($identifier, '@')) {
            $query->where('email = ' . $db->quote($identifier));
        } else {
            $query->where('username = ' . $db->quote($identifier));
        }

        $db->setQuery($query);
        $user = $db->loadObject();

        if (!$user) {
            CellfireResponse::error('Invalid username or password.', 401);
        }

        // Check email verification before anything else — gives a clear actionable message
        // Only block on OUR 64-char hex tokens — Joomla stores other values in this field
        // that existing users may have from old registrations.
        if (!empty($user->activation) && preg_match('/^[a-f0-9]{64}$/', $user->activation)) {
            CellfireResponse::error(
                'Please verify your email address before signing in. ' .
                'Check your inbox for a verification link, or request a new one.',
                403
            );
        }

        if ((int) $user->block === 1) {
            CellfireResponse::error('Account is blocked. Contact support.', 403);
        }
        if (!\Joomla\CMS\User\UserHelper::verifyPassword($password, $user->password, (int) $user->id)) {
            CellfireResponse::error('Invalid username or password.', 401);
        }

        $licenseModel = new CellfireLicenseModel();
        $license      = $licenseModel->getByUserId((int) $user->id);

        if (!$license) {
            CellfireResponse::error('No license found for this account. Visit cellfire.io to get access.', 403);
        }
        if (!(int) $license->is_active) {
            CellfireResponse::error('Your license has been deactivated. Contact support.', 403);
        }
        if ($license->expires_at !== null && strtotime($license->expires_at) < time()) {
            $expiredMsg = ($license->plan_type === 'demo')
                ? 'Your 30-day trial has expired. Visit cellfire.io/pricing to subscribe.'
                : 'Your license has expired. Visit cellfire.io to renew.';
            CellfireResponse::error($expiredMsg, 403);
        }

        // ── Device / MAC enforcement (plan-aware) ─────────────────────────────
        $maxDevices = planMaxDevices($license->plan_type);

        if ($maxDevices > 1) {
            $deviceModel = new CellfireDeviceModel();
            if (!$deviceModel->isAuthorized((int) $license->id, $macAddress)) {
                $deviceCount = $deviceModel->count((int) $license->id);
                if ($deviceCount >= $maxDevices) {
                    CellfireResponse::error(
                        "Device limit reached ($deviceCount/$maxDevices). " .
                        "Your plan supports up to $maxDevices devices. " .
                        "Visit cellfire.io or contact support to manage your devices.",
                        403
                    );
                }
                $deviceModel->add((int) $license->id, $macAddress);
            }
        } else {
            if (empty($license->mac_address)) {
                $licenseModel->activateMac((int) $license->id, $macAddress);
            } elseif ($license->mac_address !== $macAddress) {
                CellfireResponse::error(
                    'This license is already activated on another device. Contact support to transfer your license.',
                    403
                );
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        $exp     = time() + $this->tokenTtl;
        $payload = [
            'iss'      => 'cellfire.io',
            'sub'      => (int) $user->id,
            'username' => $user->username,
            'plan'     => $license->plan_type,
            'mac'      => $macAddress,
            'iat'      => time(),
            'exp'      => $exp,
        ];
        $token = CellfireJWT::encode($payload, $this->secret);

        $demoExpiresAt = null;
        if ($license->plan_type === 'demo' && !empty($license->expires_at)) {
            $demoExpiresAt = date('c', strtotime($license->expires_at));
        }

        CellfireResponse::success([
            'token'           => $token,
            'username'        => $user->username,
            'plan'            => $license->plan_type,
            'expires_at'      => date('c', $exp),
            'demo_expires_at' => $demoExpiresAt,
            'license'         => [
                'plan_type'          => $license->plan_type,
                'is_active'          => (int) $license->is_active,
                'expires_at'         => $license->expires_at,
                'stripe_status'      => $license->stripe_status      ?? null,
                'stripe_customer_id' => $license->stripe_customer_id ?? null,
            ],
        ], 'Login successful');
    }

    /**
     * POST: {username, email, password}
     * Creates account in unverified state (block=0, activation=token), sends verification email.
     * Returns requires_verification=true — no JWT issued until email is confirmed.
     */
    public function register(): void
    {
        $input    = json_decode(file_get_contents('php://input'), true) ?? [];
        $username = trim($input['username'] ?? '');
        $email    = trim($input['email']    ?? '');
        $password =      $input['password'] ?? '';

        if (!$username || !$email || !$password) {
            CellfireResponse::error('username, email, and password are required.');
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            CellfireResponse::error('Please enter a valid email address.');
        }
        if (strlen($password) < 8) {
            CellfireResponse::error('Password must be at least 8 characters long.');
        }
        if (!preg_match('/^[a-zA-Z0-9_]{3,32}$/', $username)) {
            CellfireResponse::error('Username must be 3-32 characters (letters, numbers, underscores only).');
        }

        $db = \Joomla\CMS\Factory::getContainer()->get(\Joomla\Database\DatabaseInterface::class);

        // Check duplicate username
        $db->setQuery($db->getQuery(true)->select('id')->from('#__users')->where('username = ' . $db->quote($username)));
        if ($db->loadResult()) {
            CellfireResponse::error('That username is already taken. Please choose another.', 409);
        }

        // Check duplicate email — detect unverified accounts so we can guide the user
        $db->setQuery($db->getQuery(true)->select(['id', 'activation'])->from('#__users')->where('email = ' . $db->quote($email)));
        $existing = $db->loadObject();
        if ($existing) {
            if (!empty($existing->activation)) {
                CellfireResponse::error(
                    'An account with that email already exists but hasn\'t been verified. ' .
                    'Check your inbox or use "Resend verification email" to get a new link.',
                    409
                );
            }
            CellfireResponse::error('An account with that email address already exists.', 409);
        }

        // Generate verification token
        $verifyToken = bin2hex(random_bytes(32));

        // Insert user with activation token set (unverified state)
        $now  = date('Y-m-d H:i:s');
        $hash = \Joomla\CMS\User\UserHelper::hashPassword($password);

        $db->setQuery(
            'INSERT INTO ' . $db->quoteName('#__users') .
            ' (name, username, email, password, block, sendEmail, registerDate, activation, params, resetCount, otpKey, otep, requireReset, authProvider)' .
            ' VALUES (' . implode(',', [
                $db->quote($username),
                $db->quote($username),
                $db->quote($email),
                $db->quote($hash),
                0,              // block=0 — we gate on activation, not block
                0,
                $db->quote($now),
                $db->quote($verifyToken),   // non-empty = unverified
                $db->quote('{}'),
                0,
                $db->quote(''),
                $db->quote(''),
                0,
                $db->quote('joomla'),
            ]) . ')'
        );
        $db->execute();
        $userId = (int) $db->insertid();

        if (!$userId) {
            CellfireResponse::error('Could not create account. Please try again.', 500);
        }

        // Add to Registered group (group_id = 2)
        $db->setQuery(
            'INSERT IGNORE INTO ' . $db->quoteName('#__user_usergroup_map') .
            ' (user_id, group_id) VALUES (' . $userId . ', 2)'
        );
        $db->execute();

        // Create 30-day demo license (starts ticking from verification, not signup)
        $demoExpiresStr = date('Y-m-d H:i:s', strtotime('+30 days'));
        $licenseModel   = new CellfireLicenseModel();
        $licenseModel->create($userId, 'demo', $demoExpiresStr);

        // Send verification email
        $this->sendVerifyEmail($email, $username, $verifyToken);

        // ── Slack signup alert ────────────────────────────────────────────────
        $cfLocalCfg = __DIR__ . '/cf_local_config.php';
        if (file_exists($cfLocalCfg)) {
            include_once $cfLocalCfg;
        }
        if (defined('CF_SLACK_SIGNUP_WEBHOOK') && CF_SLACK_SIGNUP_WEBHOOK) {
            $slackPayload = json_encode([
                'text' => ":new: *New Cellfire Signup* (pending verification)\n:bust_in_silhouette: @{$username}\n:email: {$email}\n:stopwatch: " . date('Y-m-d H:i T'),
            ]);
            if (function_exists('curl_init')) {
                $ch = curl_init(CF_SLACK_SIGNUP_WEBHOOK);
                curl_setopt_array($ch, [
                    CURLOPT_POST           => true,
                    CURLOPT_POSTFIELDS     => $slackPayload,
                    CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_TIMEOUT        => 3,
                    CURLOPT_SSL_VERIFYPEER => false,
                ]);
                @curl_exec($ch);
                curl_close($ch);
            }
        }

        CellfireResponse::success([
            'requires_verification' => true,
        ], 'Account created! Check your email to verify your account and start your trial.');
    }

    /**
     * POST: {username} OR {email}
     * Resends the verification email. Safe to call with either field.
     */
    public function resend_verify(): void
    {
        $input      = json_decode(file_get_contents('php://input'), true) ?? [];
        $identifier = trim($input['username'] ?? $input['email'] ?? '');

        if (!$identifier) {
            CellfireResponse::error('username or email is required.');
        }

        $db    = \Joomla\CMS\Factory::getContainer()->get(\Joomla\Database\DatabaseInterface::class);
        $query = $db->getQuery(true)
            ->select(['id', 'username', 'email', 'activation'])
            ->from('#__users');

        if (str_contains($identifier, '@')) {
            $query->where('email = ' . $db->quote($identifier));
        } else {
            $query->where('username = ' . $db->quote($identifier));
        }

        $db->setQuery($query);
        $user = $db->loadObject();

        // Always return success — don't reveal whether the account exists
        if (!$user || empty($user->activation)) {
            CellfireResponse::success(
                [],
                'If an unverified account exists for that email, a new link has been sent.'
            );
        }

        // Generate a fresh token
        $newToken = bin2hex(random_bytes(32));
        $db->setQuery(
            'UPDATE ' . $db->quoteName('#__users') .
            ' SET activation = ' . $db->quote($newToken) .
            ' WHERE id = ' . (int) $user->id
        );
        $db->execute();

        $this->sendVerifyEmail($user->email, $user->username, $newToken);

        CellfireResponse::success(
            [],
            'If an unverified account exists for that email, a new link has been sent.'
        );
    }

    /** POST: {token} -> {token, expires_at} — refresh before expiry */
    public function refresh(): void
    {
        $input = json_decode(file_get_contents('php://input'), true) ?? [];
        $token = trim($input['token'] ?? '');

        if (!$token) {
            CellfireResponse::error('token is required.');
        }

        try {
            $parts    = explode('.', $token);
            $payload  = json_decode(base64_decode(strtr($parts[1] ?? '', '-_', '+/')), true);
            $header   = $parts[0];
            $body     = $parts[1];
            $sig      = $parts[2] ?? '';
            $expected = rtrim(strtr(base64_encode(hash_hmac('sha256', "$header.$body", $this->secret, true)), '+/', '-_'), '=');
            if (!hash_equals($expected, $sig)) {
                throw new RuntimeException('Invalid signature');
            }
            $graceSeconds = 7 * 86400;
            if (isset($payload['exp']) && $payload['exp'] < (time() - $graceSeconds)) {
                CellfireResponse::error('Token too old to refresh. Please log in again.', 401);
            }
        } catch (RuntimeException $e) {
            CellfireResponse::error('Invalid token: ' . $e->getMessage(), 401);
        }

        $userId       = (int) ($payload['sub'] ?? 0);
        $licenseModel = new CellfireLicenseModel();
        $license      = $licenseModel->getByUserId($userId);

        if (!$license || !(int) $license->is_active) {
            CellfireResponse::error('License no longer active.', 403);
        }

        $exp              = time() + $this->tokenTtl;
        $payload['iat']   = time();
        $payload['exp']   = $exp;
        $newToken         = CellfireJWT::encode($payload, $this->secret);

        CellfireResponse::success([
            'token'      => $newToken,
            'expires_at' => date('c', $exp),
        ], 'Token refreshed');
    }

    /** POST: stateless JWT — client just deletes local token */
    public function logout(): void
    {
        CellfireResponse::success([], 'Logged out');
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private function sendVerifyEmail(string $toEmail, string $toUsername, string $token): void
    {
        $verifyUrl = 'https://cellfire.io/verify_email.php?token=' . urlencode($token);

        $subject = 'Verify your Cellfire account';

        $html = <<<HTML
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
<body style="margin:0;padding:0;background:#0d0d0d;font-family:Arial,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0d0d0d;padding:40px 20px;">
    <tr><td align="center">
      <table width="480" cellpadding="0" cellspacing="0" style="background:#1a1a1a;border-radius:12px;overflow:hidden;max-width:480px;width:100%;">
        <tr>
          <td style="background:#1a1a1a;padding:32px 32px 24px;text-align:center;border-bottom:1px solid #2a2a2a;">
            <span style="font-size:28px;font-weight:bold;color:#FF6B1A;letter-spacing:1px;">CELLFIRE</span>
          </td>
        </tr>
        <tr>
          <td style="padding:32px;">
            <p style="color:#ffffff;font-size:20px;font-weight:bold;margin:0 0 8px;">Hey {$toUsername},</p>
            <p style="color:#aaaaaa;font-size:15px;line-height:1.6;margin:0 0 28px;">
              Thanks for signing up! Tap the button below to verify your email address and start your 30-day free trial.
            </p>
            <div style="text-align:center;margin-bottom:28px;">
              <a href="{$verifyUrl}"
                 style="display:inline-block;background:#FF6B1A;color:#000000;font-size:16px;font-weight:bold;
                        text-decoration:none;padding:14px 36px;border-radius:8px;">
                Verify My Email
              </a>
            </div>
            <p style="color:#666666;font-size:12px;line-height:1.5;margin:0;">
              If the button doesn't work, paste this link into your browser:<br>
              <a href="{$verifyUrl}" style="color:#FF6B1A;word-break:break-all;">{$verifyUrl}</a>
            </p>
          </td>
        </tr>
        <tr>
          <td style="padding:16px 32px;border-top:1px solid #2a2a2a;text-align:center;">
            <p style="color:#444444;font-size:11px;margin:0;">
              Didn't create a Cellfire account? You can safely ignore this email.
            </p>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
HTML;

        $plain = "Hey {$toUsername},\n\nVerify your Cellfire account by visiting this link:\n{$verifyUrl}\n\nIf you didn't create a Cellfire account you can ignore this email.\n\nThe Cellfire Team";

        $headers  = "MIME-Version: 1.0\r\n";
        $headers .= "Content-Type: text/html; charset=UTF-8\r\n";
        $headers .= "From: Cellfire <noreply@cellfire.io>\r\n";
        $headers .= "Reply-To: support@cellfire.io\r\n";
        $headers .= "X-Mailer: Cellfire/1.0\r\n";

        @mail($toEmail, $subject, $html, $headers);
    }
}
