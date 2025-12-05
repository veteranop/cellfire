package com.veteranop.cellfire

object FrequencyCalculator {

    fun earfcnToFrequency(earfcn: Int): Pair<Double, Double>? {
        return when (earfcn) {
            // FDD Bands
            in 600..1199   -> dlUl(1930.0, 600, 1850.0, earfcn)   // Band 2
            in 1950..2399  -> dl(2110.0, 1950, earfcn)                  // Band 4 (DL only for this range)
            in 2400..2649  -> dlUl(869.0,  2400, 824.0, earfcn)   // Band 5
            in 5010..5179  -> dlUl(729.0,  5010, 699.0, earfcn)   // Band 12/17
            in 5180..5279  -> dlUl(746.0,  5180, 777.0, earfcn)   // Band 13
            in 5280..5379  -> dlUl(758.0,  5280, 788.0, earfcn)   // Band 14 (FirstNet)
            in 66436..67335 -> dlUl(2110.0, 66436, 1710.0, earfcn) // Band 66 (AWS-3)
            in 68586..68935 -> dlUl(617.0,  68586, 663.0, earfcn) // Band 71 (600 MHz)
            else -> null
        }
    }

    private fun dlUl(dlLow: Double, nDlLow: Int, ulLow: Double, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        // Calculate UL based on DL and the band's defined duplex separation
        val ul = dl - (dlLow - ulLow)
        return dl to ul
    }

    private fun dl(dlLow: Double, nDlLow: Int, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        return dl to 0.0 // No UL frequency for this specific range
    }
}