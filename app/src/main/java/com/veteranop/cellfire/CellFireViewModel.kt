package com.veteranop.cellfire

import android.app.Application
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veteranop.cellfire.data.local.AppDatabase
import com.veteranop.cellfire.data.local.entities.DiscoveredPci
import com.veteranop.cellfire.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CellFireUiState(
    val cells: List<Cell> = emptyList(),
    val discoveredPcis: List<DiscoveredPci> = emptyList(),
    val signalHistory: Map<Pair<Int, Int>, List<SignalHistoryPoint>> = emptyMap(),
    val logLines: List<String> = emptyList()
)

data class SignalHistoryPoint(
    val rsrp: Float,
    val sinr: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class CellFireViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    private val tm = application.getSystemService(TelephonyManager::class.java)
    private val dao = database.discoveredPciDao()

    private val _uiState = MutableStateFlow(CellFireUiState())
    val uiState = _uiState.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()

    private val _deepScanActive = MutableStateFlow(false)
    val deepScanActive = _deepScanActive.asStateFlow()

    private var listener: PhoneStateListener? = null
    private var job: Job? = null

    fun toggleMonitoring() {
        if (_isMonitoring.value) stop() else start()
    }

    fun toggleDeepScan(active: Boolean) {
        _deepScanActive.value = active
        if (_isMonitoring.value) { stop(); start() }
    }

    private fun start() {
        _isMonitoring.value = true
        addLog("ENGAGE TARGETS — ${if (_deepScanActive.value) "MAXIMUM OVERDRIVE" else "STANDARD"}")

        if (_deepScanActive.value) {
            listener = PhoneStateListener { viewModelScope.launch(Dispatchers.IO) { scan() } }
            tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                scan()
                delay(if (_deepScanActive.value) 80L else 4000L)
            }
        }
    }

    private fun stop() {
        _isMonitoring.value = false
        job?.cancel()
        listener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        listener = null
        addLog("CEASE FIRE")
    }

    private suspend fun scan() {
        val info = tm.allCellInfo ?: return
        val cells = info.mapNotNull {
            when (it) {
                is android.telephony.CellInfoLte -> it.toLteCell()
                is android.telephony.CellInfoNr -> it.toNrCell()
                else -> null
            }
        }
        _uiState.value = _uiState.value.copy(cells = cells)
        // PCI discovery logic here (add later)
    }

    private fun addLog(line: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        _uiState.value = _uiState.value.copy(logLines = (_uiState.value.logLines + "$time $line").takeLast(500))
    }

    override fun onCleared() { stop(); super.onCleared() }
}