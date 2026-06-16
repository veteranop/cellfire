<?php
/**
 * Cellfire Template — Article Layout: Privacy Policy
 * Assign to an article with alias "privacy"
 */
defined('_JEXEC') or die;
$updated = 'May 19, 2026';
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
    and a hashed password. This data is stored securely on VeteranOp servers.</p>

    <h3>Device Identifiers</h3>
    <p>The Cellfire app uses your Android device identifier (<strong>Android ID</strong>) to enforce
    single-device licensing and prevent unauthorized use of a single license across multiple devices.
    This identifier is stored on our servers associated with your license record.</p>

    <h3>Signal &amp; Location Data</h3>
    <p>The Cellfire app collects <strong>cellular signal measurements</strong> (RSRP, RSRQ, SINR,
    ARFCN, PCI, band, carrier) and, when GPS drive-test mode is active,
    <strong>GPS coordinates</strong> tied to those measurements. This data is used to power
    drive-test maps, coverage analysis, and the crowdsourced signal database.</p>
    <p>You control when GPS logging is active. Location data is only collected while the app
    is in the foreground and drive-test recording is enabled.</p>

    <h3>Usage Data &amp; Crash Reports</h3>
    <p>We collect basic app usage analytics (feature interactions, session duration) and crash
    diagnostic reports via <strong>Google Firebase Crashlytics</strong> to improve the Services.
    Crash reports may include device model, OS version, app version, and a stack trace.
    This data is anonymized and aggregated where possible.</p>

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
      <li><strong>Google Firebase</strong> — realtime database (crowdsourced signal observations)</li>
      <li><strong>Google Firebase Crashlytics</strong> — crash reporting and diagnostics</li>
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
    in transit and server-side encryption for data at rest. No method of transmission
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
