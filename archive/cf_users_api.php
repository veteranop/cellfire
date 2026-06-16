<?php
/**
 * cf_users_api.php — Cellfire user / subscription manager
 * Protected by RUNNER_SECRET token.
 * Deployed to: /home1/veterap2/public_html/website_9695cd55/cf_users_api.php
 */
header('Content-Type: application/json');
header('Cache-Control: no-store');

define('SECRET', 'cf-d5d3518f2338406f22c57277d6d85023');
define('DB_HOST', 'localhost');
define('DB_NAME', 'veterap2_joom819');
define('DB_USER', 'veterap2_joom819');
define('DB_PASS', 'B1S!wp4K)6');
define('TBL_PFX', 'josbf_');

$secret = $_REQUEST['secret'] ?? '';
if ($secret !== SECRET) {
    http_response_code(403);
    echo json_encode(['ok' => false, 'error' => 'Forbidden']);
    exit;
}

try {
    $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4',
                   DB_USER, DB_PASS,
                   [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'DB: ' . $e->getMessage()]);
    exit;
}

$action = $_REQUEST['action'] ?? 'list';

$ALLOWED_PLANS = ['demo','app_view','app_full','app_team','studio_standard','studio_full',
    'studio_enterprise','viewer_basic','viewer_pro','viewer_enterprise',
    'bundle_starter','bundle_pro','bundle_enterprise','pro','basic','enterprise','admin'];

// ── list ─────────────────────────────────────────────────────────────────────
if ($action === 'list') {
    $offset = max(0, (int)($_REQUEST['offset'] ?? 0));
    $limit  = min(200, max(1, (int)($_REQUEST['limit'] ?? 100)));
    $search = trim($_REQUEST['search'] ?? '');

    $where = 'u.id > 62';
    $params = [];
    if ($search !== '') {
        $where .= " AND (u.name LIKE :s OR u.username LIKE :s OR u.email LIKE :s)";
        $params[':s'] = '%' . $search . '%';
    }

    $sql = "
        SELECT
            u.id, u.name, u.username, u.email, u.block, u.registerDate, u.lastvisitDate,
            l.plan_type, l.is_active, l.expires_at,
            l.stripe_status, l.stripe_customer_id, l.mac_address
        FROM " . TBL_PFX . "users u
        LEFT JOIN " . TBL_PFX . "cellfire_licenses l ON l.user_id = u.id
        WHERE $where
        ORDER BY u.id DESC
        LIMIT :lim OFFSET :off
    ";
    $stmt = $pdo->prepare($sql);
    foreach ($params as $k => $v) $stmt->bindValue($k, $v);
    $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $csql  = "SELECT COUNT(*) FROM " . TBL_PFX . "users u WHERE $where";
    $cstmt = $pdo->prepare($csql);
    foreach ($params as $k => $v) $cstmt->bindValue($k, $v);
    $cstmt->execute();
    $total = (int)$cstmt->fetchColumn();

    echo json_encode(['ok' => true, 'users' => $rows, 'total' => $total,
                      'offset' => $offset, 'limit' => $limit]);
    exit;
}

// ── get_user ──────────────────────────────────────────────────────────────────
if ($action === 'get_user') {
    $uid = (int)($_REQUEST['user_id'] ?? 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    $stmt = $pdo->prepare("
        SELECT u.id, u.name, u.username, u.email, u.block, u.registerDate, u.lastvisitDate,
               l.plan_type, l.is_active, l.expires_at,
               l.stripe_status, l.stripe_customer_id, l.mac_address
        FROM " . TBL_PFX . "users u
        LEFT JOIN " . TBL_PFX . "cellfire_licenses l ON l.user_id = u.id
        WHERE u.id = ?
    ");
    $stmt->execute([$uid]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$row) { echo json_encode(['ok'=>false,'error'=>'User not found']); exit; }
    echo json_encode(['ok'=>true,'user'=>$row]);
    exit;
}

// ── create_user ───────────────────────────────────────────────────────────────
if ($action === 'create_user') {
    $name     = trim($_REQUEST['name']     ?? '');
    $username = trim($_REQUEST['username'] ?? '');
    $email    = trim($_REQUEST['email']    ?? '');
    $password = trim($_REQUEST['password'] ?? '');
    $plan     = trim($_REQUEST['plan']     ?? 'demo');
    $expires  = trim($_REQUEST['expires_at'] ?? '');
    $active   = (int)($_REQUEST['is_active'] ?? 1);

    if (!$name || !$username || !$email || !$password) {
        echo json_encode(['ok'=>false,'error'=>'Name, username, email, and password are required']); exit;
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode(['ok'=>false,'error'=>'Invalid email address']); exit;
    }
    if (!in_array($plan, $ALLOWED_PLANS, true)) {
        echo json_encode(['ok'=>false,'error'=>'Invalid plan']); exit;
    }

    // Check uniqueness
    $dup = $pdo->prepare("SELECT id FROM ".TBL_PFX."users WHERE username=? OR email=? LIMIT 1");
    $dup->execute([$username, $email]);
    if ($dup->fetch()) {
        echo json_encode(['ok'=>false,'error'=>'Username or email already exists']); exit;
    }

    $hash = password_hash($password, PASSWORD_BCRYPT);
    $now  = date('Y-m-d H:i:s');

    $pdo->prepare("
        INSERT INTO ".TBL_PFX."users
            (name, username, email, password, block, sendEmail, registerDate,
             lastvisitDate, activation, params, lastResetTime, resetCount,
             otpKey, otep, requireReset, authProvider)
        VALUES (?,?,?,?,0,0,?,NULL,'','',NULL,0,'','',0,'joomla')
    ")->execute([$name, $username, $email, $hash, $now]);

    $uid = (int)$pdo->lastInsertId();

    // Add to Registered group (group_id = 2)
    $pdo->prepare("INSERT IGNORE INTO ".TBL_PFX."user_usergroup_map (user_id, group_id) VALUES (?,2)")
        ->execute([$uid]);

    // Create license
    $exp = ($expires !== '') ? $expires : ($plan === 'demo' ? date('Y-m-d H:i:s', strtotime('+30 days')) : null);
    $pdo->prepare("
        INSERT INTO ".TBL_PFX."cellfire_licenses (user_id, plan_type, is_active, expires_at)
        VALUES (?, ?, ?, ?)
    ")->execute([$uid, $plan, $active, $exp]);

    echo json_encode(['ok'=>true,'user_id'=>$uid,'message'=>"User '$username' created (ID $uid)"]);
    exit;
}

// ── update_user ───────────────────────────────────────────────────────────────
if ($action === 'update_user') {
    $uid = (int)($_REQUEST['user_id'] ?? 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }

    // Update josbf_users fields
    $uFields = []; $uParams = [];
    foreach (['name'=>'name','username'=>'username','email'=>'email'] as $req => $col) {
        if (isset($_REQUEST[$req]) && trim($_REQUEST[$req]) !== '') {
            $uFields[] = "$col = ?";
            $uParams[] = trim($_REQUEST[$req]);
        }
    }
    // Optional password change
    if (!empty($_REQUEST['password'])) {
        $uFields[] = 'password = ?';
        $uParams[] = password_hash(trim($_REQUEST['password']), PASSWORD_BCRYPT);
    }
    // block
    if (isset($_REQUEST['block'])) {
        $uFields[] = 'block = ?';
        $uParams[] = (int)$_REQUEST['block'];
    }
    if ($uFields) {
        $uParams[] = $uid;
        $pdo->prepare("UPDATE ".TBL_PFX."users SET ".implode(', ', $uFields)." WHERE id = ?")
            ->execute($uParams);
    }

    // Upsert license
    $plan    = isset($_REQUEST['plan'])      ? trim($_REQUEST['plan'])      : null;
    $active  = isset($_REQUEST['is_active']) ? (int)$_REQUEST['is_active'] : null;
    $expires = isset($_REQUEST['expires_at'])? trim($_REQUEST['expires_at']): null;
    $stripe  = isset($_REQUEST['stripe_customer_id']) ? trim($_REQUEST['stripe_customer_id']) : null;

    if ($plan !== null || $active !== null || $expires !== null || $stripe !== null) {
        if ($plan !== null && !in_array($plan, $ALLOWED_PLANS, true)) {
            echo json_encode(['ok'=>false,'error'=>'Invalid plan']); exit;
        }
        // Build upsert
        $lCols = ['user_id']; $lVals = [$uid]; $lUpdate = [];
        if ($plan   !== null) { $lCols[]='plan_type';          $lVals[]=$plan;   $lUpdate[]='plan_type=VALUES(plan_type)'; }
        if ($active !== null) { $lCols[]='is_active';          $lVals[]=$active; $lUpdate[]='is_active=VALUES(is_active)'; }
        if ($expires !== null){ $lCols[]='expires_at';         $lVals[]= ($expires==='' ? null : $expires); $lUpdate[]='expires_at=VALUES(expires_at)'; }
        if ($stripe !== null) { $lCols[]='stripe_customer_id'; $lVals[]= ($stripe==='' ? null : $stripe);   $lUpdate[]='stripe_customer_id=VALUES(stripe_customer_id)'; }

        $placeholders = implode(',', array_fill(0, count($lVals), '?'));
        $cols         = implode(',', $lCols);
        $updates      = implode(',', $lUpdate);
        $pdo->prepare("INSERT INTO ".TBL_PFX."cellfire_licenses ($cols) VALUES ($placeholders)
                       ON DUPLICATE KEY UPDATE $updates")
            ->execute($lVals);
    }

    echo json_encode(['ok'=>true,'message'=>'User updated']);
    exit;
}

// ── toggle_active ─────────────────────────────────────────────────────────────
if ($action === 'toggle_active') {
    $uid = (int)($_REQUEST['user_id'] ?? 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    $pdo->prepare("UPDATE ".TBL_PFX."cellfire_licenses SET is_active = 1 - is_active WHERE user_id = ?")
        ->execute([$uid]);
    $row = $pdo->prepare("SELECT is_active FROM ".TBL_PFX."cellfire_licenses WHERE user_id = ?");
    $row->execute([$uid]);
    echo json_encode(['ok'=>true,'is_active'=>(int)$row->fetchColumn()]);
    exit;
}

// ── set_plan ──────────────────────────────────────────────────────────────────
if ($action === 'set_plan') {
    $uid  = (int)($_REQUEST['user_id'] ?? 0);
    $plan = trim($_REQUEST['plan'] ?? '');
    if (!$uid || !in_array($plan, $ALLOWED_PLANS, true)) {
        echo json_encode(['ok'=>false,'error'=>'Invalid user_id or plan']); exit;
    }
    $pdo->prepare("
        INSERT INTO ".TBL_PFX."cellfire_licenses (user_id, plan_type, is_active)
        VALUES (?, ?, 1)
        ON DUPLICATE KEY UPDATE plan_type = VALUES(plan_type), is_active = 1
    ")->execute([$uid, $plan]);
    echo json_encode(['ok'=>true,'plan_type'=>$plan]);
    exit;
}

// ── extend_trial ──────────────────────────────────────────────────────────────
if ($action === 'extend_trial') {
    $uid  = (int)($_REQUEST['user_id'] ?? 0);
    $days = max(1, min(365, (int)($_REQUEST['days'] ?? 30)));
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    $pdo->prepare("
        UPDATE ".TBL_PFX."cellfire_licenses
        SET expires_at = DATE_ADD(NOW(), INTERVAL ? DAY), is_active = 1
        WHERE user_id = ?
    ")->execute([$days, $uid]);
    echo json_encode(['ok'=>true,'days'=>$days]);
    exit;
}

// ── block_user ────────────────────────────────────────────────────────────────
if ($action === 'block_user') {
    $uid   = (int)($_REQUEST['user_id'] ?? 0);
    $block = (int)(($_REQUEST['block'] ?? 1) ? 1 : 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    $pdo->prepare("UPDATE ".TBL_PFX."users SET block = ? WHERE id = ?")->execute([$block, $uid]);
    echo json_encode(['ok'=>true,'block'=>$block]);
    exit;
}

// ── clear_mac ─────────────────────────────────────────────────────────────────
if ($action === 'clear_mac') {
    $uid = (int)($_REQUEST['user_id'] ?? 0);
    if (!$uid) { echo json_encode(['ok'=>false,'error'=>'Missing user_id']); exit; }
    $pdo->prepare("UPDATE ".TBL_PFX."cellfire_licenses SET mac_address = NULL WHERE user_id = ?")
        ->execute([$uid]);
    echo json_encode(['ok'=>true,'message'=>'Device MAC address cleared']);
    exit;
}

echo json_encode(['ok'=>false,'error'=>'Unknown action: '.$action]);
