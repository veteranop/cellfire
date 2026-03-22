package com.veteranop.cellfire

import android.content.Context
import org.json.JSONObject

/**
 * FCC spectrum license EARFCN range lookup.
 *
 * Loaded from carrier_earfcn_map.json (generated from FCC ULS l_market.zip).
 * Structure: state → carrier → band → [[earfcn_lo, earfcn_hi], ...]
 *
 * Validated against real device data (Idaho Falls, ID):
 *   Verizon B66 EARFCN 66536 → within [66436, 66686] ✓
 *   Verizon B2  EARFCN 1100  → within [950, 1200]    ✓
 *   Verizon B13 EARFCN 5230  → within [5180, 5279]   ✓
 */
object BandLicenseMap {

    // state → carrier → band → list of (lo, hi) EARFCN ranges
    private var earfcnMap: Map<String, Map<String, Map<String, List<Pair<Int, Int>>>>> = emptyMap()

    private val STATE_NAME_TO_ABBREV = mapOf(
        "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
        "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
        "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID",
        "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS",
        "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
        "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS",
        "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
        "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
        "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK",
        "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
        "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
        "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
        "wisconsin" to "WI", "wyoming" to "WY", "district of columbia" to "DC",
        "puerto rico" to "PR", "virgin islands" to "VI", "guam" to "GU"
    )

    // Bands that share physical spectrum — check all aliases on lookup.
    // LTE: B4↔B66 (AWS), B2↔B25 (PCS)
    // NR:  n25↔n2 (same PCS spectrum, carriers report either label)
    private val BAND_ALIASES = mapOf(
        "B4"  to listOf("B4",  "B66"),
        "B66" to listOf("B66", "B4"),
        "B25" to listOf("B25", "B2"),
        "B2"  to listOf("B2",  "B25"),
        "n25" to listOf("n25", "n2"),
        "n2"  to listOf("n2",  "n25"),
    )

    fun stateAbbrev(adminArea: String): String =
        STATE_NAME_TO_ABBREV[adminArea.lowercase()] ?: adminArea.uppercase().take(2)

    fun init(context: Context) {
        if (earfcnMap.isNotEmpty()) return
        try {
            val json = context.assets.open("carrier_earfcn_map.json").bufferedReader().readText()
            val root = JSONObject(json)
            val stateResult = mutableMapOf<String, Map<String, Map<String, List<Pair<Int, Int>>>>>()

            for (state in root.keys()) {
                val carriersJson = root.getJSONObject(state)
                val carrierResult = mutableMapOf<String, Map<String, List<Pair<Int, Int>>>>()

                for (carrier in carriersJson.keys()) {
                    val bandsJson = carriersJson.getJSONObject(carrier)
                    val bandResult = mutableMapOf<String, List<Pair<Int, Int>>>()

                    for (band in bandsJson.keys()) {
                        val rangesArr = bandsJson.getJSONArray(band)
                        val ranges = (0 until rangesArr.length()).map { i ->
                            val pair = rangesArr.getJSONArray(i)
                            Pair(pair.getInt(0), pair.getInt(1))
                        }
                        bandResult[band] = ranges
                    }
                    carrierResult[carrier] = bandResult
                }
                stateResult[state] = carrierResult
            }
            earfcnMap = stateResult
        } catch (_: Exception) { }
    }

    /**
     * Returns carrier if exactly one carrier holds a spectrum license that covers
     * [earfcn] for [bandLabel] (e.g. "B66") in [stateAbbrev] (e.g. "ID").
     *
     * Returns null if zero or multiple carriers match (ambiguous or no data).
     */
    fun resolveCarrier(stateAbbrev: String, bandLabel: String, earfcn: Int): String? {
        if (stateAbbrev.isEmpty() || earfcnMap.isEmpty()) return null
        val carriers = earfcnMap[stateAbbrev] ?: return null

        // Check band and its alias (B4↔B66, B2↔B25)
        val bandsToCheck = BAND_ALIASES[bandLabel] ?: listOf(bandLabel)

        val matching = carriers.entries.filter { (_, bands) ->
            bandsToCheck.any { b ->
                bands[b]?.any { (lo, hi) -> earfcn in lo..hi } == true
            }
        }.map { it.key }

        return if (matching.size == 1) matching[0] else null
    }
}
