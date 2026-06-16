<?php
/**
 * Cellfire Registration Page — template override for com_content/article
 * Alternative Layout: cf_register
 *
 * Accessed at: cellfire.io/register
 * Flow: user submits form → JS POST to auth.register API → success message
 */
defined('_JEXEC') or die;

/** @var \Joomla\CMS\Application\CMSApplication $app */
$app = \Joomla\CMS\Factory::getApplication();
?>
<!DOCTYPE html>
<html lang="en">
<?php // Full page only renders when called as a standalone article;
      // the parent template handles <html>/<head><meta charset="utf-8"> in normal use.
      // This file only outputs the article body section.
?>
<section class="cf-register-section">
  <div class="container" style="max-width:540px; margin:0 auto; padding:60px 20px 80px;">

    <!-- ── Header ──────────────────────────────────────── -->
    <div class="text-center mb-5">
      <h1 class="cf-section-title" style="font-size:2rem; margin-bottom:.5rem;">
        Start Your Free&nbsp;<span class="cf-gradient-text">30-Day Trial</span>
      </h1>
      <p class="cf-section-sub" style="color:#aaa;">
        No credit card required &middot; Full access except FCC tools &middot; Cancel anytime
      </p>
    </div>

    <!-- ── Alert area ─────────────────────────────────── -->
    <div id="cf-reg-alert" class="cf-alert" role="alert" style="display:none;"></div>

    <!-- ── Success card (shown after registration) ──────── -->
    <div id="cf-reg-success" style="display:none; text-align:center; background:#1e1e1e;
         border:1px solid #2a2a2a; border-radius:12px; padding:40px 32px;">
      <div style="font-size:3rem; margin-bottom:16px;">🎉</div>
      <h2 style="color:#fff; margin-bottom:12px;">Account Created!</h2>
      <p style="color:#aaa; margin-bottom:24px;">
        Your 30-day trial is active. Sign in to <strong>Cellfire RF Studio</strong>
        with the username and password you just created.
      </p>
      <a href="/downloads" class="cf-btn cf-btn--primary" style="display:inline-block; margin-bottom:16px;">
        ⬇&nbsp; Download the App
      </a>
      <p style="color:#666; font-size:.85rem;">
        Already installed? Open the app and click <em>Sign In</em>.
      </p>
    </div>

    <!-- ── Registration form ────────────────────────────── -->
    <form id="cf-reg-form" novalidate
          style="background:#1a1a1a; border:1px solid #2a2a2a; border-radius:14px;
                 padding:40px 36px; box-shadow:0 4px 32px rgba(0,0,0,.4);">

      <!-- Username -->
      <div class="mb-4">
        <label for="cf-username" class="cf-form-label">Username</label>
        <input id="cf-username" type="text" class="cf-form-input" autocomplete="username"
               placeholder="e.g. rf_engineer_mark" maxlength="32" required>
        <div class="cf-field-hint">3–32 characters, letters/numbers/underscore only</div>
      </div>

      <!-- Email -->
      <div class="mb-4">
        <label for="cf-email" class="cf-form-label">Email Address</label>
        <input id="cf-email" type="email" class="cf-form-input" autocomplete="email"
               placeholder="you@example.com" required>
      </div>

      <!-- Password -->
      <div class="mb-4">
        <label for="cf-password" class="cf-form-label">Password</label>
        <input id="cf-password" type="password" class="cf-form-input"
               autocomplete="new-password" placeholder="Minimum 8 characters" required>
      </div>

      <!-- Confirm Password -->
      <div class="mb-5">
        <label for="cf-confirm" class="cf-form-label">Confirm Password</label>
        <input id="cf-confirm" type="password" class="cf-form-input"
               autocomplete="new-password" placeholder="Repeat password" required>
      </div>

      <!-- Submit -->
      <button id="cf-reg-btn" type="submit" class="cf-btn cf-btn--primary w-100"
              style="width:100%; padding:14px; font-size:1rem; border:none; cursor:pointer;">
        Create Free Account
      </button>

      <!-- Terms micro-copy -->
      <p style="color:#555; font-size:.78rem; text-align:center; margin-top:16px;">
        By creating an account you agree to the
        <a href="/terms" style="color:#888;">Terms of Service</a> and
        <a href="/privacy" style="color:#888;">Privacy Policy</a>.
      </p>
    </form>

    <!-- ── Footer link ─────────────────────────────────── -->
    <p style="text-align:center; margin-top:24px; color:#666; font-size:.9rem;">
      Already have an account?
      <a href="/downloads" style="color:#4fc3f7;">Download the app</a> and sign in.
    </p>

  </div><!-- /container -->
</section>

<!-- ── Inline styles ────────────────────────────────────── -->
<style>
.cf-form-label {
  display: block;
  color: #ccc;
  font-size: .875rem;
  font-weight: 500;
  margin-bottom: 6px;
}
.cf-form-input {
  width: 100%;
  box-sizing: border-box;
  background: #2a2a2a;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  color: #fff;
  font-size: 1rem;
  padding: 10px 14px;
  outline: none;
  transition: border-color .2s;
}
.cf-form-input:focus { border-color: #0078d4; }
.cf-form-input.is-invalid { border-color: #f44336; }
.cf-field-hint { color: #666; font-size: .78rem; margin-top: 4px; }
.cf-alert {
  padding: 14px 18px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: .9rem;
  line-height: 1.4;
}
.cf-alert--error   { background: rgba(244,67,54,.15); color: #ef9a9a; border: 1px solid rgba(244,67,54,.3); }
.cf-alert--success { background: rgba(76,175,80,.15);  color: #a5d6a7; border: 1px solid rgba(76,175,80,.3); }
.mb-4 { margin-bottom: 1.25rem; }
.mb-5 { margin-bottom: 1.75rem; }
.w-100 { width: 100%; }
</style>

<!-- ── Registration logic ────────────────────────────────── -->
<script>
(function () {
  'use strict';

  const form    = document.getElementById('cf-reg-form');
  const btn     = document.getElementById('cf-reg-btn');
  const alert   = document.getElementById('cf-reg-alert');
  const success = document.getElementById('cf-reg-success');

  function showAlert(msg, type) {
    alert.className = 'cf-alert cf-alert--' + type;
    alert.textContent = msg;
    alert.style.display = 'block';
    alert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
  function hideAlert() { alert.style.display = 'none'; }

  function setWorking(working) {
    btn.disabled = working;
    btn.textContent = working ? 'Creating account…' : 'Create Free Account';
  }

  function validateInputs() {
    const username = document.getElementById('cf-username').value.trim();
    const email    = document.getElementById('cf-email').value.trim();
    const password = document.getElementById('cf-password').value;
    const confirm  = document.getElementById('cf-confirm').value;

    if (!username || !email || !password || !confirm) {
      showAlert('Please fill in all fields.', 'error'); return false;
    }
    if (!/^[a-zA-Z0-9_]{3,32}$/.test(username)) {
      showAlert('Username must be 3–32 characters using only letters, numbers, or underscores.', 'error'); return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showAlert('Please enter a valid email address.', 'error'); return false;
    }
    if (password.length < 8) {
      showAlert('Password must be at least 8 characters.', 'error'); return false;
    }
    if (password !== confirm) {
      showAlert('Passwords do not match.', 'error'); return false;
    }
    return true;
  }

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    hideAlert();

    if (!validateInputs()) return;

    const username = document.getElementById('cf-username').value.trim();
    const email    = document.getElementById('cf-email').value.trim();
    const password = document.getElementById('cf-password').value;

    setWorking(true);

    try {
      const resp = await fetch(
        '/index.php?option=com_cellfireapi&task=auth.register&format=json',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, email, password }),
        }
      );

      let data;
      try { data = await resp.json(); } catch (_) {
        throw new Error('Server returned an unexpected response. Please try again.');
      }

      if (data.success) {
        form.style.display      = 'none';
        success.style.display   = 'block';
      } else {
        showAlert(data.message || 'Registration failed. Please try again.', 'error');
      }

    } catch (err) {
      showAlert(err.message || 'Network error. Please check your connection and try again.', 'error');
    } finally {
      setWorking(false);
    }
  });
})();
</script>