package com.veteranop.cellfire

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Thin wrapper around Firebase Analytics.
 * All event names follow Firebase snake_case convention (max 40 chars).
 * Call CellfireAnalytics.init(context) once in Application.onCreate().
 */
object CellfireAnalytics {

    private lateinit var fa: FirebaseAnalytics

    fun init(context: Context) {
        fa = Firebase.analytics
    }

    // ── Scan lifecycle ────────────────────────────────────────────────────────

    /** User tapped FIRE / started monitoring. */
    fun scanStarted() = log("scan_started")

    /** User tapped CEASE FIRE / stopped monitoring. */
    fun scanStopped(durationSeconds: Long) = log("scan_stopped") {
        putLong("duration_s", durationSeconds)
    }

    /** Deep scan activated (35-second extended scan cycle). */
    fun deepScanActivated() = log("deep_scan_activated")

    /** Drive test mode toggled. */
    fun driveTestToggled(enabled: Boolean) = log("drive_test_toggled") {
        putString("enabled", enabled.toString())
    }

    // ── Database ──────────────────────────────────────────────────────────────

    /** User tapped Sync DB in Settings. */
    fun dbSyncRequested() = log("db_sync_requested")

    /** Tile download completed for a location refresh. */
    fun dbTilesRefreshed(tilesLoaded: Int, tilesUpdated: Int, recordCount: Int) =
        log("db_tiles_refreshed") {
            putInt("tiles_loaded", tilesLoaded)
            putInt("tiles_updated", tilesUpdated)
            putInt("record_count", recordCount)
        }

    // ── Crowdsource upload ────────────────────────────────────────────────────

    /** A single observation submitted to Firebase from CrowdsourceReporter. */
    fun observationSubmitted(carrier: String, source: String, band: String) =
        log("observation_submitted") {
            putString("carrier", carrier)
            putString("source", source)
            putString("band", band)
        }

    /** User tapped "Upload discovered PCIs" in Settings. */
    fun pciUploadRequested(pciCount: Int) = log("pci_upload_requested") {
        putInt("pci_count", pciCount)
    }

    /** PCI upload completed. */
    fun pciUploadCompleted(sent: Int, skipped: Int) = log("pci_upload_completed") {
        putInt("sent", sent)
        putInt("skipped", skipped)
    }

    // ── Signal Rules ──────────────────────────────────────────────────────────

    /** User tapped Update in Signal Rules section. */
    fun signalRulesUpdateRequested() = log("signal_rules_update_requested")

    /** Signal rules download result. */
    fun signalRulesUpdated(version: String, success: Boolean) =
        log("signal_rules_updated") {
            putString("version", version)
            putString("success", success.toString())
        }

    // ── Carrier resolution ────────────────────────────────────────────────────

    /** Carrier resolved for a cell — logged once per unique (pci, earfcn) per session. */
    fun carrierResolved(carrier: String, source: String, band: String, confidence: Int) =
        log("carrier_resolved") {
            putString("carrier", carrier)
            putString("source", source)
            putString("band", band)
            putInt("confidence", confidence)
        }

    // ── Data management ───────────────────────────────────────────────────────

    /** User confirmed Clear All Data. */
    fun allDataCleared() = log("all_data_cleared")

    // ── Navigation ────────────────────────────────────────────────────────────

    /** User opened the About screen. */
    fun aboutScreenOpened() = log("about_screen_opened")

    /** User opened Settings. */
    fun settingsScreenOpened() = log("settings_screen_opened")

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun log(event: String, block: (Bundle.() -> Unit)? = null) {
        if (!::fa.isInitialized) return
        try {
            val bundle = if (block != null) Bundle().apply(block) else null
            fa.logEvent(event, bundle)
        } catch (_: Exception) { }
    }
}
