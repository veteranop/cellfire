package com.veteranop.cellfire

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// We use your existing CellFireUiState from DataModels.kt — NO redeclaration
// We just add deepScanActive as a separate state holder
@HiltViewModel
class CellFireViewModel @Inject constructor(
    private val application: Application,
    private val cellRepository: CellRepository
) : ViewModel() {

    // Keep your original uiState from the repository
    val uiState: StateFlow<CellFireUiState> = cellRepository.uiState

    // Add only the deep scan toggle state
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
        application.startForegroundService(intent)
    }

    fun toggleMonitoring() {
        val intent = Intent(application, CellScanService::class.java).apply {
            action = if (uiState.value.isMonitoring) CellScanService.ACTION_STOP else CellScanService.ACTION_START
        }
        application.startForegroundService(intent)
    }

    fun toggleDeepScan(enable: Boolean) {
        val intent = Intent(application, CellScanService::class.java).apply {
            action = CellScanService.ACTION_TOGGLE_DEEP
            putExtra("enable", enable)
        }
        application.startForegroundService(intent)
        _deepScanActive.value = enable
    }
}