package com.veteranop.cellfire

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellRepository @Inject constructor(
    private val discoveredPciDao: DiscoveredPciDao
) {
    private val _uiState = MutableStateFlow(CellFireUiState())
    val uiState = _uiState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        repositoryScope.launch {
            discoveredPciDao.getAll().collect { discoveredPcis ->
                _uiState.update { it.copy(discoveredPcis = discoveredPcis) }
            }
        }
    }

    fun updateCells(newCells: List<Cell>) {
        _uiState.update { it.copy(cells = newCells, isRefreshing = false) }
        addSignalHistory(newCells)
        repositoryScope.launch {
            updateDiscoveredPcis(newCells)
        }
    }

    fun setServiceActive(isActive: Boolean) {
        _uiState.update { it.copy(isMonitoring = isActive) }
    }

    fun setPermissionsGranted(areGranted: Boolean) {
        _uiState.update { it.copy(allPermissionsGranted = areGranted) }
    }

    fun setRefreshing(isRefreshing: Boolean) {
        _uiState.update { it.copy(isRefreshing = isRefreshing) }
    }

    fun addLogLine(logLine: String) {
        val now = System.currentTimeMillis()
        val newEntry = LogEntry(now, logLine)
        _uiState.update { currentState ->
            val oneMinuteAgo = now - 60_000
            val updatedLogs = (listOf(newEntry) + currentState.logLines).filter { it.timestamp >= oneMinuteAgo }
            currentState.copy(logLines = updatedLogs)
        }
    }

    fun clearLog() {
        _uiState.update { it.copy(logLines = emptyList()) }
    }

    fun clearPciHistory() {
        repositoryScope.launch {
            discoveredPciDao.clearAll()
        }
    }

    fun updateCarrierForPci(pci: Int, band: String, newCarrier: String) {
        _uiState.update { currentState ->
            val newCells = currentState.cells.map { cell ->
                if (cell.pci == pci && cell.band == band) {
                    cell.carrier = newCarrier
                }
                cell
            }
            currentState.copy(cells = newCells)
        }
        repositoryScope.launch {
            val discovered = discoveredPciDao.getDiscoveredPci(pci, band)
            if (discovered != null) {
                discovered.carrier = newCarrier
                discoveredPciDao.insert(discovered)
            }
        }
    }

    fun updatePciFlags(pci: Int, band: String, isIgnored: Boolean? = null, isTargeted: Boolean? = null) {
        repositoryScope.launch {
            val discovered = discoveredPciDao.getDiscoveredPci(pci, band)
            if (discovered != null) {
                isIgnored?.let { discovered.isIgnored = it }
                isTargeted?.let { discovered.isTargeted = it }
                discoveredPciDao.insert(discovered)
            }
        }
    }

    private suspend fun updateDiscoveredPcis(cells: List<Cell>) {
        val now = System.currentTimeMillis()
        for (cell in cells) {
            val existingPci = discoveredPciDao.getDiscoveredPci(cell.pci, cell.band)
            if (existingPci != null) {
                existingPci.discoveryCount++
                existingPci.lastSeen = now
                discoveredPciDao.insert(existingPci)
            } else {
                discoveredPciDao.insert(
                    DiscoveredPci(
                        pci = cell.pci,
                        carrier = cell.carrier,
                        band = cell.band,
                        discoveryCount = 1,
                        lastSeen = now
                    )
                )
            }
        }
    }

    private fun addSignalHistory(cells: List<Cell>) {
        val now = System.currentTimeMillis()
        _uiState.update { currentState ->
            val newHistory = currentState.signalHistory.toMutableMap()

            for (cell in cells) {
                val key = Pair(cell.pci, cell.arfcn)
                val history = newHistory.getOrDefault(key, emptyList()).toMutableList()
                history.add(SignalHistoryPoint(now, cell.signalStrength, cell.signalQuality))

                newHistory[key] = history.takeLast(100)
            }
            currentState.copy(signalHistory = newHistory)
        }
    }
}