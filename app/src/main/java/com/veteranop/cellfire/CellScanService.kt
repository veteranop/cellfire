package com.veteranop.cellfire

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CellScanService : LifecycleService() {

    @Inject lateinit var cellRepository: CellRepository

    private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private var scanningJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "cellfire_scan"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startScanning()
            ACTION_STOP -> stopScanning()
        }
        return START_STICKY
    }

    private fun startScanning() {
        if (scanningJob?.isActive == true) return

        cellRepository.setServiceActive(true)

        scanningJob = lifecycleScope.launch {
            while (true) {
                updateCellInfo()
                delay(1800)
            }
        }
    }

    private fun stopScanning() {
        scanningJob?.cancel()
        cellRepository.setServiceActive(false)
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun updateCellInfo() {
        val cellInfoList = telephonyManager.allCellInfo ?: return
        val newCells = cellInfoList.mapNotNull { parseCellInfo(it) }

        cellRepository.updateCells(newCells)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CellFire Scanning",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Scanning for cell towers" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CellFire Active")
            .setContentText("Scanning towers...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun parseCellInfo(info: CellInfo): Cell? {
        return when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity
                val sig = info.cellSignalStrength

                val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString?.toIntOrNull() ?: 0 else id.mcc.takeIf { it != Int.MAX_VALUE } ?: 0
                val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString?.toIntOrNull() ?: 0 else id.mnc.takeIf { it != Int.MAX_VALUE } ?: 0

                val rsrp = sig.rsrp.coerceIn(-140, -44)
                val rssnr = if (sig.rssnr != Int.MAX_VALUE) sig.rssnr / 10 else 0
                val band = earfcnToLteBand(id.earfcn)
                val carrier = plmnToCarrier(mcc, mnc)

                LteCell(
                    pci = id.pci,
                    arfcn = id.earfcn,
                    band = band,
                    signalStrength = rsrp,
                    signalQuality = rssnr,
                    isRegistered = info.isRegistered,
                    carrier = carrier,
                    tac = id.tac
                )
            }
            is CellInfoNr -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val id = info.cellIdentity as CellIdentityNr
                val sig = info.cellSignalStrength as CellSignalStrengthNr

                val mcc = id.mccString?.toIntOrNull() ?: 0
                val mnc = id.mncString?.toIntOrNull() ?: 0

                val rsrp = sig.ssRsrp.coerceIn(-140, -44)
                val sinr = if (sig.ssSinr != Int.MAX_VALUE) sig.ssSinr / 10 else 0
                val band = nrarfcnToNrBand(id.nrarfcn)
                val carrier = plmnToCarrier(mcc, mnc)

                NrCell(
                    pci = id.pci,
                    arfcn = id.nrarfcn,
                    band = band,
                    signalStrength = rsrp,
                    signalQuality = sinr,
                    isRegistered = info.isRegistered,
                    carrier = carrier,
                    tac = id.tac
                )
            } else null
            else -> null
        }
    }

    private fun plmnToCarrier(mcc: Int, mnc: Int): String {
        val plmn = String.format("%03d%02d", mcc, mnc)
        return when (plmn) {
            in listOf("310260", "310210", "310220", "310230", "310240", "310250", "310270", "310660", "310200", "310160", "310310") -> "T-Mobile"
            in listOf("310004", "310012", "311480", "311110") -> "Verizon"
            in listOf("310410", "310150", "310170", "310280", "310380", "310560", "311870") -> "AT&T"
            "313100" -> "FirstNet"
            "312670" -> "Dish Wireless"
            "310030", "311220" -> "US Cellular"
            else -> "Carrier $plmn"
        }
    }

    private fun earfcnToLteBand(earfcn: Int): String = when (earfcn) {
        in 0..599 -> "B1"
        in 600..1199 -> "B2"
        in 1200..1949 -> "B3"
        in 1950..2399 -> "B4"
        in 2400..2649 -> "B5"
        in 2650..3449 -> "B7"
        in 3800..4149 -> "B12"
        in 4150..4749 -> "B13"
        in 4750..4949 -> "B14"
        in 5730..5849 -> "B20"
        in 5850..5999 -> "B25"
        in 6000..6149 -> "B26"
        in 6150..6449 -> "B28"
        in 8690..9039 -> "B41"
        in 9210..9659 -> "B66"
        in 9660..9769 -> "B71"
        else -> "B??"
    }

    private fun nrarfcnToNrBand(nrarfcn: Int): String = when (nrarfcn) {
        in 123400..151600 -> "n71"
        in 499200..537999 -> "n71"
        in 514998..573332 -> "n41"
        in 620000..653333 -> "n77/n78"
        in 653334..693333 -> "n78"
        in 693334..733333 -> "n78"
        in 342000..356000 -> "n66"
        else -> "n??"
    }
}