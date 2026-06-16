#!/usr/bin/env python3
"""Update checkout_urls in cf_pricing.php with real Stripe payment links."""
import urllib.request, urllib.parse, json, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

BASE_DIR = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article'
AUTH = 'cpanel veterap2:DD59L2BQRDZNLXU45RUOBJWIPQBV3RMA'
HOST = 'https://veteranop.com:2083'

def cpanel_get_file(fname):
    params = urllib.parse.urlencode({'dir': BASE_DIR, 'file': fname})
    url = f'{HOST}/execute/Fileman/get_file_content?{params}'
    req = urllib.request.Request(url, headers={'Authorization': AUTH})
    with urllib.request.urlopen(req, context=ctx) as r:
        d = json.loads(r.read())
        return (d.get('data') or {}).get('content', '')

def cpanel_put_file(fname, content):
    """Upload file content via cPanel Fileman save_file_content API."""
    body = urllib.parse.urlencode({
        'dir': BASE_DIR,
        'file': fname,
        'content': content,
    }).encode('utf-8')
    url = f'{HOST}/execute/Fileman/save_file_content'
    req = urllib.request.Request(
        url, data=body,
        headers={
            'Authorization': AUTH,
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        method='POST'
    )
    with urllib.request.urlopen(req, context=ctx) as r:
        return json.loads(r.read())

# ── Payment link map ────────────────────────────────────────────────────────
# Keyed by CTA text (unique per plan) or by price for disambiguation

CTA_MAP = {
    'Get View Only':  'https://buy.stripe.com/14AbJ06QS6kTcAL14v1sQ00',
    'Get Team Access':'https://buy.stripe.com/7sY9AScbcaB9gR100r1sQ02',
    'Get Standard':   'https://buy.stripe.com/7sYdR8cbcfVtfMX7sT1sQ03',
    'Contact Sales':  'https://buy.stripe.com/bJeaEWcbc10zgR19B11sQ05',
    'Get Basic':      'https://buy.stripe.com/aFa00i3EG24D58j14v1sQ06',
    'Get Pro':        'https://buy.stripe.com/6oUeVc4IK10z30bdRh1sQ07',
    'Get Enterprise': 'https://buy.stripe.com/bJeeVc6QS4cLcAL5kL1sQ08',
}

# "Get Full Access" appears for both App Full ($10) and Studio Full ($60)
# so use price-based regex
PRICE_MAP = {
    '10':  'https://buy.stripe.com/fZubJ0dfg10z6cn5kL1sQ01',
    '60':  'https://buy.stripe.com/dRm7sKb7810z7gr14v1sQ04',
    '30':  'https://buy.stripe.com/8x200iejk38HgR1eVl1sQ09',
    '69':  'https://buy.stripe.com/6oUdR8ejkeRp1W7aF51sQ0a',
    '299': 'https://buy.stripe.com/cNieVca349x5eITeVl1sQ0b',
}

def fix_checkout_urls(content):
    # Replace by CTA (unique ones)
    for cta, url in CTA_MAP.items():
        pattern = r"('checkout_url'\s*=>\s*)'#'(,\s*'cta'\s*=>\s*'" + re.escape(cta) + r"')"
        replacement = r"\g<1>'" + url + r"'\2"
        content = re.sub(pattern, replacement, content)

    # Replace by price (for ambiguous CTAs and bundles)
    for price, url in PRICE_MAP.items():
        pattern = r"('price'\s*=>\s*'" + re.escape(price) + r"'.*?'checkout_url'\s*=>\s*)'#'"
        replacement = r"\g<1>'" + url + "'"
        content = re.sub(pattern, replacement, content, flags=re.DOTALL)

    return content

def main():
    print("Reading cf_pricing.php...")
    content = cpanel_get_file('cf_pricing.php')
    print(f"  Read {len(content)} bytes")

    before = content.count("'checkout_url' => '#'")
    print(f"  Found {before} placeholder checkout_urls to replace")

    updated = fix_checkout_urls(content)

    after = updated.count("'checkout_url' => '#'")
    print(f"  After replacement: {after} placeholders remaining")

    if after == 0:
        print("  All URLs replaced successfully!")
    else:
        print("  WARNING: Some URLs were not replaced")
        for line in updated.split('\n'):
            if "checkout_url" in line:
                print("   ", line.strip())

    print("Uploading cf_pricing.php...")
    result = cpanel_put_file('cf_pricing.php', updated)
    print(f"  Upload result: {result}")

if __name__ == '__main__':
    main()
