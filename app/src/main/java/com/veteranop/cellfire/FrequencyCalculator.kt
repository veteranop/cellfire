package com.veteranop.cellfire

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

object FrequencyCalculator {

    private var lteBands: Map<String, BandInfo> = emptyMap()
    private var nrBands: Map<String, BandInfo> = emptyMap()

    fun init(context: Context) {
        val json = Json { ignoreUnknownKeys = true }
        val earfcnJson = context.assets.open("earfcn_frequencies.json").bufferedReader().use { it.readText() }
        val bandData: BandData = json.decodeFromString(earfcnJson)
        lteBands = bandData.lte
        nrBands = bandData.nr
    }

    fun getLteFrequency(earfcn: Int): Pair<Double, Double>? {
        val band = lteBands.values.firstOrNull { earfcn in it.dl.split("-").let { r -> r[0].toInt()..r[1].toInt() } } ?: return null
        val dlOffset = earfcn - band.dl.split("-")[0].toInt()
        val ulOffset = earfcn - band.ul.split("-")[0].toInt()

        val dlFreq = (band.likely_bw as? Double)?.plus(0.1 * dlOffset) ?: 0.0
        val ulFreq = (band.likely_bw as? Double)?.plus(0.1 * ulOffset) ?: 0.0

        return Pair(dlFreq, ulFreq)
    }

    fun getNrFrequency(nrarfcn: Int): Pair<Double, Double>? {
        val band = nrBands.values.firstOrNull { nrarfcn in it.dl.split("-").let { r -> r[0].toInt()..r[1].toInt() } } ?: return null
        val dlOffset = nrarfcn - band.dl.split("-")[0].toInt()
        val ulOffset = nrarfcn - band.ul.split("-")[0].toInt()

        val dlFreq = (band.likely_bw as? Double)?.plus(0.05 * dlOffset) ?: 0.0
        val ulFreq = (band.likely_bw as? Double)?.plus(0.05 * ulOffset) ?: 0.0

        return Pair(dlFreq, ulFreq)
    }
}