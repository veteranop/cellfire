"""Upload updated cf_pricing.php with price_id fields and checkout JS."""
import urllib.request, urllib.parse, ssl, json, re

cfg = open('C:/Users/markd/StudioProjects/cellfire/push_rules_config.py').read()
m = re.search(r'API_TOKEN\s*=\s*[\"\'](.*?)[\"\']\s*$', cfg, re.MULTILINE)
tok = m.group(1) if m else ''
CPANEL_USER = 'veterap2'
CPANEL_HOST = 'cellfire.io'
CPANEL_PORT = 2083
TPL = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article'

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

content = open('C:/Users/markd/StudioProjects/cellfire/cf_pricing_new.php', 'r', encoding='utf-8').read()

url = f'https://{CPANEL_HOST}:{CPANEL_PORT}/execute/Fileman/save_file_content'
body = urllib.parse.urlencode({'dir': TPL, 'file': 'cf_pricing.php', 'content': content}).encode()
req = urllib.request.Request(url, data=body)
req.add_header('Authorization', f'cpanel {CPANEL_USER}:{tok}')
resp = urllib.request.urlopen(req, context=ctx, timeout=60)
data = json.loads(resp.read())
print(f'Upload: {"OK" if data.get("status")==1 else "FAIL"} — {data.get("errors","") or "no errors"}')
