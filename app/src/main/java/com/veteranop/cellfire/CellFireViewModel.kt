package com.veteranop.cellfire

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CellFireViewModel @Inject constructor(
    private val application: Application,
    private val cellRepository: CellRepository
) : ViewModel() {

    val uiState: StateFlow<CellFireUiState> = cellRepository.uiState

    private val _deepScanActive = MutableStateFlow(true)
    val deepScanActive: StateFlow<Boolean> = _deepScanActive.asStateFlow()

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