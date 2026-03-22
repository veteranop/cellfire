package com.veteranop.cellfire

object FrequencyCalculator {

    fun earfcnToFrequency(earfcn: Int): Pair<Double, Double>? {
        return when (earfcn) {
            in 600..1199    -> dlUl(1930.0, 600,   1850.0, earfcn)
            in 1950..2399   -> dl(2110.0,   1950,          earfcn)
            in 2400..2649   -> dlUl(869.0,  2400,   824.0, earfcn)
            in 5010..5179   -> dlUl(729.0,  5010,   699.0, earfcn)
            in 5180..5279   -> dlUl(746.0,  5180,   777.0, earfcn)
            in 5280..5379   -> dlUl(758.0,  5280,   788.0, earfcn)
            in 66436..67335 -> dlUl(2110.0, 66436, 1710.0, earfcn)
            in 68586..68935 -> dlUl(617.0,  68586,  663.0, earfcn)
            else -> null
        }
    }

    /**
     * Convert NR-ARFCN to (DL MHz, UL MHz) per 3GPP TS 38.104.
     * TDD bands: UL == DL (same channel). FDD bands: UL computed from duplex gap.
     */
    fun nrarfcnToFrequency(nrarfcn: Int): Pair<Double, Double>? {
        val dl = when {
            nrarfcn in 0..599999       -> nrarfcn * 0.005
            nrarfcn in 600000..2016666 -> 3000.0 + (nrarfcn - 600000) * 0.015
            nrarfcn >= 2016667         -> 24250.08 + (nrarfcn - 2016667) * 0.06
            else                       -> return null
        }
        val ul = when (nrarfcn) {
            in 620000..646666   -> dl           // n77, n78 — TDD
            in 509202..537999   -> dl           // n41     — TDD
            in 2269584..2308333 -> dl           // n260, n261 mmWave — TDD
            in 370000..384000   -> dl - 80.0    // n2  FDD  DL 1930-1990 / UL 1850-1910
            in 173800..178000   -> dl - 45.0    // n5  FDD  DL 869-894   / UL 824-849
            in 384000..404000   -> dl - 400.0   // n66 FDD  DL 2110-2200 / UL 1710-1780
            in 418000..434000   -> dl - 80.0    // n25 FDD  DL 1930-1995 / UL 1850-1915
            in 122400..131400   -> dl + 46.0    // n71 FDD  DL 617-652   / UL 663-698
            else                -> dl           // unknown — assume TDD
        }
        return dl to ul
    }

    private fun dlUl(dlLow: Double, nDlLow: Int, ulLow: Double, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        val ul = dl - (dlLow - ulLow)
        return dl to ul
    }

    private fun dl(dlLow: Double, nDlLow: Int, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        return dl to 0.0
    }
}
