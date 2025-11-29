package com.veteranop.cellfire

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton class to hold the application's cell data and service status.
 * This allows the data to be shared safely between the UI (ViewModels) and background services.
 */
@Singleton
class CellRepository @Inject constructor() {
    private val _uiState = MutableStateFlow(CellFireUiState())
    val uiState = _uiState.asStateFlow()

    fun updateCells(newCells: List<Cell>) {
        _uiState.update { it.copy(cells = newCells) }
    }

    fun setServiceActive(isActive: Boolean) {
        _uiState.update { it.copy(isMonitoring = isActive) }
    }

    fun setPermissionsGranted(areGranted: Boolean) {
        _uiState.update { it.copy(allPermissionsGranted = areGranted) }
    }
}