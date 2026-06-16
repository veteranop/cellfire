<?php
/**
 * Cellfire API - License Model
 * All DB queries for #__cellfire_licenses.
 */
defined('_JEXEC') or die;

class CellfireLicenseModel
{
    /** @var \Joomla\Database\DatabaseInterface */
    private $db;

    public function __construct()
    {
        $this->db = \Joomla\CMS\Factory::getContainer()
            ->get(\Joomla\Database\DatabaseInterface::class);
    }

    /** Get license row for a user (or null). */
    public function getByUserId(int $userId): ?object
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_licenses')
            ->where('user_id = ' . (int) $userId);
        $this->db->setQuery($query);
        return $this->db->loadObject() ?: null;
    }

    /** Get license row by MAC address. */
    public function getByMac(string $mac): ?object
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_licenses')
            ->where('mac_address = ' . $this->db->quote($mac));
        $this->db->setQuery($query);
        return $this->db->loadObject() ?: null;
    }

    /**
     * Bind a MAC address to a license on first activation.
     * Returns false if MAC is already taken by a different user.
     */
    public function activateMac(int $licenseId, string $mac): bool
    {
        $now = \Joomla\CMS\Factory::getDate()->toSql();
        $query = $this->db->getQuery(true)
            ->update('#__cellfire_licenses')
            ->set([
                'mac_address  = ' . $this->db->quote($mac),
                'activated_at = ' . $this->db->quote($now),
            ])
            ->where('id = ' . (int) $licenseId);
        $this->db->setQuery($query);
        $this->db->execute();
        return true;
    }

    /** Clear the MAC lock so a user can activate on a new machine. */
    public function resetMac(int $userId): void
    {
        $query = $this->db->getQuery(true)
            ->update('#__cellfire_licenses')
            ->set(['mac_address = NULL', 'activated_at = NULL'])
            ->where('user_id = ' . (int) $userId);
        $this->db->setQuery($query);
        $this->db->execute();
    }

    /** Create a new license row for a user (admin use). */
    public function create(int $userId, string $planType = 'pro', ?string $expiresAt = null): int
    {
        $now = \Joomla\CMS\Factory::getDate()->toSql();
        $obj = (object) [
            'user_id'    => $userId,
            'mac_address'=> null,
            'expires_at' => $expiresAt,
            'is_active'  => 1,
            'plan_type'  => $planType,
            'created_at' => $now,
        ];
        $this->db->insertObject('#__cellfire_licenses', $obj);
        return (int) $this->db->insertid();
    }

    /** Deactivate a license. */
    public function deactivate(int $licenseId): void
    {
        $query = $this->db->getQuery(true)
            ->update('#__cellfire_licenses')
            ->set('is_active = 0')
            ->where('id = ' . (int) $licenseId);
        $this->db->setQuery($query);
        $this->db->execute();
    }

    /** List all licenses with joined username (admin). */
    public function listAll(): array
    {
        $query = $this->db->getQuery(true)
            ->select(['l.*', 'u.username', 'u.email'])
            ->from('#__cellfire_licenses AS l')
            ->join('LEFT', '#__users AS u ON u.id = l.user_id')
            ->order('l.created_at DESC');
        $this->db->setQuery($query);
        return $this->db->loadObjectList() ?: [];
    }

    // ── Stripe helpers ────────────────────────────────────────────────────────

    /** Find license by Stripe customer ID. */
    public function getByStripeCustomer(string $customerId): ?object
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_licenses')
            ->where('stripe_customer_id = ' . $this->db->quote($customerId));
        $this->db->setQuery($query);
        return $this->db->loadObject() ?: null;
    }

    /** Find license by Stripe subscription ID. */
    public function getByStripeSubscription(string $subId): ?object
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_licenses')
            ->where('stripe_subscription_id = ' . $this->db->quote($subId));
        $this->db->setQuery($query);
        return $this->db->loadObject() ?: null;
    }

    /**
     * Upsert Stripe fields on an existing license row.
     * Only updates fields provided in $data.
     *
     * @param  int   $userId  Joomla user ID
     * @param  array $data    Keys: plan_type, stripe_customer_id, stripe_subscription_id,
     *                              stripe_price_id, stripe_status, expires_at, is_active
     */
    public function upsertStripe(int $userId, array $data): void
    {
        $existing = $this->getByUserId($userId);
        $now      = \Joomla\CMS\Factory::getDate()->toSql();

        if ($existing) {
            $sets = [];
            $map  = [
                'plan_type'              => true,
                'stripe_customer_id'     => true,
                'stripe_subscription_id' => true,
                'stripe_price_id'        => true,
                'stripe_status'          => true,
                'is_active'              => false, // numeric
                'expires_at'             => true,
            ];
            foreach ($map as $col => $quoted) {
                if (!array_key_exists($col, $data)) continue;
                $val = $data[$col];
                if ($val === null) {
                    $sets[] = "`{$col}` = NULL";
                } elseif ($quoted) {
                    $sets[] = "`{$col}` = " . $this->db->quote($val);
                } else {
                    $sets[] = "`{$col}` = " . (int) $val;
                }
            }
            if (!$sets) return;
            $query = $this->db->getQuery(true)
                ->update('#__cellfire_licenses')
                ->set($sets)
                ->where('user_id = ' . $userId);
            $this->db->setQuery($query)->execute();
        } else {
            // Create a new row
            $this->create(
                $userId,
                $data['plan_type']  ?? 'pro',
                $data['expires_at'] ?? null
            );
            // Then set Stripe fields
            $this->upsertStripe($userId, array_diff_key($data, ['plan_type' => 1, 'expires_at' => 1]));
        }
    }

    // ── Promo code helpers ────────────────────────────────────────────────────

    /** Get all promo codes (admin). */
    public function listPromoCodes(): array
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_promo_codes')
            ->order('created_at DESC');
        $this->db->setQuery($query);
        return $this->db->loadObjectList() ?: [];
    }

    /** Get a single promo code by ID. */
    public function getPromoById(int $id): ?object
    {
        $query = $this->db->getQuery(true)
            ->select('*')
            ->from('#__cellfire_promo_codes')
            ->where('id = ' . $id);
        $this->db->setQuery($query);
        return $this->db->loadObject() ?: null;
    }

    /** Create a new promo code. Returns the new ID. */
    public function createPromoCode(
        string  $code,
        string  $planType       = 'pro',
        int     $durationMonths = 3,
        ?int    $maxUses        = null,
        ?string $expiresAt      = null,
        string  $note           = ''
    ): int {
        $now = \Joomla\CMS\Factory::getDate()->toSql();
        $obj = (object) [
            'code'            => strtoupper($code),
            'plan_type'       => $planType,
            'duration_months' => $durationMonths,
            'max_uses'        => $maxUses,
            'uses_count'      => 0,
            'expires_at'      => $expiresAt,
            'is_active'       => 1,
            'note'            => $note,
            'created_at'      => $now,
        ];
        $this->db->insertObject('#__cellfire_promo_codes', $obj);
        return (int) $this->db->insertid();
    }

    /** Toggle a promo code active/inactive. */
    public function togglePromoCode(int $id): void
    {
        $this->db->setQuery(
            'UPDATE #__cellfire_promo_codes SET is_active = 1 - is_active WHERE id = ' . $id
        )->execute();
    }

    /** Delete a promo code. */
    public function deletePromoCode(int $id): void
    {
        $this->db->setQuery(
            'DELETE FROM #__cellfire_promo_codes WHERE id = ' . $id
        )->execute();
    }
}
