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

data class WcdmaCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val carrier: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean
) : Cell() { override val type = "WCDMA" }

data class GsmCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val carrier: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean
) : Cell() { override val type = "GSM" }

// Extensions — these were missing
fun CellInfoLte.toLteCell() = LteCell(
    pci = (cellIdentity as CellIdentityLte).pci,
    arfcn = (cellIdentity as CellIdentityLte).earfcn,
    band = "B${com.veteranop.cellfire.FrequencyCalculator.getLteBand((cellIdentity as CellIdentityLte).earfcn)}",
    carrier = getCarrier((cellIdentity as CellIdentityLte).mccString, (cellIdentity as CellIdentityLte).mncString),
    signalStrength = cellSignalStrength.dbm,
    signalQuality = cellSignalStrength.level,
    isRegistered = isRegistered
)

fun CellInfoNr.toNrCell() = NrCell(
    pci = (cellIdentity as CellIdentityNr).pci,
    arfcn = (cellIdentity as CellIdentityNr).nrarfcn,
    band = "n${com.veteranop.cellfire.FrequencyCalculator.getNrBand((cellIdentity as CellIdentityNr).nrarfcn)}",
    carrier = getCarrier((cellIdentity as CellIdentityNr).mccString, (cellIdentity as CellIdentityNr).mncString),
    signalStrength = cellSignalStrength.dbm,
    signalQuality = cellSignalStrength.level,
    isRegistered = isRegistered
)

fun CellInfoWcdma.toWcdmaCell() = WcdmaCell(
    pci = (cellIdentity as CellIdentityWcdma).psc,
    arfcn = (cellIdentity as CellIdentityWcdma).uarfcn,
    band = "WCDMA",
    carrier = getCarrier((cellIdentity as CellIdentityWcdma).mccString, (cellIdentity as CellIdentityWcdma).mncString),
    signalStrength = cellSignalStrength.dbm,
    signalQuality = cellSignalStrength.level,
    isRegistered = isRegistered
)

fun CellInfoGsm.toGsmCell() = GsmCell(
    pci = (cellIdentity as CellIdentityGsm).cid,
    arfcn = (cellIdentity as CellIdentityGsm).arfcn,
    band = "GSM",
    carrier = getCarrier((cellIdentity as CellIdentityGsm).mccString, (cellIdentity as CellIdentityGsm).mncString),
    signalStrength = cellSignalStrength.dbm,
    signalQuality = cellSignalStrength.level,
    isRegistered = isRegistered
)

private fun getCarrier(mcc: String?, mnc: String?): String {
    val code = "$mcc$mnc"
    return when (code) {
        "310410", "311480" -> "T-Mobile"
        "311870" -> "T-Mobile (Low-Band)"
        "310030" -> "AT&T"
        "312670" -> "FirstNet"
        "310004" -> "Verizon"
        "310120" -> "Dish Wireless"
        else -> "Unknown"
    }
}