package com.veteranop.cellfire

object FrequencyCalculator {
    fun getLteBand(earfcn: Int) = when (earfcn) {
        in 2400..2649 -> 5
        in 66436..67335 -> 71
        else -> earfcn / 100
    }

    fun getNrBand(nrarfcn: Int) = when {
        nrarfcn in 422000..434000 -> 71
        else -> nrarfcn / 1000
    }
}