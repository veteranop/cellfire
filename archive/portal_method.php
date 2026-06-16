    /**
     * GET/POST ?task=stripe.portal&format=json
     *
     * Works with both a logged-in Joomla session (website) and a Bearer JWT (app/Studio).
     * Looks up the user's stripe_customer_id, creates a Customer Portal session, returns URL.
     */
    public function portal(): void
    {
        // ── Identify user ────────────────────────────────────────────────────
        $userId = 0;
        $user   = Factory::getUser();
        if ($user->id) {
            $userId = (int) $user->id;
        } else {
            // Try Bearer JWT (for app / Studio calls)
            $authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
            if (!$authHeader && function_exists('getallheaders')) {
                $headers    = getallheaders();
                $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
            }
            if (preg_match('/^Bearer\s+(.+)$/i', $authHeader, $m)) {
                $secret = $this->params->get('jwt_secret', '');
                try {
                    $payload = CellfireJWT::decode($m[1], $secret);
                    $userId  = (int) ($payload['sub'] ?? 0);
                } catch (\RuntimeException $e) {
                    CellfireResponse::error('Invalid token: ' . $e->getMessage(), 401);
                }
            }
        }

        if (!$userId) {
            CellfireResponse::error('Authentication required. Please sign in to manage billing.', 401);
        }

        // ── Get Stripe customer ID from license ──────────────────────────────
        $lic = $this->db->setQuery(
            $this->db->getQuery(true)
                ->select(['stripe_customer_id', 'plan_type'])
                ->from('#__cellfire_licenses')
                ->where('user_id = ' . $userId)
        )->loadObject();

        if (!$lic || empty($lic->stripe_customer_id)) {
            CellfireResponse::error(
                'No billing account found. You may not have an active paid subscription yet.',
                404
            );
        }

        // ── Create portal session ────────────────────────────────────────────
        $returnUrl = \Joomla\CMS\Uri\Uri::root() . 'account';

        try {
            $session = CellfireStripe::createPortalSession([
                'customer'   => $lic->stripe_customer_id,
                'return_url' => $returnUrl,
            ]);
        } catch (\RuntimeException $e) {
            CellfireResponse::error($e->getMessage(), 500);
        }

        CellfireResponse::success(['url' => $session['url']]);
    }

