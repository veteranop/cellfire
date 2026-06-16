#!/usr/bin/env python3
"""Create cf_privacy.php and cf_terms.php layout files on the server,
then create the Joomla articles via the REST API."""
import urllib.request, urllib.parse, json, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

ARTICLE_DIR = '/home1/veterap2/public_html/website_9695cd55/templates/cellfire/html/com_content/article'
AUTH = 'cpanel veterap2:DD59L2BQRDZNLXU45RUOBJWIPQBV3RMA'
HOST = 'https://veteranop.com:2083'


def cpanel_put(base_dir, fname, content):
    body = urllib.parse.urlencode({'dir': base_dir, 'file': fname, 'content': content}).encode('utf-8')
    req = urllib.request.Request(f'{HOST}/execute/Fileman/save_file_content', data=body, headers={
        'Authorization': AUTH, 'Content-Type': 'application/x-www-form-urlencoded',
    }, method='POST')
    with urllib.request.urlopen(req, context=ctx) as r:
        return json.loads(r.read())


PRIVACY_PHP = r"""<?php
/**
 * Cellfire Template — Article Layout: Privacy Policy
 * Assign to an article with alias "privacy"
 */
defined('_JEXEC') or die;
$updated = 'March 24, 2026';
?>
<section class="cf-legal-section">
<div class="cf-container" style="max-width:860px; padding:60px 24px 100px;">

  <div class="cf-legal-header">
    <h1 class="cf-section-title">Privacy <span class="cf-gradient-text">Policy</span></h1>
    <p class="cf-section-sub">Last updated: <?= $updated ?></p>
  </div>

  <div class="cf-legal-body">

    <h2>1. Overview</h2>
    <p>Cellfire (&ldquo;we,&rdquo; &ldquo;our,&rdquo; or &ldquo;us&rdquo;) is a product of VeteranOp, LLC.
    This Privacy Policy describes how we collect, use, and protect information when you use
    the Cellfire mobile app, RF Studio desktop application, Cellfire Web Viewer, and the
    cellfire.io website (collectively, the &ldquo;Services&rdquo;).</p>
    <p>By using our Services you agree to the collection and use of information as described here.</p>

    <h2>2. Information We Collect</h2>
    <h3>Account Information</h3>
    <p>When you register, we collect your <strong>username</strong>, <strong>email address</strong>,
    and a hashed password. This data is stored securely in Google Firebase Authentication.</p>

    <h3>Signal &amp; Location Data</h3>
    <p>The Cellfire app collects <strong>cellular signal measurements</strong> (RSRP, RSRQ, SINR,
    ARFCN, PCI, band, carrier) and, when GPS drive-test mode is active,
    <strong>GPS coordinates</strong> tied to those measurements. This data is used to power
    drive-test maps, coverage analysis, and the crowdsourced signal database.</p>
    <p>You control when GPS logging is active. Location data is only collected while the app
    is in the foreground and drive-test recording is enabled.</p>

    <h3>Usage Data</h3>
    <p>We collect basic app usage analytics (feature interactions, error reports, session duration)
    to improve the Services. This data is anonymized and aggregated.</p>

    <h3>Payment Information</h3>
    <p>Payments are processed by <strong>Stripe, Inc.</strong> We do not store your credit card
    number, CVV, or full payment details on our servers. Stripe may collect and store payment
    information in accordance with their own
    <a href="https://stripe.com/privacy" target="_blank" rel="noopener">privacy policy</a>.</p>

    <h2>3. How We Use Your Information</h2>
    <ul>
      <li>Provide and operate the Services</li>
      <li>Authenticate your account and manage subscriptions</li>
      <li>Generate drive-test maps and coverage visualizations</li>
      <li>Improve signal data accuracy and coverage of the crowdsourced database</li>
      <li>Send transactional emails (account confirmation, billing receipts)</li>
      <li>Respond to support requests</li>
      <li>Detect and prevent fraud or abuse</li>
    </ul>
    <p>We do <strong>not</strong> sell your personal data to third parties.</p>

    <h2>4. Data Sharing</h2>
    <p>We share data only with service providers necessary to operate the Services:</p>
    <ul>
      <li><strong>Google Firebase</strong> — authentication and realtime database</li>
      <li><strong>Stripe</strong> — payment processing and subscription management</li>
      <li><strong>JustHost / Liquid Web</strong> — web hosting infrastructure</li>
    </ul>
    <p>We may disclose information if required by law or to protect the rights and safety
    of VeteranOp, LLC, our users, or the public.</p>

    <h2>5. Data Retention</h2>
    <p>Account data is retained for as long as your account is active. Signal and location
    data contributed to the crowdsourced database is retained indefinitely in anonymized,
    aggregated form. You may request deletion of your account and associated personal data
    at any time by emailing <a href="mailto:privacy@cellfire.io">privacy@cellfire.io</a>.</p>
    <p>The Cellfire Web Viewer does <strong>not</strong> retain uploaded CSV data — files are
    processed in memory and discarded immediately after rendering.</p>

    <h2>6. Security</h2>
    <p>We use industry-standard measures to protect your data, including TLS encryption
    in transit and Firebase security rules for data at rest. No method of transmission
    over the Internet is 100% secure; we cannot guarantee absolute security.</p>

    <h2>7. Children&rsquo;s Privacy</h2>
    <p>The Services are not directed to children under 13. We do not knowingly collect
    personal information from children under 13. If you believe a child has provided us
    personal information, contact us and we will delete it promptly.</p>

    <h2>8. Your Rights</h2>
    <p>Depending on your location, you may have rights to access, correct, or delete your
    personal data, or to object to or restrict certain processing. To exercise these rights,
    contact <a href="mailto:privacy@cellfire.io">privacy@cellfire.io</a>.</p>

    <h2>9. Changes to This Policy</h2>
    <p>We may update this Privacy Policy from time to time. We will post the revised policy
    on this page with an updated &ldquo;Last updated&rdquo; date. Continued use of the
    Services after changes constitutes acceptance of the updated policy.</p>

    <h2>10. Contact Us</h2>
    <p>Questions about this Privacy Policy? Contact us at:<br>
    <strong>VeteranOp, LLC</strong><br>
    <a href="mailto:privacy@cellfire.io">privacy@cellfire.io</a></p>

  </div><!-- /.cf-legal-body -->
</div>
</section>

<style>
.cf-legal-section { background: var(--cf-bg-1, #111); color: var(--cf-text-1, #eee); }
.cf-legal-header  { margin-bottom: 48px; }
.cf-legal-body h2 {
  font-size: 1.25rem; font-weight: 700; color: #fff;
  margin: 2.5rem 0 .75rem; border-bottom: 1px solid #2a2a2a; padding-bottom: .5rem;
}
.cf-legal-body h3 { font-size: 1rem; font-weight: 600; color: #ccc; margin: 1.5rem 0 .5rem; }
.cf-legal-body p, .cf-legal-body li { color: #aaa; line-height: 1.8; margin-bottom: .75rem; }
.cf-legal-body ul { padding-left: 1.5rem; margin-bottom: 1rem; }
.cf-legal-body a  { color: #4fc3f7; text-decoration: none; }
.cf-legal-body a:hover { text-decoration: underline; }
</style>
"""

TERMS_PHP = r"""<?php
/**
 * Cellfire Template — Article Layout: Terms of Service
 * Assign to an article with alias "terms"
 */
defined('_JEXEC') or die;
$updated = 'March 24, 2026';
?>
<section class="cf-legal-section">
<div class="cf-container" style="max-width:860px; padding:60px 24px 100px;">

  <div class="cf-legal-header">
    <h1 class="cf-section-title">Terms of <span class="cf-gradient-text">Service</span></h1>
    <p class="cf-section-sub">Last updated: <?= $updated ?></p>
  </div>

  <div class="cf-legal-body">

    <h2>1. Acceptance of Terms</h2>
    <p>By accessing or using any Cellfire product or service — including the Cellfire mobile app,
    RF Studio, Cellfire Web Viewer, or cellfire.io (collectively, the &ldquo;Services&rdquo;) —
    you agree to be bound by these Terms of Service (&ldquo;Terms&rdquo;) and our
    <a href="/privacy">Privacy Policy</a>. If you do not agree, do not use the Services.</p>
    <p>The Services are operated by <strong>VeteranOp, LLC</strong>, an Idaho limited liability company.</p>

    <h2>2. Eligibility</h2>
    <p>You must be at least 18 years old and capable of entering a binding contract to use the Services.
    By using the Services you represent that you meet these requirements.</p>

    <h2>3. Account Registration</h2>
    <p>You must create an account to access most features. You agree to provide accurate information
    and keep your credentials confidential. You are responsible for all activity under your account.
    Notify us immediately at <a href="mailto:support@cellfire.io">support@cellfire.io</a> if you
    suspect unauthorized access.</p>
    <p>We reserve the right to suspend or terminate accounts that violate these Terms.</p>

    <h2>4. Subscriptions &amp; Billing</h2>
    <h3>Free Trial</h3>
    <p>New accounts receive a <strong>30-day free trial</strong> with full access to all features.
    No credit card is required for the trial. After 30 days your account reverts to read-only
    unless you subscribe to a paid plan.</p>

    <h3>Paid Plans</h3>
    <p>Paid subscriptions are billed monthly. All prices are in USD. Subscriptions automatically
    renew each billing period unless cancelled. You may cancel at any time through
    <a href="/account">your account page</a>; cancellation takes effect at the end of the
    current billing period — no partial refunds are issued for unused time.</p>

    <h3>Payment Processing</h3>
    <p>Payments are processed by Stripe, Inc. By subscribing you agree to
    <a href="https://stripe.com/legal" target="_blank" rel="noopener">Stripe&rsquo;s terms</a>.
    We reserve the right to change pricing with 30 days&rsquo; notice.</p>

    <h3>Refunds</h3>
    <p>Subscription fees are generally non-refundable. If you believe you were charged in error,
    contact <a href="mailto:billing@cellfire.io">billing@cellfire.io</a> within 14 days of the charge.</p>

    <h2>5. Acceptable Use</h2>
    <p>You agree <strong>not</strong> to:</p>
    <ul>
      <li>Use the Services for any unlawful purpose or in violation of any regulation</li>
      <li>Interfere with or disrupt the Services or servers connected to them</li>
      <li>Attempt to gain unauthorized access to any part of the Services</li>
      <li>Reverse engineer, decompile, or disassemble any part of the Services</li>
      <li>Use the Services to collect signal data in areas where you do not have permission to be</li>
      <li>Resell or sublicense the Services without written permission from VeteranOp, LLC</li>
      <li>Upload malicious code or interfere with other users&rsquo; use of the Services</li>
    </ul>
    <p>Signal data collected using the Cellfire app is subject to all applicable FCC regulations
    and local laws. You are solely responsible for ensuring your use of the Services complies
    with applicable law in your jurisdiction.</p>

    <h2>6. Intellectual Property</h2>
    <p>All content, software, trademarks, and data in the Services are the property of
    VeteranOp, LLC or its licensors. You may not copy, modify, distribute, or create derivative
    works without our written consent.</p>
    <p>You retain ownership of signal data you collect. By uploading data to Cellfire you grant
    VeteranOp, LLC a non-exclusive, royalty-free license to use that data in anonymized,
    aggregated form to improve the Services and the crowdsourced signal database.</p>

    <h2>7. Signal Data Accuracy</h2>
    <p>Cellfire provides cellular RF signal data as-is for informational and engineering purposes.
    <strong>We make no warranty</strong> regarding the accuracy, completeness, or fitness for any
    particular purpose of signal data, coverage maps, or propagation models generated by the Services.
    Signal data should not be used as the sole basis for critical infrastructure, life-safety, or
    regulatory compliance decisions.</p>

    <h2>8. Disclaimer of Warranties</h2>
    <p>THE SERVICES ARE PROVIDED &ldquo;AS IS&rdquo; AND &ldquo;AS AVAILABLE&rdquo; WITHOUT
    WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. WE DO NOT WARRANT
    THAT THE SERVICES WILL BE UNINTERRUPTED, ERROR-FREE, OR FREE OF VIRUSES OR OTHER HARMFUL COMPONENTS.</p>

    <h2>9. Limitation of Liability</h2>
    <p>TO THE MAXIMUM EXTENT PERMITTED BY LAW, VETERANOP, LLC AND ITS OFFICERS, EMPLOYEES, AND
    AGENTS SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE
    DAMAGES ARISING OUT OF YOUR USE OF THE SERVICES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
    DAMAGES. OUR TOTAL LIABILITY TO YOU FOR ANY CLAIM ARISING OUT OF THESE TERMS SHALL NOT EXCEED
    THE AMOUNT YOU PAID US IN THE THREE MONTHS PRECEDING THE CLAIM.</p>

    <h2>10. Indemnification</h2>
    <p>You agree to indemnify and hold harmless VeteranOp, LLC from any claims, losses, liabilities,
    damages, and expenses (including reasonable attorneys&rsquo; fees) arising out of your use of
    the Services, violation of these Terms, or violation of any third-party rights.</p>

    <h2>11. Governing Law &amp; Disputes</h2>
    <p>These Terms are governed by the laws of the State of Idaho, without regard to conflict-of-law
    principles. Any dispute arising under these Terms shall be resolved by binding arbitration in
    Ada County, Idaho, under the rules of the American Arbitration Association, except that either
    party may seek injunctive relief in any court of competent jurisdiction.</p>

    <h2>12. Changes to Terms</h2>
    <p>We may modify these Terms at any time. We will provide at least 14 days&rsquo; notice of
    material changes by posting on this page or emailing registered users. Continued use of the
    Services after the effective date constitutes acceptance.</p>

    <h2>13. Contact</h2>
    <p><strong>VeteranOp, LLC</strong><br>
    <a href="mailto:legal@cellfire.io">legal@cellfire.io</a></p>

  </div><!-- /.cf-legal-body -->
</div>
</section>

<style>
.cf-legal-section { background: var(--cf-bg-1, #111); color: var(--cf-text-1, #eee); }
.cf-legal-header  { margin-bottom: 48px; }
.cf-legal-body h2 {
  font-size: 1.25rem; font-weight: 700; color: #fff;
  margin: 2.5rem 0 .75rem; border-bottom: 1px solid #2a2a2a; padding-bottom: .5rem;
}
.cf-legal-body h3 { font-size: 1rem; font-weight: 600; color: #ccc; margin: 1.5rem 0 .5rem; }
.cf-legal-body p, .cf-legal-body li { color: #aaa; line-height: 1.8; margin-bottom: .75rem; }
.cf-legal-body ul { padding-left: 1.5rem; margin-bottom: 1rem; }
.cf-legal-body a  { color: #4fc3f7; text-decoration: none; }
.cf-legal-body a:hover { text-decoration: underline; }
</style>
"""


def main():
    for fname, content in [('cf_privacy.php', PRIVACY_PHP), ('cf_terms.php', TERMS_PHP)]:
        print(f'Uploading {fname}...')
        result = cpanel_put(ARTICLE_DIR, fname, content)
        print(f'  {"OK" if result.get("status") == 1 else result.get("errors")}')


if __name__ == '__main__':
    main()
