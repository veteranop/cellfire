<?php
/**
 * Cellfire API - Auth Controller
 * Handles: login, register, refresh, logout
 */
defined('_JEXEC') or die;

require_once __DIR__ . '/../helpers/jwt.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../models/license.php';

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
            ->select(['id', 'username', 'email', 'password', 'block'])
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

        if (empty($license->mac_address)) {
            $licenseModel->activateMac((int) $license->id, $macAddress);
        } elseif ($license->mac_address !== $macAddress) {
            CellfireResponse::error(
                'This license is already activated on another device. Contact support to transfer your license.',
                403
            );
        }

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
     * Creates a Joomla account + 30-day demo license, returns JWT.
     * MAC is bound on first app login (not at registration time).
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

        // Check duplicate email
        $db->setQuery($db->getQuery(true)->select('id')->from('#__users')->where('email = ' . $db->quote($email)));
        if ($db->loadResult()) {
            CellfireResponse::error('An account with that email address already exists.', 409);
        }

        // Insert user directly (avoids User::save() plugin/email cascade which 500s on this host)
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
                0, 0,
                $db->quote($now),
                $db->quote(''),
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

        // Create 30-day demo license
        $demoExpiresStr = date('Y-m-d H:i:s', strtotime('+30 days'));
        $licenseModel   = new CellfireLicenseModel();
        $licenseModel->create($userId, 'demo', $demoExpiresStr);

        // Issue JWT (MAC left blank — bound on first app login)
        $exp     = time() + $this->tokenTtl;
        $payload = [
            'iss'      => 'cellfire.io',
            'sub'      => $userId,
            'username' => $username,
            'plan'     => 'demo',
            'mac'      => '',
            'iat'      => time(),
            'exp'      => $exp,
        ];
        $token = CellfireJWT::encode($payload, $this->secret);

        CellfireResponse::success([
            'token'           => $token,
            'username'        => $username,
            'plan'            => 'demo',
            'expires_at'      => date('c', $exp),
            'demo_expires_at' => date('c', strtotime($demoExpiresStr)),
            'license'         => [
                'plan_type'          => 'demo',
                'is_active'          => 1,
                'expires_at'         => $demoExpiresStr,
                'stripe_status'      => null,
                'stripe_customer_id' => null,
            ],
        ], 'Account created! Your 30-day trial starts now.');
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
}
