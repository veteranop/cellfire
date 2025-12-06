package com.veteranop.cellfire

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class BandInfo(
    val dl: String,
    val ul: String,
    val bw: List<Double>,
    val likely_bw: Double
)

@Serializable
data class BandsHolder(
    val bands: Map<String, BandInfo>
)

@Serializable
data class BandData(
    val lte: BandsHolder,
    val nr: BandsHolder
)

sealed class Cell {
    abstract val pci: Int
    abstract val arfcn: Int
    abstract val band: String
    abstract val signalStrength: Int
    abstract val signalQuality: Int
    abstract val rsrq: Int
    abstract val isRegistered: Boolean
    abstract var carrier: String
    abstract val type: String
    abstract val tac: Int
    abstract val lastSeen: Long
    abstract val latitude: Double
    abstract val longitude: Double
}

data class LteCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val rsrq: Int,
    override val isRegistered: Boolean,
    override var carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis(),
    override val latitude: Double,
    override val longitude: Double
) : Cell() {
    override val type: String = "LTE"
}

data class NrCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val rsrq: Int,
    override val isRegistered: Boolean,
    override var carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis(),
    override val latitude: Double,
    override val longitude: Double
) : Cell() {
    override val type: String = "5G NR"
}

data class WcdmaCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val rsrq: Int = 0,
    override val isRegistered: Boolean,
    override var carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis(),
    override val latitude: Double,
    override val longitude: Double
) : Cell() {
    override val type: String = "WCDMA"
}

data class GsmCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val rsrq: Int = 0,
    override val isRegistered: Boolean,
    override var carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis(),
    override val latitude: Double,
    override val longitude: Double
) : Cell() {
    override val type: String = "GSM"
}

@Entity(tableName = "discovered_pcis")
data class DiscoveredPci(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pci: Int,
    var carrier: String,
    var band: String,
    var discoveryCount: Int,
    var lastSeen: Long,
    var isIgnored: Boolean = false,
    var isTargeted: Boolean = false
)

data class SignalHistoryPoint(val timestamp: Long, val rsrp: Int, val sinr: Int, val rsrq: Int)

data class LogEntry(val timestamp: Long, val message: String)

data class CellFireUiState(
    val allPermissionsGranted: Boolean = false,
    val isMonitoring: Boolean = false,
    val cells: List<Cell> = emptyList(),
    val logLines: List<LogEntry> = emptyList(),
    val selectedCarriers: Set<String> = setOf("T-Mobile", "Verizon", "AT&T", "Dish", "FirstNet", "US Cellular"),
    val registeredCarrierName: String = "Unknown",
    val discoveredPcis: List<DiscoveredPci> = emptyList(),
    val signalHistory: Map<Pair<Int, Int>, List<SignalHistoryPoint>> = emptyMap(),
    val isRefreshing: Boolean = false
)
