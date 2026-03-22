package com.veteranop.cellfire

import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UnwiredLabsApi {
    @FormUrlEncoded
    @POST("v2/process.php")
    suspend fun locate(
        @Field("token") token: String = "pk.d7c0a15de9e4e577ea9207f2e1377de4",
        @Field("radio") radio: String = "lte",
        @Field("mcc") mcc: Int,
        @Field("mnc") mnc: Int,
        @Field("cells") cells: String
    ): UnwiredResponse
}

data class UnwiredResponse(
    val status: String,
    val balance: Double?,
    val lat: Double?,
    val lon: Double?,
    val accuracy: Double?,
    val address: String?
)

@Serializable
data class BandCarrierLookup(
    val lte_band_to_carrier: Map<String, List<String>>,
    val nr_band_to_carrier: Map<String, List<String>>,
    val earfcn_ranges: Map<String, CarrierLookupBandInfo>,
    val carrier_exclusive_bands: Map<String, String>,
    val carrier_priority_bands: Map<String, List<Int>>
)

@Serializable
data class CarrierLookupBandInfo(
    val band: Int,
    val name: String
)

object CarrierResolver {

    private val cache = LruCache<String, Pair<String, GeoPoint?>>(500)
    private var bandLookup: BandCarrierLookup? = null

    private val api = Retrofit.Builder()
        .baseUrl("https://us1.unwiredlabs.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(UnwiredLabsApi::class.java)

    fun initialize(context: Context) {
        try {
            val raw = context.assets.open("band_carrier_lookup.json")
                .bufferedReader().use { it.readText() }
            // Strip // line comments — the JSON file uses them for readability
            // but Kotlinx Serialization rejects non-standard JSON.
            val clean = raw.lines()
                .map { line -> line.substringBefore("//").trimEnd() }
                .joinToString("\n")
            bandLookup = Json.decodeFromString(clean)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resolve carrier from EARFCN (LTE) or NR-ARFCN (5G)
     * Returns list of possible carriers, or null if unknown
     */
    fun resolveFromBand(earfcn: Int, isNr: Boolean = false): List<String>? {
        val lookup = bandLookup ?: return null
        
        // Find band from EARFCN
        val band = if (isNr) {
            // NR-ARFCN to band mapping is more complex, simplified here
            when (earfcn) {
                in 422000..434000 -> 71  // n71
                in 514000..524000 -> 12  // n12
                in 620000..680000 -> 77  // n77 C-Band
                in 2054166..2104165 -> 260 // n260 mmWave
                in 2016667..2070832 -> 261 // n261 mmWave
                else -> null
            }
        } else {
            // LTE EARFCN lookup
            lookup.earfcn_ranges.entries
                .firstOrNull { (range, _) ->
                    val parts = range.split("-")
                    if (parts.size == 2) {
                        try {
                            earfcn in parts[0].toInt()..parts[1].toInt()
                        } catch (e: Exception) {
                            false
                        }
                    } else false
                }?.value?.band
        }
        
        band ?: return null
        
        // Check if exclusive band
        lookup.carrier_exclusive_bands[band.toString()]?.let {
            return listOf(it)
        }
        
        // Return possible carriers for this band
        val carriers = if (isNr) {
            lookup.nr_band_to_carrier[band.toString()]
        } else {
            lookup.lte_band_to_carrier[band.toString()]
        }
        
        return carriers?.let { list ->
            if (list.size > 1) {
                // Prioritize: e.g., n71 is usually T-Mobile
                when (band) {
                    71 -> listOf("T-Mobile") + list.filter { it != "T-Mobile" }
                    2, 4, 13 -> listOf("Verizon") + list.filter { it != "Verizon" }
                    // Add more heuristics as needed
                    else -> list
                }
            } else list
        }
    }

    /**
     * Returns true if [carrier] is known to operate on the given band.
     *
     * [bandLabel] is the already-resolved band string (e.g. "B12", "n71") from
     * earfcnToLteBand() / nrarfcnToNrBand(). When provided it is used directly
     * against lte_band_to_carrier / carrier_exclusive_bands, bypassing the
     * incomplete earfcn_ranges table in the JSON which only covers B17 and B41.
     *
     * Falls back to resolveFromBand(earfcn) if bandLabel is absent or unrecognised.
     * Returns true (permissive) only if no band data exists at all.
     */
    fun isCarrierValidForBand(
        carrier: String,
        earfcn: Int,
        bandLabel: String = "",
        isNr: Boolean = false
    ): Boolean {
        if (bandLabel.isNotEmpty()) {
            val bandNum = bandLabel.removePrefix("B").removePrefix("n").toIntOrNull()
            if (bandNum != null) {
                val lookup = bandLookup
                // Exclusive band → only one carrier allowed
                lookup?.carrier_exclusive_bands?.get(bandNum.toString())?.let { exclusive ->
                    return carrier == exclusive
                }
                // Shared band → check allowed list
                val valid = if (isNr) lookup?.nr_band_to_carrier?.get(bandNum.toString())
                            else      lookup?.lte_band_to_carrier?.get(bandNum.toString())
                if (valid != null) return carrier in valid
            }
        }
        // Fallback: resolve band from EARFCN (works for B17/B41 only via earfcn_ranges)
        val validCarriers = resolveFromBand(earfcn, isNr) ?: return true
        return carrier in validCarriers
    }

    // Helper for consistent PCI naming (e.g., for UI/logs)
    fun formatPciName(cell: Cell): String {
        val carrier = cell.carrier.takeIf { it != "Unknown" } ?: "?"
        val bandPrefix = if (cell.band.isNotEmpty()) "${cell.band} " else ""
        return "$carrier $bandPrefix PCI ${cell.pci}"
    }

    suspend fun resolveFromOpenCellId(
        mcc: Int, 
        mnc: Int, 
        tacOrLac: Int, 
        cellId: Long,
        earfcn: Int? = null,
        isNr: Boolean = false
    ): Pair<String, GeoPoint?> {
        val key = "$mcc-$mnc-$tacOrLac-$cellId"
        cache[key]?.let { return it }

        // If MCC/MNC is valid, use it
        if (mcc != 0 && mnc != 0 && mcc != 2147483647) {
            return resolveFromMccMnc(mcc, mnc, tacOrLac, cellId, key)
        }
        
        // Fallback: try EARFCN-based detection
        earfcn?.let { freq ->
            val carriers = resolveFromBand(freq, isNr)
            carriers?.let {
                if (it.size == 1) {
                    // Single carrier match - high confidence
                    val result = it[0] to null
                    cache.put(key, result)
                    return result
                } else if (it.isNotEmpty()) {
                    // Multiple possible carriers
                    val carrierStr = it.joinToString("/")
                    val result = carrierStr to null
                    cache.put(key, result)
                    return result
                }
            }
        }
        
        // Last resort
        return "Unknown Carrier" to null
    }

    private suspend fun resolveFromMccMnc(
        mcc: Int,
        mnc: Int, 
        tacOrLac: Int,
        cellId: Long,
        key: String
    ): Pair<String, GeoPoint?> {
        return try {
            val cellsJson = """[{"lac": $tacOrLac, "cid": $cellId}]"""
            val response = api.locate(mcc = mcc, mnc = mnc, cells = cellsJson)

            if (response.status == "ok") {
                val point = if (response.lat != null && response.lon != null) {
                    GeoPoint(response.lat, response.lon)
                } else null
                val carrier = response.address?.split(",")?.get(0)?.trim()
                    ?.takeIf { it.isNotBlank() && it.length > 2 }
                    ?: fallbackName(mcc, mnc)
                val result = carrier to point
                cache.put(key, result)
                result
            } else {
                fallbackName(mcc, mnc) to null
            }
        } catch (e: Exception) {
            Log.e("CarrierResolver", "UnwiredLabs failed: ${e.message}")
            fallbackName(mcc, mnc) to null
        }
    }

    private fun fallbackName(mcc: Int, mnc: Int): String {
        val plmn = String.format("%03d%02d", mcc, mnc)
        
        return when (plmn) {
            // T-Mobile (including MVNOs and legacy Sprint)
            "310160", "310200", "310210", "310220", "310230", "310240",
            "310250", "310260", "310270", "310310", "310490", "310660",
            "310800", "311882", "312250", "316010", "310120" /* Mint Mobile */, "310026" /* Boost on T-Mobile */ -> "T-Mobile"

            // Verizon (including MVNOs like Visible)
            "310004", "310010", "310012", "310013", "310590", "310890",
            "310910", "311012", "311110", "311270", "311271", "311272",
            "311273", "311274", "311275", "311276", "311277", "311278",
            "311279", "311280", "311281", "311282", "311283", "311284",
            "311285", "311286", "311287", "311288", "311289", "311390",
            "311480", "311481", "311482", "311483", "311484", "311485",
            "311486", "311487", "311488", "311489", "310005" /* Visible */ -> "Verizon"

            // AT&T (including Cricket, FirstNet expansions)
            "310070", "310150", "310170", "310280", "310380", "310410",
            "310560", "310670", "310680", "310950", "311070", "311180",
            "311870", "312670", "310030" /* Cricket */ -> "AT&T"
            // Dish Wireless
            "312670", "313340" -> "Dish Wireless"
            
            // FirstNet (expanded)
            "313100", "310410" /* Some overlaps */ -> "FirstNet"

            // US Cellular
            "310066", "311220", "311580", "311581", "311582", "311583",
            "311584", "311585", "311586", "311587", "311588", "311589" -> "US Cellular"
            
            // More Regionals/MVNOs
            "311230" -> "C Spire"
            "311800" -> "Bluegrass Cellular"
            "310000" -> "Cellcom"
            "311120" -> "Chat Mobility"
            "312040" -> "Copper Valley"
            "312030" -> "Appalachian Wireless"
            "311370", "310018" -> "GCI"
            "310470" -> "Shentel"
            "311860" -> "Union Wireless"
            "310890" -> "Rural Cellular"
            "312270" -> "Viaero"
            "310830" -> "Thumb Cellular"  // Additional regional
            "311490" -> "Cellular One"    // Northeast
            "310910" -> "Panhandle"       // Texas regional

            else -> "Unknown ($plmn)"
        }
    }
}
