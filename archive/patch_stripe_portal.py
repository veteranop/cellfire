"""Patch stripe helper + controller to add portal() support."""
import urllib.request, urllib.parse, ssl, json, re

cfg = open('C:/Users/markd/StudioProjects/cellfire/push_rules_config.py').read()
m = re.search(r'API_TOKEN\s*=\s*[\"\'](.*?)[\"\']\s*$', cfg, re.MULTILINE)
tok = m.group(1) if m else ''
CPANEL_USER = 'veterap2'; CPANEL_HOST = 'cellfire.io'; CPANEL_PORT = 2083

ctx = ssl.create_default_context()
ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE

def get(path):
    url = f'https://{CPANEL_HOST}:{CPANEL_PORT}/execute/Fileman/get_file_content'
    params = urllib.parse.urlencode({'dir': path.rsplit('/',1)[0], 'file': path.rsplit('/',1)[1]})
    req = urllib.request.Request(f'{url}?{params}')
    req.add_header('Authorization', f'cpanel {CPANEL_USER}:{tok}')
    return json.loads(urllib.request.urlopen(req, context=ctx, timeout=20).read()).get('data',{}).get('content','')

def put(path, content):
    url = f'https://{CPANEL_HOST}:{CPANEL_PORT}/execute/Fileman/save_file_content'
    body = urllib.parse.urlencode({'dir': path.rsplit('/',1)[0], 'file': path.rsplit('/',1)[1], 'content': content}).encode()
    req = urllib.request.Request(url, data=body)
    req.add_header('Authorization', f'cpanel {CPANEL_USER}:{tok}')
    return json.loads(urllib.request.urlopen(req, context=ctx, timeout=30).read()).get('status')

comp = '/home1/veterap2/public_html/website_9695cd55/components/com_cellfireapi'

# ── 1. Add createPortalSession to stripe helper ──────────────────────────────
helper = get(f'{comp}/helpers/stripe.php')
if 'createPortalSession' not in helper:
    portal_session_method = (
        '\n'
        '    /**\n'
        '     * Create a Stripe Customer Portal session.\n'
        '     *\n'
        "     * @param  array $params  Must include 'customer' (cus_...) and 'return_url'\n"
        '     * @return array          Decoded session object with url\n'
        '     */\n'
        '    public static function createPortalSession(array $params): array\n'
        '    {\n'
        "        return self::request('POST', '/billing_portal/sessions', $params);\n"
        '    }\n'
        '\n'
        '    /**\n'
        '     * Verify a Stripe webhook'
    )
    helper_patched = helper.replace(
        '    /**\n     * Verify a Stripe webhook',
        portal_session_method
    )
    r = put(f'{comp}/helpers/stripe.php', helper_patched)
    print(f'stripe helper createPortalSession: {"OK" if r==1 else "FAIL"}')
else:
    print('stripe helper: createPortalSession already exists')

# ── 2. Add portal() to stripe controller ────────────────────────────────────
ctrl = get(f'{comp}/controllers/stripe.php')

# Add JWT helper require if missing
if "helpers/jwt.php'" not in ctrl:
    ctrl = ctrl.replace(
        "require_once __DIR__ . '/../helpers/stripe.php';",
        "require_once __DIR__ . '/../helpers/stripe.php';\nrequire_once __DIR__ . '/../helpers/jwt.php';"
    )

portal_php = open('C:/Users/markd/StudioProjects/cellfire/portal_method.php', 'r', encoding='utf-8').read()

if 'public function portal()' not in ctrl:
    # Insert before the webhook method
    ctrl = ctrl.replace('    public function webhook(): void', portal_php + '\n    public function webhook(): void')
    r = put(f'{comp}/controllers/stripe.php', ctrl)
    print(f'stripe controller portal(): {"OK" if r==1 else "FAIL"}')
else:
    print('stripe controller: portal() already exists')

# ── 3. Update cf_account.php ─────────────────────────────────────────────────
tpl = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article'
account_php = open('C:/Users/markd/StudioProjects/cellfire/cf_account_new.php', 'r', encoding='utf-8').read()
r = put(f'{tpl}/cf_account.php', account_php)
print(f'cf_account.php: {"OK" if r==1 else "FAIL"}')
