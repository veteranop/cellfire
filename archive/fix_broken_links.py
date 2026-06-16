#!/usr/bin/env python3
"""Fix broken links across cellfire.io template files."""
import urllib.request, urllib.parse, json, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

ARTICLE_DIR = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article'
TEMPLATE_DIR = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire'
AUTH = 'cpanel veterap2:DD59L2BQRDZNLXU45RUOBJWIPQBV3RMA'
HOST = 'https://veteranop.com:2083'


def cpanel_get(base_dir, fname):
    params = urllib.parse.urlencode({'dir': base_dir, 'file': fname})
    url = f'{HOST}/execute/Fileman/get_file_content?{params}'
    req = urllib.request.Request(url, headers={'Authorization': AUTH})
    with urllib.request.urlopen(req, context=ctx) as r:
        d = json.loads(r.read())
        return (d.get('data') or {}).get('content', '')


def cpanel_put(base_dir, fname, content):
    body = urllib.parse.urlencode({
        'dir': base_dir,
        'file': fname,
        'content': content,
    }).encode('utf-8')
    url = f'{HOST}/execute/Fileman/save_file_content'
    req = urllib.request.Request(url, data=body, headers={
        'Authorization': AUTH,
        'Content-Type': 'application/x-www-form-urlencoded',
    }, method='POST')
    with urllib.request.urlopen(req, context=ctx) as r:
        return json.loads(r.read())


def fix_index_php(content):
    """Fix Privacy Policy and Terms of Service links in footer."""
    content = content.replace(
        '<a href="#">Privacy Policy</a>',
        '<a href="/privacy">Privacy Policy</a>'
    )
    content = content.replace(
        '<a href="#">Terms of Service</a>',
        '<a href="/terms">Terms of Service</a>'
    )
    return content


def fix_android_php(content):
    """Fix Google Play links and update version badge."""
    # Update version badge: v1.0.0.4 → v1.0.0.6
    content = content.replace(
        '<span class="cf-android-badge">v1.0.0.4 Stable</span>',
        '<span class="cf-android-badge">v1.0.0.6 Stable</span>'
    )

    # Fix Google Play href="#" → /downloads (placeholder until Play Store URL known)
    # There are two occurrences - one in hero, one in lower CTA
    content = re.sub(
        r'<a href="#" class="cf-btn cf-btn--primary">\s*<svg[^>]*>.*?</svg>\s*Get it on Google Play\s*</a>',
        lambda m: m.group(0).replace('href="#"', 'href="/downloads"'),
        content, flags=re.DOTALL
    )
    content = re.sub(
        r'<a href="#" class="cf-btn cf-btn--primary">\s*<svg[^>]*>.*?</svg>\s*Google Play\s*</a>',
        lambda m: m.group(0).replace('href="#"', 'href="/downloads"'),
        content, flags=re.DOTALL
    )
    return content


def fix_account_php(content):
    """Update Manage Billing link to Stripe billing portal endpoint."""
    content = content.replace(
        '<a href="#" class="cf-btn cf-btn--ghost" style="font-size:13px;">Manage Billing →</a>',
        '<a href="/stripe-billing" class="cf-btn cf-btn--ghost" style="font-size:13px;">Manage Billing →</a>'
    )
    return content


def main():
    # Fix index.php (template root)
    print("Fixing index.php footer links...")
    content = cpanel_get(TEMPLATE_DIR, 'index.php')
    updated = fix_index_php(content)
    changes = (
        ('href="#">Privacy Policy' in content and 'href="/privacy">Privacy Policy' in updated),
        ('href="#">Terms of Service' in content and 'href="/terms">Terms of Service' in updated),
    )
    print(f"  Privacy Policy link fixed: {changes[0]}")
    print(f"  Terms of Service link fixed: {changes[1]}")
    result = cpanel_put(TEMPLATE_DIR, 'index.php', updated)
    print(f"  Upload: {'OK' if result.get('status') == 1 else result.get('errors')}")

    # Fix cf_android.php
    print("\nFixing cf_android.php...")
    content = cpanel_get(ARTICLE_DIR, 'cf_android.php')
    updated = fix_android_php(content)
    print(f"  Version badge updated: {'v1.0.0.6 Stable' in updated}")
    # Count Google Play link fixes
    old_count = content.count('href="#" class="cf-btn cf-btn--primary"')
    new_count = updated.count('href="#" class="cf-btn cf-btn--primary"')
    print(f"  Google Play href=# fixed: {old_count - new_count} of {old_count}")
    result = cpanel_put(ARTICLE_DIR, 'cf_android.php', updated)
    print(f"  Upload: {'OK' if result.get('status') == 1 else result.get('errors')}")

    # Fix cf_account.php
    print("\nFixing cf_account.php billing link...")
    content = cpanel_get(ARTICLE_DIR, 'cf_account.php')
    updated = fix_account_php(content)
    print(f"  Manage Billing link updated: {'/stripe-billing' in updated}")
    result = cpanel_put(ARTICLE_DIR, 'cf_account.php', updated)
    print(f"  Upload: {'OK' if result.get('status') == 1 else result.get('errors')}")

    print("\nDone!")


if __name__ == '__main__':
    main()
