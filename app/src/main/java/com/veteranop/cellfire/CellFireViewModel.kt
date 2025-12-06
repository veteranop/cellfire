package com.veteranop.cellfire

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CellFireViewModel @Inject constructor(
    private val application: Application,
    private val cellRepository: CellRepository
) : ViewModel() {

    val uiState: StateFlow<CellFireUiState> = cellRepository.uiState

    private val _deepScanActive = MutableStateFlow(true)
    val deepScanActive: StateFlow<Boolean> = _deepScanActive.asStateFlow()

    private val _exportEvent = Channel<File>(Channel.CONFLATED)
    val exportEvent: Flow<File> = _exportEvent.receiveAsFlow()

    fun onPermissionsResult(granted: Boolean) {
        cellRepository.setPermissionsGranted(granted)
        if (granted) {
            startMonitoring()
        }
    }

    fun startMonitoring() {
        val intent = Intent(application, CellScanService::class.java).apply {
            action = CellScanService.ACTION_START
        }
        ContextCompat.startForegroundService(application, intent)
    }

    fun toggleMonitoring() {
        val intent = Intent(application, CellScanService::class.java).apply {
            action = if (uiState.value.isMonitoring) CellScanService.ACTION_STOP else CellScanService.ACTION_START
        }
        ContextCompat.startForegroundService(application, intent)
    }

    fun toggleDeepScan(enable: Boolean) {
        val intent = Intent(application, CellScanService::class.java).apply {
            action = CellScanService.ACTION_TOGGLE_DEEP
            putExtra("enable", enable)
        }
        ContextCompat.startForegroundService(application, intent)
        _deepScanActive.value = enable
    }

    fun refresh() {
        viewModelScope.launch {
            cellRepository.setRefreshing(true)
            val intent = Intent(application, CellScanService::class.java).apply {
                action = CellScanService.ACTION_REFRESH
            }
            ContextCompat.startForegroundService(application, intent)
            // The service will set refreshing to false when it's done
        }
    }

    fun exportCarrierWithHistory(carrier: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cells = uiState.value.cells.filter { it.carrier.equals(carrier, ignoreCase = true) }
            val historyMap = uiState.value.signalHistory

            val csv = buildString {
                appendLine("Timestamp,Latitude,Longitude,Carrier,Band,PCI,EARFCN,RSRP_dBm,SINR_dB,RSRQ_dB,Registered,TAC,Type,History_RSRP_List")
                cells.forEach { cell ->
                    val key = Pair(cell.pci, cell.arfcn)
                    val history = historyMap[key]?.joinToString(";") { "${it.rsrp}" } ?: ""
                    appendLine(
                        "${System.currentTimeMillis()}," +
                                "${cell.latitude}," +
                                "${cell.longitude}," +
                                "\"${cell.carrier}\"," +
                                "\"${cell.band}\"," +
                                "${cell.pci}," +
                                "${cell.arfcn}," +
                                "${cell.signalStrength}," +
                                "${cell.signalQuality}," +
                                "${cell.rsrq}," +
                                "${cell.isRegistered}," +
                                "${cell.tac}," +
                                "\"${cell.type}\"," +
                                "\"$history\""
                    )
                }
            }

            val fileName = "CellFire_${carrier.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            val file = File(application.getExternalFilesDir(null), fileName)
            file.writeText(csv)

            withContext(Dispatchers.Main) {
                _exportEvent.trySend(file)
            }
        }
    }

    fun exportAllWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val cells = uiState.value.cells
            val historyMap = uiState.value.signalHistory

            val csv = buildString {
                appendLine("Timestamp,Latitude,Longitude,Carrier,Band,PCI,EARFCN,RSRP_dBm,SINR_dB,RSRQ_dB,Registered,TAC,Type,History_RSRP_List")
                cells.forEach { cell ->
                    val key = Pair(cell.pci, cell.arfcn)
                    val history = historyMap[key]?.joinToString(";") { "${it.rsrp}" } ?: ""
                    appendLine(
                        "${System.currentTimeMillis()}," +
                                "${cell.latitude}," +
                                "${cell.longitude}," +
                                "\"${cell.carrier}\"," +
                                "\"${cell.band}\"," +
                                "${cell.pci}," +
                                "${cell.arfcn}," +
                                "${cell.signalStrength}," +
                                "${cell.signalQuality}," +
                                "${cell.rsrq}," +
                                "${cell.isRegistered}," +
                                "${cell.tac}," +
                                "\"${cell.type}\"," +
                                "\"$history\""
                    )
                }
            }

            val fileName = "CellFire_ALL_${System.currentTimeMillis()}.csv"
            val file = File(application.getExternalFilesDir(null), fileName)
            file.writeText(csv)

            withContext(Dispatchers.Main) {
                _exportEvent.trySend(file)
            }
        }
    }

    fun updateCarrier(pci: Int, band: String, newCarrier: String) {
        cellRepository.updateCarrierForPci(pci, band, newCarrier)
    }

    fun updatePci(pci: Int, band: String, isIgnored: Boolean? = null, isTargeted: Boolean? = null) {
        cellRepository.updatePciFlags(pci, band, isIgnored, isTargeted)
    }

    fun clearLog() {
        cellRepository.clearLog()
    }

    fun clearPciHistory() {
        cellRepository.clearPciHistory()
    }
}