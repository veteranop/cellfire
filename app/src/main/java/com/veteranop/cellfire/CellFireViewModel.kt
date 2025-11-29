package com.veteranop.cellfire

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun updateCarrier(pci: Int, arfcn: Int, newCarrier: String) {
        cellRepository.updateCarrierForPci(pci, arfcn, newCarrier)
    }

    fun updatePci(pci: Int, isIgnored: Boolean? = null, isTargeted: Boolean? = null) {
        cellRepository.updatePciFlags(pci, isIgnored, isTargeted)
    }
}