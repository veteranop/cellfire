#!/usr/bin/env python3
"""Patch license.php and verify_email.php on the server."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import push_component_files as pc

COMPONENT = pc.COMPONENT
SITE_ROOT = pc.SITE_ROOT

# ── Patch 1: license.php ── add expires_at to verify response ─────────────────
LICENSE_PHP = r"""<?php
/**
 * Cellfire API - License Controller
 * Handles: verify
 */
defined('_JEXEC') or die;

require_once __DIR__ . '/../helpers/jwt.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../models/license.php';

class CellfireControllerLicense
{
    private string $secret;

    public function __construct()
    {
        $params = \Joomla\CMS\Component\ComponentHelper::getParams('com_cellfireapi');
        $this->secret = $params->get('jwt_secret', '');
    }

    /**
     * GET /index.php?option=com_cellfireapi&task=license.verify
     * Header: Authorization: Bearer <token>
     * Returns current plan_type and actual subscription expires_at from the DB
     * so license upgrades are reflected immediately without requiring re-login.
     */
    public function verify(): void
    {
        $authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
        if (!$authHeader && function_exists('getallheaders')) {
            $headers    = getallheaders();
            $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        }

        if (!preg_match('/^Bearer\s+(.+)$/i', $authHeader, $m)) {
            CellfireResponse::error('Authorization header with Bearer token required.', 401);
        }
        $token = $m[1];

        try {
            $payload = CellfireJWT::decode($token, $this->secret);
        } catch (RuntimeException $e) {
            CellfireResponse::error('Token invalid or expired: ' . $e->getMessage(), 401);
        }

        $licenseModel = new CellfireLicenseModel();
        $license      = $licenseModel->getByUserId((int) ($payload['sub'] ?? 0));

        if (!$license || !(int) $license->is_active) {
            CellfireResponse::error('License deactivated.', 403);
        }

        if ($license->expires_at !== null && strtotime($license->expires_at) < time()) {
            CellfireResponse::error('License expired.', 403);
        }

        CellfireResponse::success([
            'username'   => $payload['username'] ?? '',
            'plan'       => $license->plan_type,
            'exp'        => $payload['exp']         ?? 0,
            'expires_at' => $license->expires_at,   // actual subscription expiry, not JWT expiry
        ], 'License valid');
    }
}
"""

# ── Patch 2: verify_email.php ── only reset demo trial, not paid plans ─────────
verify_php = open(os.path.join(os.path.dirname(__file__), 'verify_email.php'),
                  encoding='utf-8').read()

old_update = ("$pdo->prepare('UPDATE ' . TBL_PFX . \"cellfire_licenses "
              "SET expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE user_id = ?\")\n"
              "    ->execute([$user->id]);")
new_update = ("$pdo->prepare('UPDATE ' . TBL_PFX . \"cellfire_licenses \"\n"
              "    . \"SET expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) \"\n"
              "    . \"WHERE user_id = ? AND plan_type = 'demo'\")\n"
              "    ->execute([$user->id]);")

patched_verify = verify_php.replace(old_update, new_update)
if patched_verify == verify_php:
    # Try simpler find
    old_simple = "SET expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE user_id = ?"
    new_simple  = "SET expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE user_id = ? AND plan_type = 'demo'"
    patched_verify = verify_php.replace(old_simple, new_simple)

print("1. Pushing controllers/license.php ...")
ok, msg = pc.cpanel_save(f"{COMPONENT}/controllers", "license.php", LICENSE_PHP)
print(f"   [{'OK' if ok else 'FAIL'}] {msg}")

print("2. Pushing verify_email.php (demo-only expiry reset) ...")
ok, msg = pc.cpanel_save(SITE_ROOT, "verify_email.php", patched_verify)
print(f"   [{'OK' if ok else 'FAIL'}] {msg}")

# Also update local copy
open(os.path.join(os.path.dirname(__file__), 'verify_email.php'), 'w', encoding='utf-8').write(patched_verify)
print("   (local copy updated)")
