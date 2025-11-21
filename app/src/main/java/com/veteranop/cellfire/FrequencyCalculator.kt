package com.veteranop.cellfire

import android.content.Context
import org.json.JSONObject

object FrequencyCalculator {
    private lateinit var json: JSONObject

    fun init(context: Context) {
        try {
            val text = context.assets.open("earfcn_frequencies.json").use { it.reader().readText() }
            json = JSONObject(text)
        } catch (e: Exception) {
            // Handle exceptions, e.g., file not found
            json = JSONObject() // Initialize with an empty object to avoid crashes
        }
    }

    fun getLteFrequency(earfcn: Int): Pair<Double, Double>? {
        if (!::json.isInitialized || !json.has("lte")) return null
        val lte = json.optJSONObject("lte") ?: return null
        val bands = lte.optJSONObject("bands") ?: return null

        for (bandKey in bands.keys()) {
            val band = bands.optJSONObject(bandKey) ?: continue
            val dlLow = band.optDouble("dl_low")
            val dlOffset = band.optInt("dl_offset")
            val ulLow = band.optDouble("ul_low")
            val duplexMode = band.optString("duplex", "FDD")
            val rangeLen = band.optInt("range_len", 600)

            val dlRangeEnd = dlOffset + rangeLen - 1

            if (earfcn in dlOffset..dlRangeEnd) {
                val dlFreq = dlLow + 0.1 * (earfcn - dlOffset)
                
                val ulFreq = if (duplexMode == "FDD") {
                    // For FDD, calculate the offset from the start of the DL range
                    // and apply it to the start of the UL frequency range.
                    val earfcnInBandOffset = earfcn - dlOffset
                    ulLow + 0.1 * earfcnInBandOffset
                } else {
                    // For TDD, UL and DL use the same frequency band.
                    dlFreq 
                }

                return dlFreq to ulFreq
            }
        }
        return null
    }

    fun getNrFrequency(nrarfcn: Int): Pair<Double, Double>? {
        if (!::json.isInitialized || !json.has("nr")) return null
        val nr = json.optJSONObject("nr") ?: return null
        val global = nr.optJSONObject("global") ?: return null
        
        val range = when {
            nrarfcn < 600000 -> global.optJSONObject("low")
            nrarfcn < 2016667 -> global.optJSONObject("mid")
            else -> global.optJSONObject("high")
        } ?: return null

        val fRefLow = range.optDouble("f_ref_low")
        val nRefOffset = range.optInt("n_ref_offset")
        val deltaF = range.optDouble("delta_f") / 1000.0 // kHz → MHz

        val fRef = fRefLow + deltaF * (nrarfcn - nRefOffset)
        return fRef to fRef // TDD has same UL/DL
    }
}
