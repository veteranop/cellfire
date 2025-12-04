package com.veteranop.cellfire.domain.model

import android.telephony.*

sealed class Cell {
    abstract val pci: Int
    abstract val arfcn: Int
    abstract val band: String
    abstract val carrier: String
    abstract val signalStrength: Int
    abstract val signalQuality: Int
    abstract val isRegistered: Boolean
    abstract val type: String
}

data class LteCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val carrier: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean
) : Cell() { override val type = "LTE" }

data class NrCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val carrier: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean
) : Cell() { override val type = "5G NR" }

fun CellInfoLte.toLteCell(): LteCell {
    val id = cellIdentity as CellIdentityLte
    return LteCell(
        pci = id.pci,
        arfcn = id.earfcn,
        band = "B${com.veteranop.cellfire.FrequencyCalculator.getLteBand(id.earfcn)}",
        carrier = getCarrier(id.mccString, id.mncString),
        signalStrength = cellSignalStrength.dbm,
        signalQuality = cellSignalStrength.level,
        isRegistered = isRegistered
    )
}

fun CellInfoNr.toNrCell(): NrCell {
    val id = cellIdentity as CellIdentityNr
    return NrCell(
        pci = id.pci,
        arfcn = id.nrarfcn,
        band = "n${com.veteranop.cellfire.FrequencyCalculator.getNrBand(id.nrarfcn)}",
        carrier = getCarrier(id.mccString, id.mncString),
        signalStrength = cellSignalStrength.dbm,
        signalQuality = cellSignalStrength.level,
        isRegistered = isRegistered
    )
}

private fun getCarrier(mcc: String?, mnc: String?): String {
    val code = "$mcc$mnc"
    return when (code) {
        "310410", "311480", "312530" -> "T-Mobile"
        "311870" -> "T-Mobile (Low-Band)"
        "310030", "310150" -> "AT&T"
        "312670" -> "FirstNet"
        "310004", "310012" -> "Verizon"
        "310120" -> "Dish Wireless"
        else -> "Unknown"
    }
}