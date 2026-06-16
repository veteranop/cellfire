package com.veteranop.cellfire

/**
 * LTE EARFCN → frequency per 3GPP TS 36.101 Table 5.4.4-1
 * NR-ARFCN  → frequency per 3GPP TS 38.104 Table 5.4.2.1-1
 *
 * F_DL = F_DL_low + 0.1 * (EARFCN - N_Offs_DL)   [MHz, 100 kHz steps]
 */
object FrequencyCalculator {

    fun earfcnToFrequency(earfcn: Int): Pair<Double, Double>? {
        return when (earfcn) {
            // Band 1  — 2100 FDD  DL 2110–2170 / UL 1920–1980
            in 0..599         -> dlUl(2110.0,    0,   1920.0, earfcn)
            // Band 2  — 1900 PCS FDD  DL 1930–1990 / UL 1850–1910
            in 600..1199      -> dlUl(1930.0,  600,   1850.0, earfcn)
            // Band 3  — 1800 FDD  DL 1805–1880 / UL 1710–1785
            in 1200..1949     -> dlUl(1805.0, 1200,   1710.0, earfcn)
            // Band 4  — AWS-1 FDD  DL 2110–2155 / UL 1710–1755
            in 1950..2399     -> dlUl(2110.0, 1950,   1710.0, earfcn)
            // Band 5  — 850 CLR FDD  DL 869–894 / UL 824–849
            in 2400..2649     -> dlUl(869.0,  2400,    824.0, earfcn)
            // Band 7  — 2600 FDD  DL 2620–2690 / UL 2500–2570
            in 2750..3449     -> dlUl(2620.0, 2750,   2500.0, earfcn)
            // Band 12 — 700 Lower B FDD  DL 729–746 / UL 699–716  (AT&T, T-Mobile)
            in 5010..5179     -> dlUl(729.0,  5010,    699.0, earfcn)
            // Band 13 — 700 Upper C FDD  DL 746–756 / UL 777–787  (Verizon)
            in 5180..5279     -> dlUl(746.0,  5180,    777.0, earfcn)
            // Band 14 — 700 Upper D FDD  DL 758–768 / UL 788–798  (FirstNet/AT&T)
            in 5280..5379     -> dlUl(758.0,  5280,    788.0, earfcn)
            // Band 17 — 700 Lower B/C FDD  DL 734–746 / UL 704–716  (AT&T)
            in 5730..5849     -> dlUl(734.0,  5730,    704.0, earfcn)
            // Band 25 — 1900 PCS+ FDD  DL 1930–1995 / UL 1850–1915  (T-Mobile, Verizon)
            in 8040..8689     -> dlUl(1930.0, 8040,   1850.0, earfcn)
            // Band 26 — 850 CLR+ FDD  DL 859–894 / UL 814–849  (US Cellular)
            in 8690..9039     -> dlUl(859.0,  8690,    814.0, earfcn)
            // Band 29 — 700 D FDD DL-only  DL 717–728  (AT&T/Verizon supplemental DL)
            in 9660..9769     -> dl(717.0,    9660,           earfcn)
            // Band 30 — WCS FDD  DL 2350–2360 / UL 2305–2315  (AT&T)
            in 9770..9869     -> dlUl(2350.0, 9770,   2305.0, earfcn)
            // Band 41 — 2500 TDD  DL/UL 2496–2690  (T-Mobile/Sprint)
            in 39650..41589   -> tdd(2496.0,  39650,          earfcn)
            // Band 48 — CBRS TDD  DL/UL 3550–3700
            in 55240..56739   -> tdd(3550.0,  55240,          earfcn)
            // Band 66 — AWS-3 FDD extended  DL 2110–2200 / UL 1710–1780  (T-Mobile, Verizon, AT&T)
            in 66436..67335   -> dlUl(2110.0, 66436,  1710.0, earfcn)
            // Band 71 — 600 MHz FDD  DL 617–652 / UL 663–698  (T-Mobile)
            in 68586..68935   -> dlUl(617.0,  68586,   663.0, earfcn)
            else -> null
        }
    }

    /**
     * NR-ARFCN to (DL MHz, UL MHz) per 3GPP TS 38.104 Table 5.4.2.1-1.
     * TDD bands: UL == DL. FDD bands: UL offset applied per band.
     */
    fun nrarfcnToFrequency(nrarfcn: Int): Pair<Double, Double>? {
        val dl = when {
            nrarfcn in 0..599999       -> nrarfcn * 0.005
            nrarfcn in 600000..2016666 -> 3000.0 + (nrarfcn - 600000) * 0.015
            nrarfcn >= 2016667         -> 24250.08 + (nrarfcn - 2016667) * 0.06
            else                       -> return null
        }
        val ul = when (nrarfcn) {
            // FDD bands — UL offset from DL
            in 386000..398000   -> dl - 80.0    // n2   FDD  DL 1930–1990 / UL 1850–1910
            in 173800..178800   -> dl - 45.0    // n5   FDD  DL 869–894   / UL 824–849
            in 145800..149200   -> dl - 30.0    // n12  FDD  DL 729–746   / UL 699–716
            in 149200..151200   -> dl + 31.0    // n13  FDD  DL 746–756   / UL 777–787
            in 151600..153600   -> dl + 30.0    // n14  FDD  DL 758–768   / UL 788–798
            in 146800..149200   -> dl - 30.0    // n17  FDD  DL 734–746   / UL 704–716
            in 386000..399000   -> dl - 80.0    // n25  FDD  DL 1930–1995 / UL 1850–1915
            in 171800..178800   -> dl - 45.0    // n26  FDD  DL 859–894   / UL 814–849
            in 422000..440000   -> dl - 400.0   // n66  FDD  DL 2110–2200 / UL 1710–1780
            in 123400..130400   -> dl + 46.0    // n71  FDD  DL 617–652   / UL 663–698
            // TDD bands — UL == DL
            in 499200..537999   -> dl            // n41  TDD  2496–2690 MHz
            in 636667..646667   -> dl            // n48  TDD  3550–3700 MHz  (CBRS)
            in 620000..680000   -> dl            // n77/n78  TDD  3300–4200 MHz
            in 2229166..2279166 -> dl            // n260 mmWave TDD  37–40 GHz
            in 2070832..2084999 -> dl            // n261 mmWave TDD  27.5–28.35 GHz
            else                -> dl            // unknown / other TDD — assume UL == DL
        }
        return dl to ul
    }

    // FDD: UL derived from duplex gap  (ul = dl − (dlLow − ulLow))
    private fun dlUl(dlLow: Double, nDlLow: Int, ulLow: Double, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        val ul = dl - (dlLow - ulLow)
        return dl to ul
    }

    // DL-only (e.g. Band 29 supplemental DL): UL returned as 0.0 so UI skips it
    private fun dl(dlLow: Double, nDlLow: Int, earfcn: Int): Pair<Double, Double> {
        val dl = dlLow + 0.1 * (earfcn - nDlLow)
        return dl to 0.0
    }

    // TDD: UL == DL (same channel used for both directions)
    private fun tdd(dlLow: Double, nDlLow: Int, earfcn: Int): Pair<Double, Double> {
        val f = dlLow + 0.1 * (earfcn - nDlLow)
        return f to f
    }
}
