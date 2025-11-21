package com.veteranop.cellfire

// ==================================================
// DATA MODELS
// ==================================================

data class BandInfo(
    val dl: String,
    val ul: String,
    val bw: List<Any>,
    val likely_bw: Any
)

data class BandData(
    val lte: Map<String, BandInfo>,
    val nr: Map<String, BandInfo>
)

sealed class Cell {
    abstract val pci: Int
    abstract val arfcn: Int
    abstract val band: String
    abstract val signalStrength: Int
    abstract val signalQuality: Int
    abstract val isRegistered: Boolean
    abstract val carrier: String
    abstract val type: String
    abstract val tac: Int
    abstract val lastSeen: Long
}

data class LteCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean,
    override val carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis()
) : Cell() {
    override val type: String = "LTE"
}

data class NrCell(
    override val pci: Int,
    override val arfcn: Int,
    override val band: String,
    override val signalStrength: Int,
    override val signalQuality: Int,
    override val isRegistered: Boolean,
    override val carrier: String,
    override val tac: Int,
    override val lastSeen: Long = System.currentTimeMillis()
) : Cell() {
    override val type: String = "5G NR"
}

data class CellFireUiState(
    val allPermissionsGranted: Boolean = false,
    val isMonitoring: Boolean = false,
    val cells: List<Cell> = emptyList(),
    val logLines: List<String> = emptyList(),
    val selectedCarriers: Set<String> = setOf("T-Mobile", "Verizon", "AT&T"),
    val registeredCarrierName: String = "Unknown"
)
