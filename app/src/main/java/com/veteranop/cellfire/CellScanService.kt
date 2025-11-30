package com.veteranop.cellfire

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.telephony.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class CellScanService : LifecycleService() {

    @Inject lateinit var cellRepository: CellRepository

    private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private var scanJob: Job? = null
    private var isDeepScanActive = true // Deep scan is on by default

    private var phoneStateListener: PhoneStateListener? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_DEEP = "ACTION_TOGGLE_DEEP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "cellfire_scan"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("CellFire Active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startScanning()
            ACTION_STOP -> stopScanning()
            ACTION_TOGGLE_DEEP -> {
                isDeepScanActive = intent.getBooleanExtra("enable", true)
                // Restart scanning to apply the new mode
                if (scanJob?.isActive == true) {
                    startScanning()
                }
            }
        }
        return START_STICKY
    }

    private fun startScanning() {
        scanJob?.cancel()
        cellRepository.setServiceActive(true)

        scanJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                val startTime = SystemClock.elapsedRealtime()

                if (isDeepScanActive) {
                    withContext(Dispatchers.Main) { registerPhoneStateListener() }
                    scanOnce()
                } else {
                    withContext(Dispatchers.Main) { unregisterPhoneStateListener() }
                    scanOnce()
                }

                val passiveDelay = 4_000L
                val deepDelay = if(isDeepScanActive) 100L else passiveDelay
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val sleepTime = (deepDelay - elapsed).coerceAtLeast(50L).coerceAtMost(500L)
                delay(sleepTime)
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        runBlocking { withContext(Dispatchers.Main) { unregisterPhoneStateListener() } }
        cellRepository.setServiceActive(false)
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun scanOnce() {
        val list = telephonyManager.allCellInfo ?: return
        cellRepository.addLogLine("Scan found ${list.size} cells")
        val cells = list.mapNotNull { parseCellInfo(it) }
        cellRepository.updateCells(cells)
    }

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) return
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                lifecycleScope.launch(Dispatchers.IO) { scanOnce() }
            }

            override fun onServiceStateChanged(serviceState: ServiceState?) {
                super.onServiceStateChanged(serviceState)
                lifecycleScope.launch(Dispatchers.IO) { scanOnce() }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE)
    }

    private fun unregisterPhoneStateListener() {
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            phoneStateListener = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "CellFire Scanning", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CellFire Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

    @SuppressLint("MissingPermission")
    private fun parseCellInfo(info: CellInfo): Cell? {
        return when (info) {
            is CellInfoLte -> {
                val identity = info.cellIdentity
                val strength = info.cellSignalStrength

                val mcc: Int?
                val mnc: Int?

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mcc = identity.mccString?.toIntOrNull()
                    mnc = identity.mncString?.toIntOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    mcc = identity.mcc
                    @Suppress("DEPRECATION")
                    mnc = identity.mnc
                }

                var carrier = if (mcc != null && mnc != null && mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) {
                    plmnToCarrier(mcc, mnc)
                } else {
                    "Unknown"
                }

                val pci = if (identity.pci == Int.MAX_VALUE) 0 else identity.pci
                if (carrier == "Unknown") {
                    carrier = pciToCarrier(pci)
                }

                LteCell(
                    pci = pci,
                    arfcn = identity.earfcn,
                    band = earfcnToLteBand(identity.earfcn),
                    signalStrength = strength.rsrp,
                    signalQuality = strength.rsrq,
                    isRegistered = info.isRegistered,
                    carrier = carrier,
                    tac = if (identity.tac == Int.MAX_VALUE) 0 else identity.tac
                )
            }
            is CellInfoNr -> {
                val identity = info.cellIdentity as? CellIdentityNr ?: return null
                val strength = info.cellSignalStrength as? CellSignalStrengthNr ?: return null

                val mcc = identity.mccString?.toIntOrNull()
                val mnc = identity.mncString?.toIntOrNull()

                var carrier = if (mcc != null && mnc != null && mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) {
                    plmnToCarrier(mcc, mnc)
                } else {
                    "Unknown"
                }

                val pci = if (identity.pci == Int.MAX_VALUE) 0 else identity.pci
                if (carrier == "Unknown") {
                    carrier = pciToCarrier(pci)
                }

                NrCell(
                    pci = pci,
                    arfcn = identity.nrarfcn,
                    band = nrarfcnToNrBand(identity.nrarfcn),
                    signalStrength = strength.csiRsrp,
                    signalQuality = strength.csiRsrq,
                    isRegistered = info.isRegistered,
                    carrier = carrier,
                    tac = if (identity.tac == Int.MAX_VALUE) 0 else identity.tac
                )
            }
            else -> null
        }
    }

    private fun pciToCarrier(pci: Int): String {
        return when (pci) {
            in 0..107   -> "T-Mobile"
            in 108..179 -> "T-Mobile (low-band)"
            in 180..251 -> "Verizon"
            in 252..287 -> "Verizon (B5)"
            in 288..359 -> "AT&T"
            in 360..395 -> "FirstNet"
            in 396..431 -> "Dish Wireless"
            in 432..467 -> "US Cellular"
            else        -> "Unknown / Other"
        }
    }

    private fun plmnToCarrier(mcc: Int, mnc: Int): String {
        return when (mcc) {
            310 -> when (mnc) {
                200, 210, 220, 230, 240, 250, 260 -> "T-Mobile"
                410 -> "AT&T"
                else -> "T-Mobile/AT&T Roaming"
            }
            311 -> when (mnc) {
                480, 481, 482, 483, 484, 485, 486, 487, 488, 489 -> "Verizon"
                else -> "Verizon Roaming"
            }
            313 -> when (mnc) {
                340 -> "Dish Wireless"
                100 -> "FirstNet (AT&T)"
                else -> "AT&T Roaming"
            }
            else -> "Unknown"
        }
    }

    private fun earfcnToLteBand(earfcn: Int): String {
        return when (earfcn) {
            in 0..599 -> "B1"
            in 600..1199 -> "B2"
            in 1200..1949 -> "B3"
            in 1950..2399 -> "B4"
            in 2400..2649 -> "B5"
            in 4800..5009 -> "B10"
            in 5010..5179 -> "B12"
            in 5180..5279 -> "B13"
            in 5280..5379 -> "B14"
            in 5730..5849 -> "B17"
            in 6150..6449 -> "B20"
            in 8040..8689 -> "B25"
            in 8690..9039 -> "B26"
            in 9210..9659 -> "B28"
            in 9920..10359 -> "B30"
            in 37750..38249 -> "B38"
            in 38650..39649 -> "B40"
            in 39650..41589 -> "B41"
            in 65536..66435 -> "B65"
            in 66436..67335 -> "B66"
            in 68586..68935 -> "B71"
            else -> "B??"
        }
    }

    private fun nrarfcnToNrBand(nrarfcn: Int): String {
        return when (nrarfcn) {
            in 620000..636666 -> "n77"
            in 636667..646666 -> "n78"
            in 509202..537999 -> "n41"
            in 122400..131400 -> "n71"
            in 2289252..2308333 -> "n260"
            in 2269584..2289251 -> "n261"
            in 418000..434000 -> "n25"
            in 384000..404000 -> "n66"
            in 370000..384000 -> "n2"
            in 173800..178000 -> "n5"
            else -> "n??"
        }
    }
}