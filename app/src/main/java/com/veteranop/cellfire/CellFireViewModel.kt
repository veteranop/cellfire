package com.veteranop.cellfire

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.telephony.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veteranop.cellfire.data.local.AppDatabase
import com.veteranop.cellfire.data.local.entities.DiscoveredPci
import com.veteranop.cellfire.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CellFireUiState(
    val cells: List<Cell> = emptyList(),
    val discoveredPcis: List<DiscoveredPci> = emptyList(),
    val signalHistory: Map<Pair<Int, Int>, List<SignalHistoryPoint>> = emptyMap(),
    val logLines: List<String> = emptyList()
)

data class SignalHistoryPoint(val rsrp: Float, val sinr: Float, val timestamp: Long = System.currentTimeMillis())

@HiltViewModel
class CellFireViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    private val telephonyManager = application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val dao = database.discoveredPciDao()

    private val _uiState = MutableStateFlow(CellFireUiState())
    val uiState: StateFlow<CellFireUiState> = _uiState.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _deepScanActive = MutableStateFlow(false)
    val deepScanActive: StateFlow<Boolean> = _deepScanActive.asStateFlow()

    private var phoneStateListener: PhoneStateListener? = null
    private var scanJob: Job? = null

    init {
        loadPersistentData()
    }

    fun toggleMonitoring() {
        if (_isMonitoring.value) stopMonitoring() else startMonitoring()
    }

    fun toggleDeepScan(active: Boolean) {
        _deepScanActive.value = active
        if (_isMonitoring.value) {
            stopMonitoring()
            startMonitoring()
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) startMonitoring()
    }

    private fun startMonitoring() {
        _isMonitoring.value = true
        addLog("Monitoring started - Deep Scan: ${_deepScanActive.value}")

        if (_deepScanActive.value) registerPhoneStateListener()

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && _isMonitoring.value) {
                val startTime = SystemClock.elapsedRealtime()
                scanOnce()
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val delay = if (_deepScanActive.value) 80L else 4000L
                delay((delay - elapsed).coerceAtLeast(50L))
            }
        }
    }

    private fun stopMonitoring() {
        _isMonitoring.value = false
        scanJob?.cancel()
        unregisterPhoneStateListener()
        addLog("Monitoring stopped")
    }

    @SuppressLint("MissingPermission")
    private suspend fun scanOnce() {
        val allCellInfo = telephonyManager.allCellInfo ?: return

        val cells = mutableListOf<Cell>()
        val now = System.currentTimeMillis()

        for (info in allCellInfo) {
            when (info) {
                is CellInfoLte -> cells.add(info.toLteCell())
                is CellInfoNr -> cells.add(info.toNrCell())
                is CellInfoWcdma -> cells.add(info.toWcdmaCell())
                is CellInfoGsm -> cells.add(info.toGsmCell())
            }
        }

        _uiState.value = _uiState.value.copy(cells = cells)

        // PCI discovery and history logic (expand as needed)
        cells.forEach { cell ->
            val key = Pair(cell.pci, cell.arfcn)
            val history = _uiState.value.signalHistory[key]?.toMutableList() ?: mutableListOf()
            history.add(SignalHistoryPoint(cell.signalStrength.toFloat(), cell.signalQuality.toFloat(), now))
            if (history.size > 100) history.removeAt(0)
            _uiState.value = _uiState.value.copy(signalHistory = _uiState.value.signalHistory + (key to history))
        }
    }

    private fun registerPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                viewModelScope.launch(Dispatchers.IO) { scanOnce() }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun unregisterPhoneStateListener() {
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            phoneStateListener = null
        }
    }

    fun updatePci(pci: Int, isIgnored: Boolean? = null, isTargeted: Boolean? = null) = viewModelScope.launch(Dispatchers.IO) {
        val current = dao.getByPci(pci) ?: return@launch
        val updated = current.copy(
            isIgnored = isIgnored ?: current.isIgnored,
            isTargeted = isTargeted ?: current.isTargeted
        )
        dao.update(updated)
        _uiState.value = _uiState.value.copy(
            discoveredPcis = _uiState.value.discoveredPcis.map { if (it.pci == pci) updated else it }
        )
    }

    fun clearPciHistory() = viewModelScope.launch(Dispatchers.IO) {
        dao.clearAll()
        _uiState.value = _uiState.value.copy(discoveredPcis = emptyList())
    }

    fun clearLog() {
        _uiState.value = _uiState.value.copy(logLines = emptyList())
    }

    private fun addLog(line: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _uiState.value = _uiState.value.copy(
            logLines = (_uiState.value.logLines + "$timestamp $line").takeLast(500)
        )
    }

    private fun loadPersistentData() = viewModelScope.launch(Dispatchers.IO) {
        dao.getAll().collect { pcis ->
            _uiState.value = _uiState.value.copy(discoveredPcis = pcis)
        }
    }

    override fun onCleared() {
        stopMonitoring()
        super.onCleared()
    }
}