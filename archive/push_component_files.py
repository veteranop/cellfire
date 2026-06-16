#!/usr/bin/env python3
"""
Push multi-device support files to cellfire.io via cPanel API.
Run locally: python push_component_files.py
"""
import urllib.request
import urllib.parse
import ssl
import json
import sys
import os

# Load credentials from push_rules_config.py
sys.path.insert(0, os.path.dirname(__file__))
import push_rules_config as cfg

CPANEL_HOST  = "just2029.justhost.com"
CPANEL_PORT  = 2083
CPANEL_USER  = cfg.CPANEL_USER
API_TOKEN    = cfg.API_TOKEN

SITE_ROOT = "/home1/veterap2/public_html/website_9695cd55"
COMPONENT = f"{SITE_ROOT}/components/com_cellfireapi"


def cpanel_save(server_dir: str, filename: str, content: str) -> tuple:
    url  = f"https://{CPANEL_HOST}:{CPANEL_PORT}/execute/Fileman/save_file_content"
    data = urllib.parse.urlencode({
        "dir":     server_dir,
        "file":    filename,
        "content": content,
    }).encode("utf-8")
    headers = {
        "Authorization": f"cpanel {CPANEL_USER}:{API_TOKEN}",
        "Content-Type":  "application/x-www-form-urlencoded",
    }
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode    = ssl.CERT_NONE
    req = urllib.request.Request(url, data=data, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=30) as r:
            resp = json.loads(r.read())
        errors = resp.get("errors") or resp.get("error")
        ok     = not errors and (resp.get("status") == 1 or resp.get("data") is not None)
        msg    = str(errors) if errors else "OK"
        return ok, msg
    except Exception as e:
        return False, str(e)


def push(server_dir: str, filename: str, content: str):
    ok, msg = cpanel_save(server_dir, filename, content)
    status  = "OK" if ok else "FAIL"
    print(f"  [{status}] {server_dir}/{filename}: {msg}")
    return ok


# ── File contents ─────────────────────────────────────────────────────────────

DEVICE_MODEL_PHP = r"""<?php
/**
 * Cellfire API - Device Model
 * Manages the #__cellfire_devices table for multi-device enterprise plans.
 * Table is auto-created on first use.
 */
defined('_JEXEC') or die;

class CellfireDeviceModel
{
    /** @var \Joomla\Database\DatabaseInterface */
    private $db;

    public function __construct()
    {
        $this->db = \Joomla\CMS\Factory::getContainer()
            ->get(\Joomla\Database\DatabaseInterface::class);
        $this->ensureTable();
    }

    private function ensureTable(): void
    {
        $this->db->setQuery("
            CREATE TABLE IF NOT EXISTS `#__cellfire_devices` (
                `id`          INT AUTO_INCREMENT PRIMARY KEY,
                `license_id`  INT NOT NULL,
                `mac_address` VARCHAR(64) NOT NULL,
                `device_name` VARCHAR(128) DEFAULT NULL,
                `added_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
                `last_seen`   DATETIME DEFAULT NULL,
                UNIQUE KEY `uniq_license_mac` (`license_id`, `mac_address`),
                KEY `idx_license_id` (`license_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ")->execute();
    }

    /**
     * Check whether a MAC is already registered for this license.
     * Also updates last_seen if it is.
     */
    public function isAuthorized(int $licenseId, string $mac): bool
    {
        $this->db->setQuery(
            "SELECT id FROM #__cellfire_devices WHERE license_id = " . (int) $licenseId .
            " AND mac_address = " . $this->db->quote($mac)
        );
        $id = $this->db->loadResult();
        if ($id) {
            $now = \Joomla\CMS\Factory::getDate()->toSql();
            $this->db->setQuery(
                "UPDATE #__cellfire_devices SET last_seen = " . $this->db->quote($now) .
                " WHERE id = " . (int) $id
            )->execute();
            return true;
        }
        return false;
    }

    /** Count registered devices for this license. */
    public function count(int $licenseId): int
    {
        $this->db->setQuery(
            "SELECT COUNT(*) FROM #__cellfire_devices WHERE license_id = " . (int) $licenseId
        );
        return (int) $this->db->loadResult();
    }

    /** Register a new device for this license. */
    public function add(int $licenseId, string $mac): void
    {
        $now = \Joomla\CMS\Factory::getDate()->toSql();
        $this->db->setQuery(
            "INSERT IGNORE INTO #__cellfire_devices (license_id, mac_address, added_at, last_seen) VALUES (" .
            (int) $licenseId . ", " . $this->db->quote($mac) . ", " .
            $this->db->quote($now) . ", " . $this->db->quote($now) . ")"
        )->execute();
    }

    /** List all devices registered to a license. */
    public function listByLicenseId(int $licenseId): array
    {
        $this->db->setQuery(
            "SELECT id, mac_address, device_name, added_at, last_seen " .
            "FROM #__cellfire_devices WHERE license_id = " . (int) $licenseId .
            " ORDER BY added_at ASC"
        );
        return $this->db->loadObjectList() ?: [];
    }

    /** Remove a specific device by its row ID. */
    public function remove(int $deviceId): void
    {
        $this->db->setQuery(
            "DELETE FROM #__cellfire_devices WHERE id = " . (int) $deviceId
        )->execute();
    }

    /** Remove all devices for a license (e.g. when resetting to single-device plan). */
    public function removeAllForLicense(int $licenseId): void
    {
        $this->db->setQuery(
            "DELETE FROM #__cellfire_devices WHERE license_id = " . (int) $licenseId
        )->execute();
    }
}
"""

# Read current auth.php from the probe output we already have — we know the exact structure
AUTH_PHP = r"""<?php
/**
 * Cellfire API - Auth Controller
 * Handles: login, register, refresh, logout
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

        // ── Device / MAC enforcement (plan-aware) ─────────────────────────────
        $maxDevices = planMaxDevices($license->plan_type);

        if ($maxDevices > 1) {
            // Multi-device path: check the devices table
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
            // Single-device path: legacy MAC column on the license row
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

        // ── Slack signup alert ────────────────────────────────────────────────
        $cfLocalCfg = __DIR__ . '/cf_local_config.php';
        if (file_exists($cfLocalCfg)) {
            include_once $cfLocalCfg;
        }
        if (defined('CF_SLACK_SIGNUP_WEBHOOK') && CF_SLACK_SIGNUP_WEBHOOK) {
            $slackPayload = json_encode([
                'text' => ":new: *New Cellfire Signup*\n:bust_in_silhouette: @{$username}\n:email: {$email}\n:dart: 30-day demo trial started\n:stopwatch: " . date('Y-m-d H:i T'),
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
"""


def read_local(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def build_updated_cf_users_api(current: str) -> str:
    """
    Inject list_devices and remove_device actions into cf_users_api.php
    just before the final "Unknown action" fallthrough.
    """
    insert_before = "echo json_encode(['ok'=>false,'error'=>'Unknown action: '.$action]);"
    new_actions = r"""
// ── list_devices ─────────────────────────────────────────────────────────────
if ($action === 'list_devices') {
    $uid = (int)($_REQUEST['user_id'] ?? 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    // Get license_id for this user
    $row = $pdo->prepare("SELECT id, plan_type FROM " . TBL_PFX . "cellfire_licenses WHERE user_id = ?");
    $row->execute([$uid]);
    $lic = $row->fetch(PDO::FETCH_OBJ);
    if (!$lic) { echo json_encode(['ok'=>false,'error'=>'No license found']); exit; }
    // Check if devices table exists
    $tableCheck = $pdo->query("SHOW TABLES LIKE '" . str_replace('jo_', '', TBL_PFX) . "jo_cellfire_devices'")->fetchColumn();
    $devices = [];
    if ($tableCheck !== false) {
        $dstmt = $pdo->prepare("SELECT id, mac_address, device_name, added_at, last_seen FROM " . TBL_PFX . "cellfire_devices WHERE license_id = ? ORDER BY added_at ASC");
        $dstmt->execute([$lic->id]);
        $devices = $dstmt->fetchAll(PDO::FETCH_ASSOC);
    }
    echo json_encode(['ok'=>true,'devices'=>$devices,'plan_type'=>$lic->plan_type,'license_id'=>(int)$lic->id]);
    exit;
}

// ── remove_device ─────────────────────────────────────────────────────────────
if ($action === 'remove_device') {
    $deviceId = (int)($_REQUEST['device_id'] ?? 0);
    if (!$deviceId) { echo json_encode(['ok'=>false,'error'=>'Missing device_id']); exit; }
    $pdo->prepare("DELETE FROM " . TBL_PFX . "cellfire_devices WHERE id = ?")
        ->execute([$deviceId]);
    echo json_encode(['ok'=>true,'message'=>'Device removed']);
    exit;
}

"""
    if insert_before in current:
        return current.replace(insert_before, new_actions + insert_before)
    # fallback: append before closing ?>
    return current + "\n" + new_actions


if __name__ == "__main__":
    print("=== Cellfire Multi-Device Deployment ===\n")

    # 1. Push device model
    print("1. Pushing models/device.php ...")
    push(f"{COMPONENT}/models", "device.php", DEVICE_MODEL_PHP)

    # 2. Push updated auth.php
    print("2. Pushing controllers/auth.php ...")
    push(f"{COMPONENT}/controllers", "auth.php", AUTH_PHP)

    # 3. Update cf_users_api.php
    print("3. Updating cf_users_api.php ...")
    cf_users_path = os.path.join(os.path.dirname(__file__), "site_template", "cf_users_api.php")
    if not os.path.exists(cf_users_path):
        # Try to get current content from server probe
        print("  (cf_users_api.php not found locally — skipping; run the probe to get it first)")
    else:
        current = read_local(cf_users_path)
        if 'list_devices' not in current:
            updated = build_updated_cf_users_api(current)
            push(SITE_ROOT, "cf_users_api.php", updated)
        else:
            print("  [SKIP] list_devices already present")

    print("\nDone. Enterprise accounts can now register up to 5 devices.")
    print("Team/bundle_pro accounts: up to 3 devices.")
    print("All other plans: unchanged single-device behavior.")
