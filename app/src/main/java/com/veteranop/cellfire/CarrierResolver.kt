package com.veteranop.cellfire

import android.util.LruCache
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import org.osmdroid.util.GeoPoint

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

object CarrierResolver {

    private val cache = LruCache<String, Pair<String, GeoPoint?>>(500)

    private val api = Retrofit.Builder()
        .baseUrl("https://us1.unwiredlabs.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(UnwiredLabsApi::class.java)

    suspend fun resolveFromOpenCellId(mcc: Int, mnc: Int, tacOrLac: Int, cellId: Long): Pair<String, GeoPoint?> {
        val key = "$mcc-$mnc-$tacOrLac-$cellId"
        cache[key]?.let { return it }

        return try {
            val cellsJson = """[{"lac": $tacOrLac, "cid": $cellId}]"""
            val response = api.locate(mcc = mcc, mnc = mnc, cells = cellsJson)

            if (response.status == "ok") {
                val point = if (response.lat != null && response.lon != null) GeoPoint(response.lat, response.lon) else null
                // address usually contains city/state — we take first part before comma as carrier hint
                val carrierFromApi = response.address?.split(",")?.get(0)?.trim() ?: ""
                val carrier = if (carrierFromApi.isNotBlank() && carrierFromApi.length > 2) carrierFromApi else fallbackName(mcc, mnc)
                cache.put(key, carrier to point)
                carrier to point
            } else {
                fallbackName(mcc, mnc) to null
            }
        } catch (e: Exception) {
            fallbackName(mcc, mnc) to null
        }
    }

    private fun fallbackName(mcc: Int, mnc: Int): String {
        val plmn = String.format("%03d%02d", mcc, mnc)
        return when (plmn) {
            in listOf("310260", "310210", "310220", "310230", "310240", "310250", "310270", "310660", "310200", "310160", "310310") -> "T-Mobile"
            in listOf("310004", "310012", "311480", "311110") -> "Verizon"
            in listOf("310410", "310150", "310170", "310280", "310380", "310560", "311870") -> "AT&T"
            "313100" -> "FirstNet"
            "312670" -> "Dish Wireless"
            "310030", "311220" -> "US Cellular"
            else -> "Carrier $plmn"
        }
    }
}