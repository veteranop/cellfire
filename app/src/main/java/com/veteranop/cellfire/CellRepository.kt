package com.veteranop.cellfire

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton class to hold the application's cell data and service status.
 * This allows the data to be shared safely between the UI (ViewModels) and background services.
 */
@Singleton
class CellRepository @Inject constructor() {
    private val _cells = MutableStateFlow<List<Cell>>(emptyList())
    val cells = _cells.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive = _isServiceActive.asStateFlow()

    fun updateCells(newCells: List<Cell>) {
        _cells.value = newCells
    }

    fun setServiceActive(isActive: Boolean) {
        _isServiceActive.value = isActive
    }
}