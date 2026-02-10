package com.veteranop.cellfire

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellRepository @Inject constructor(
    private val discoveredPciDao: DiscoveredPciDao,
    private val driveTestPointDao: DriveTestPointDao,
    @ApplicationContext private val context: Context
) {
    private val _uiState = MutableStateFlow(CellFireUiState())
    val uiState = _uiState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var isDriveTestMode = false

    init {
        repositoryScope.launch {
            discoveredPciDao.getAll().collect { discoveredPcis ->
                _uiState.update { it.copy(discoveredPcis = discoveredPcis) }
            }
        }
    }

    fun updateCells(newCells: List<Cell>) {
        _uiState.update { currentState ->
            val updatedCells = if (currentState.isRecording) {
                currentState.cells + newCells
            } else {
                (currentState.cells + newCells).distinctBy { it.pci to it.arfcn }
            }
            currentState.copy(cells = updatedCells, isRefreshing = false)
        }
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

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setDriveTestMode(enabled: Boolean) {
        isDriveTestMode = enabled
        _uiState.update { it.copy(isDriveTestMode = enabled) }
        Log.d("CellFire", "Drive test mode set to $enabled")
    }

    fun isDriveTestMode() = isDriveTestMode

    fun getAllPointsFlow(): Flow<List<DriveTestPoint>> = driveTestPointDao.getAllPoints()

    suspend fun getAllPointsSync(): List<DriveTestPoint> {
        return driveTestPointDao.getAllPoints().first()
    }

    fun getPointsForPci(pci: Int): Flow<List<DriveTestPoint>> = driveTestPointDao.getPointsForPci(pci)

    suspend fun getPointsForPciSync(pci: Int): List<DriveTestPoint> {
        return driveTestPointDao.getPointsForPci(pci).first()
    }

    suspend fun exportPciCsv(pci: Int): File {
        val points = getPointsForPciSync(pci)
        val csv = buildString {
            appendLine("Timestamp,Latitude,Longitude,Carrier,Band,PCI,RSRP_dBm,SNR_dB")
            points.forEach { point ->
                appendLine("${point.timestamp},${point.latitude},${point.longitude},${point.carrier},${point.band},${point.pci},${point.rsrp},${point.snr}")
            }
        }
        val file = File(context.cacheDir, "pci_${pci}_drive.csv")
        FileWriter(file).use { it.write(csv) }
        Log.d("CellFire", "Exported ${points.size} points for PCI $pci to ${file.absolutePath}")
        return file
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
            _uiState.update { it.copy(cells = emptyList(), signalHistory = emptyMap()) }
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

        if (isDriveTestMode) {
            Log.d("CellFire", "Drive test active, fetching location for ${cells.size} cells")
            val location = try {
                fusedLocationClient.lastLocation.await()
            } catch (e: Exception) {
                null
            }

            if (location != null) {
                val points = cells.mapNotNull { cell ->
                    // Use signalStrength and signalQuality instead of empty rsrp/snr fields
                    if (cell.signalStrength != Int.MIN_VALUE && cell.signalStrength != 0) {
                        DriveTestPoint(
                            timestamp = now,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            pci = cell.pci,
                            rsrp = cell.signalStrength,
                            snr = cell.signalQuality,
                            band = cell.band,
                            carrier = cell.carrier
                        )
                    } else null
                }
                if (points.isNotEmpty()) {
                    driveTestPointDao.insertAll(points)
                    Log.d("CellFire", "Inserted ${points.size} points to DB")
                }
            } else {
                Log.w("CellFire", "No location available for drive test logging")
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
                history.add(SignalHistoryPoint(now, cell.signalStrength, cell.signalQuality, cell.rsrq))

                newHistory[key] = history.takeLast(100)
            }
            currentState.copy(signalHistory = newHistory)
        }
    }
}