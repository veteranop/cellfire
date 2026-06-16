package com.veteranop.cellfire

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

// ─── Data models ────────────────────────────────────────────────────────────

data class LicenseInfo(
    val planType: String,
    val planLabel: String,
    val isActive: Boolean,
    val expiresAt: String?,
    val stripeStatus: String?,
    val hasStripeCustomer: Boolean
)

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val username: String, val license: LicenseInfo) : AuthState()
    data class AwaitingVerification(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ─── Manager ─────────────────────────────────────────────────────────────────

@Singleton
class CellfireAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val API_BASE = "https://cellfire.io/index.php?option=com_cellfireapi&task="
        private const val PREFS_FILE = "cellfire_auth"
        private const val KEY_JWT              = "jwt"
        private const val KEY_USERNAME         = "username"
        private const val KEY_LICENSE_PLAN     = "license_plan"
        private const val KEY_LICENSE_LABEL    = "license_label"
        private const val KEY_LICENSE_ACTIVE   = "license_active"
        private const val KEY_LICENSE_EXPIRES  = "license_expires"
        private const val KEY_LICENSE_STRIPE   = "license_stripe"
    }

    private val prefs by lazy {
        fun build(): android.content.SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context, PREFS_FILE, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        try {
            build()
        } catch (e: Exception) {
            // Keystore invalidated (OS update, screen-lock change, etc.) — wipe and recreate.
            // User will need to log in again, but won't be stuck in a broken state.
            context.deleteSharedPreferences(PREFS_FILE)
            try { build() } catch (e2: Exception) {
                // Absolute last resort — plain prefs (no sensitive data survives reboot)
                context.getSharedPreferences(PREFS_FILE + "_plain", Context.MODE_PRIVATE)
            }
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    /** Device identifier used as mac_address field for the API */
    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "android-unknown"

    fun getStoredToken(): String? = prefs.getString(KEY_JWT, null)
    fun getStoredUsername(): String? = prefs.getString(KEY_USERNAME, null)

    /** Called on app start — restores session if token exists.
     *  On server failure, uses the last cached license so paying users aren't
     *  shown "Inactive" due to a temporary network issue or expired JWT. */
    suspend fun restoreSession() {
        val token = getStoredToken()
        val username = getStoredUsername()
        if (token == null || username == null) {
            _authState.value = AuthState.LoggedOut
            return
        }
        _authState.value = AuthState.Loading
        val result = verifyLicense(token)
        _authState.value = if (result != null) {
            cacheLicense(result)
            AuthState.LoggedIn(username, result)
        } else {
            // Server unreachable or JWT expired — use last cached state.
            // Never show "Inactive" just because we couldn't reach the server.
            AuthState.LoggedIn(username, getCachedLicense())
        }
    }

    /** Register new account. Returns error string on failure, null on success.
     *  On success sets AwaitingVerification — no JWT issued until email is confirmed. */
    suspend fun register(username: String, email: String, password: String): String? = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val body = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
            }
            val resp = post("auth.register", body)
            if (!resp.optBoolean("success", false)) {
                val msg = resp.optString("message", "Registration failed")
                _authState.value = AuthState.Error(msg)
                return@withContext msg
            }
            // Server returns requires_verification=true — no JWT yet
            _authState.value = AuthState.AwaitingVerification(email)
            null
        } catch (e: Exception) {
            val msg = e.message ?: "Network error"
            _authState.value = AuthState.Error(msg)
            msg
        }
    }

    /** Resend verification email. Returns error string on failure, null on success. */
    suspend fun resendVerification(identifier: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("username", identifier) }
            val resp = post("auth.resend_verify", body)
            if (!resp.optBoolean("success", false)) {
                return@withContext resp.optString("message", "Failed to resend")
            }
            null
        } catch (e: Exception) {
            e.message ?: "Network error"
        }
    }

    /** Login with username + password. Returns error string on failure, null on success. */
    suspend fun login(username: String, password: String): String? = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
                put("mac_address", deviceId)
            }
            val resp = post("auth.login", body)
            // API returns {"success": true/false, "message": "...", "data": {...}}
            if (!resp.optBoolean("success", false)) {
                val msg = resp.optString("message", "Login failed")
                _authState.value = AuthState.Error(msg)
                return@withContext msg
            }
            val data    = resp.optJSONObject("data") ?: resp
            val token   = data.optString("token")
            val license = parseLicense(data.optJSONObject("license"))
            cacheLicense(license)
            prefs.edit()
                .putString(KEY_JWT, token)
                .putString(KEY_USERNAME, username)
                .apply()
            _authState.value = AuthState.LoggedIn(username, license)
            null
        } catch (e: Exception) {
            val msg = e.message ?: "Network error"
            _authState.value = AuthState.Error(msg)
            msg
        }
    }

    /** Verify existing JWT against the server and return license info, or null if invalid */
    suspend fun verifyLicense(token: String): LicenseInfo? = withContext(Dispatchers.IO) {
        try {
            val resp = getWithAuth("license.verify", token)
            if (!resp.optBoolean("success", false)) return@withContext null
            val data     = resp.optJSONObject("data") ?: return@withContext null
            val planType = data.optString("plan", "demo")
            // Prefer the real DB subscription expiry; fall back to JWT exp for legacy responses
            val expiresAt = data.optString("expires_at")
                .takeIf { it.isNotBlank() && it != "null" }
                ?: run {
                    val exp = data.optLong("exp", 0L)
                    if (exp > 0)
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            .format(java.util.Date(exp * 1000L))
                    else null
                }
            LicenseInfo(
                planType          = planType,
                planLabel         = planLabel(planType),
                isActive          = true,
                expiresAt         = expiresAt,
                stripeStatus      = null,
                hasStripeCustomer = false
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Get a Stripe Customer Portal URL for billing management */
    suspend fun getPortalUrl(): Result<String> = withContext(Dispatchers.IO) {
        val token = getStoredToken() ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val resp = getWithAuth("stripe.portal", token)
            val url = resp.optJSONObject("data")?.optString("url")
                ?: return@withContext Result.failure(Exception("No portal URL returned"))
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        clearSession()
        _authState.value = AuthState.LoggedOut
    }

    fun clearError() {
        if (_authState.value is AuthState.Error || _authState.value is AuthState.AwaitingVerification) {
            _authState.value = AuthState.LoggedOut
        }
    }

    private fun clearSession() {
        prefs.edit().remove(KEY_JWT).remove(KEY_USERNAME).apply()
    }

    // ─── License cache ────────────────────────────────────────────────────────

    private fun cacheLicense(license: LicenseInfo) {
        prefs.edit()
            .putString(KEY_LICENSE_PLAN,    license.planType)
            .putString(KEY_LICENSE_LABEL,   license.planLabel)
            .putBoolean(KEY_LICENSE_ACTIVE, license.isActive)
            .putString(KEY_LICENSE_EXPIRES, license.expiresAt ?: "")
            .putString(KEY_LICENSE_STRIPE,  license.stripeStatus ?: "")
            .apply()
    }

    private fun getCachedLicense(): LicenseInfo {
        val planType = prefs.getString(KEY_LICENSE_PLAN, "") ?: ""
        return if (planType.isNotBlank()) {
            LicenseInfo(
                planType          = planType,
                planLabel         = prefs.getString(KEY_LICENSE_LABEL, planLabel(planType)) ?: planLabel(planType),
                isActive          = prefs.getBoolean(KEY_LICENSE_ACTIVE, true),
                expiresAt         = prefs.getString(KEY_LICENSE_EXPIRES, "")?.takeIf { it.isNotBlank() },
                stripeStatus      = prefs.getString(KEY_LICENSE_STRIPE,  "")?.takeIf { it.isNotBlank() },
                hasStripeCustomer = false
            )
        } else {
            // No cache at all — first login ever failed. Give benefit of the doubt.
            LicenseInfo("unknown", "Verifying…", isActive = true, null, null, false)
        }
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    /**
     * Read the response body regardless of HTTP status code.
     * HttpURLConnection.inputStream throws for non-2xx; use errorStream instead.
     */
    private fun readResponse(conn: HttpURLConnection): JSONObject {
        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text   = stream?.bufferedReader()?.readText()
        if (text.isNullOrBlank()) {
            return JSONObject().apply {
                put("success", false)
                put("message", "Server returned HTTP $code with no body")
            }
        }
        return try {
            JSONObject(text)
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("message", "Unexpected server response (HTTP $code)")
            }
        }
    }

    private fun post(task: String, body: JSONObject): JSONObject {
        val url  = URL("$API_BASE$task&format=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod    = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept",       "application/json")
            doOutput                = true
            connectTimeout          = 15_000
            readTimeout             = 15_000
            instanceFollowRedirects = false   // never follow redirects — API calls should be direct
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        return readResponse(conn)
    }

    private fun getWithAuth(task: String, token: String): JSONObject {
        val url  = URL("$API_BASE$task&format=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod    = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept",        "application/json")
            connectTimeout          = 15_000
            readTimeout             = 15_000
            instanceFollowRedirects = false
        }
        return readResponse(conn)
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private fun parseLicense(lic: JSONObject?): LicenseInfo {
        if (lic == null) return LicenseInfo("demo", "Demo / Trial", false, null, null, false)
        val planType = lic.optString("plan_type", "demo")
        return LicenseInfo(
            planType = planType,
            planLabel = planLabel(planType),
            isActive = lic.optInt("is_active", 0) == 1,
            expiresAt = lic.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
            stripeStatus = lic.optString("stripe_status").takeIf { it.isNotBlank() && it != "null" },
            hasStripeCustomer = lic.optString("stripe_customer_id").let { it.isNotBlank() && it != "null" }
        )
    }

    private fun planLabel(planType: String): String = when (planType) {
        "app_view"            -> "App — View Only"
        "app_full"            -> "App — Full"
        "app_team"            -> "App — Team"
        "studio_standard"     -> "Studio Standard"
        "studio_full"         -> "Studio Full"
        "studio_enterprise"   -> "Studio Enterprise"
        "viewer_basic"        -> "Viewer Basic"
        "viewer_pro"          -> "Viewer Pro"
        "viewer_enterprise"   -> "Viewer Enterprise"
        "bundle_starter"      -> "Bundle — Starter"
        "bundle_pro"          -> "Bundle — Pro"
        "bundle_enterprise"   -> "Bundle — Enterprise"
        "demo"                -> "Demo / Trial"
        else                  -> planType.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
