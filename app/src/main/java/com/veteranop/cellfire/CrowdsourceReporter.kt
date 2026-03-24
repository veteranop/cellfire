package com.veteranop.cellfire

import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.abs
import kotlin.math.floor

/**
 * Submits cell observations to Firebase Realtime Database.
 *
 * Submits any cell where we have a non-Unknown carrier, valid PCI and TAC, and
 * a GPS fix. Sources are tagged by quality: alpha > plmn > fcc_band > db > pci_range.
 * A higher-quality source for the same (pci, tac) will re-submit within a session,
 * overwriting the lower-quality entry in Firebase.
 *
 * Firebase path:
 *   /observations/{tileKey}/{pushId}
 */
object CrowdsourceReporter {

    private const val TAG = "CrowdsourceReporter"
    private const val TILE_SIZE = 0.5

    // Source quality ranking — higher = more reliable
    private val SOURCE_QUALITY = mapOf(
        "alpha"          to 5,
        "plmn"           to 4,
        "exclusive_band" to 3,  // band-law lock (B71→T-Mobile, B13→Verizon, etc.)
        "fcc_band"       to 3,
        "db"             to 2,
        "pci_range"      to 1
    )

    private val db by lazy {
        Firebase.database("https://veteranopcom-default-rtdb.firebaseio.com")
    }

    // Tracks best source quality already submitted for each (pci, tac) this session
    private val submittedQuality = HashMap<Long, Int>()

    fun submit(
        pci: Int,
        tac: Int,
        carrier: String,
        mnc: String,
        lat: Double,
        lon: Double,
        arfcn: Int,
        band: String,
        source: String,
        state: String = ""
    ) {
        if (lat == 0.0 && lon == 0.0) return
        if (pci <= 0) return
        if (tac >= 0xFFFF) return  // 65535 (0xFFFF) = modem sentinel; 66535+ = overflow — both invalid
        // TAC=0 is allowed — neighbor cells without TAC are still valuable:
        //   GPS + PCI + EARFCN lets the merge script do exclusive-band resolution
        //   and neighbor inference, seeding the tile DB for future lookups.
        // Unknown carrier is allowed — the server fills it in from band/geo data.

        val newQuality = SOURCE_QUALITY.getOrDefault(source, 0)
        val key = packKey(pci, tac)
        val existingQuality = submittedQuality.getOrDefault(key, -1)

        // Only submit if this source is strictly better than what we've already sent
        if (newQuality <= existingQuality) return
        submittedQuality[key] = newQuality

        val tileKey = tileFilename(lat, lon)
        val observation = mapOf(
            "pci"       to pci,
            "tac"       to tac,
            "carrier"   to carrier,
            "mnc"       to mnc,
            "lat"       to lat,
            "lon"       to lon,
            "arfcn"     to arfcn,
            "band"      to band,
            "source"    to source,
            "state"     to state,
            "timestamp" to System.currentTimeMillis(),
            "processed" to false
        )

        // Use pci_tac as the node key so the same cell always overwrites itself — no duplicates.
        val nodeKey = "${pci}_${tac}"
        Log.d(TAG, "Submitting PCI=$pci TAC=$tac carrier=$carrier source=$source tile=$tileKey")
        db.getReference("observations/$tileKey/$nodeKey")
            .setValue(observation)
            .addOnSuccessListener {
                Log.d(TAG, "SUCCESS PCI=$pci TAC=$tac → $tileKey/$nodeKey")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "FAILED PCI=$pci TAC=$tac: ${e.message}")
                // Roll back so it can retry with same or better source
                if ((submittedQuality[key] ?: -1) == newQuality) {
                    submittedQuality[key] = existingQuality
                }
            }
    }

    /**
     * Bulk upload variant — bypasses the session quality dedup so historical
     * discovered PCIs can always be submitted (e.g. from the Settings upload button).
     */
    fun submitBulk(
        pci: Int, tac: Int, carrier: String, mnc: String,
        lat: Double, lon: Double, arfcn: Int, band: String, source: String,
        state: String = ""
    ) {
        if (lat == 0.0 && lon == 0.0) return
        if (pci <= 0) return
        if (tac >= 0xFFFF) return

        val tileKey = tileFilename(lat, lon)
        val observation = mapOf(
            "pci" to pci, "tac" to tac, "carrier" to carrier, "mnc" to mnc,
            "lat" to lat, "lon" to lon, "arfcn" to arfcn, "band" to band,
            "source" to source, "state" to state,
            "timestamp" to System.currentTimeMillis(), "processed" to false
        )

        val nodeKey = "${pci}_${tac}"
        Log.d(TAG, "Bulk submit PCI=$pci TAC=$tac carrier=$carrier tile=$tileKey/$nodeKey")
        db.getReference("observations/$tileKey/$nodeKey").setValue(observation)
            .addOnSuccessListener { Log.d(TAG, "Bulk SUCCESS PCI=$pci TAC=$tac") }
            .addOnFailureListener { e -> Log.w(TAG, "Bulk FAILED PCI=$pci: ${e.message}") }
    }

    private fun tileFilename(lat: Double, lon: Double): String {
        val tLat = floor(lat / TILE_SIZE) * TILE_SIZE
        val tLon = floor(lon / TILE_SIZE) * TILE_SIZE
        return "grid_${coordStr(tLat)}_${coordStr(tLon)}"
    }

    private fun coordStr(v: Double): String {
        val prefix = if (v >= 0.0) "p" else "n"
        val tenths = Math.round(abs(v) * 10).toInt()
        return "$prefix$tenths"
    }

    private fun packKey(pci: Int, tac: Int): Long = (pci.toLong() shl 32) or (tac.toLong() and 0xFFFFFFFFL)
}
