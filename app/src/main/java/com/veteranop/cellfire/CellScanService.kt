package com.veteranop.cellfire

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import android.telephony.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class CellScanService : LifecycleService() {

    @Inject lateinit var cellRepository: CellRepository

    private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private var regularJob: Job? = null
    private var deepScanJob: Job? = null
    private var currentScan: NetworkScan? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_DEEP = "ACTION_TOGGLE_DEEP"
        const val ACTION_REFRESH = "ACTION_REFRESH"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "cellfire_scan"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        // Android 14+ requires explicit type in startForeground if declared in manifest
        val notification = createNotification("CellFire Active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startScanning()
            ACTION_STOP -> stopScanning()
            ACTION_TOGGLE_DEEP -> {
                val enable = intent.getBooleanExtra("enable", true)
                if (enable) startDeepScan() else stopDeepScan()
            }
            ACTION_REFRESH -> refresh()
        }
        return START_STICKY
    }

    private fun startScanning() {
        if (regularJob?.isActive == true) return
        cellRepository.setServiceActive(true)

        regularJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateFromAllCellInfo()
                delay(5000L)
            }
        }

        startDeepScan()
    }

    private fun startDeepScan() {
        if (deepScanJob?.isActive == true) return
        deepScanJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateNotification("Deep Scan: Searching...")
                performDeepScan()
                delay(35000L) // 30s scan + 5s delay
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                lastLatitude = it.latitude
                lastLongitude = it.longitude
            }
        }
    }

    private fun stopDeepScan() {
        deepScanJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                currentScan?.stopScan()
            } catch (e: Exception) {
                // Ignore if already stopped
            }
        }
        currentScan = null
        updateNotification("CellFire Active")
    }

    private fun stopScanning() {
        regularJob?.cancel()
        stopDeepScan()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        cellRepository.setServiceActive(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun refresh() {
        lifecycleScope.launch(Dispatchers.IO) {
            cellRepository.setRefreshing(true)
            updateFromAllCellInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun performDeepScan() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            updateFromAllCellInfo()
            return
        }
        
        try {
            currentScan?.stopScan()
        } catch (e: Exception) {}

        val specifiers = mutableListOf(
            RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.GERAN, null, null),
            RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.UTRAN, null, null),
            RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.EUTRAN, null, null)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specifiers.add(RadioAccessSpecifier(AccessNetworkConstants.AccessNetworkType.NGRAN, null, null))
        }

        val request = NetworkScanRequest(
            NetworkScanRequest.SCAN_TYPE_ONE_SHOT,
            specifiers.toTypedArray(),
            5,
            30,
            true,
            5,
            null
        )

        try {
            val executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mainExecutor else {
                // Fallback for older versions if needed, but performDeepScan checks P
                return
            }
            
            currentScan = telephonyManager.requestNetworkScan(request, executor, object : TelephonyScanManager.NetworkScanCallback() {
                override fun onResults(results: MutableList<CellInfo>) {
                    results.takeIf { it.isNotEmpty() }?.let { list ->
                        list.forEach { cellRepository.addLogLine(it.toString()) }
                        val cells = list.mapNotNull { parseCellInfo(it) }
                        cellRepository.updateCells(cells)
                        updateNotification("Deep Scan: Found ${cells.size} new towers")
                    }
                }

                override fun onComplete() {
                    currentScan = null
                    updateNotification("Deep Scan: Cycle complete")
                }

                override fun onError(error: Int) {
                    currentScan = null
                    updateFromAllCellInfo()
                    updateNotification("Deep Scan: Error $error")
                }
            })
        } catch (e: Exception) {
            currentScan = null
            updateFromAllCellInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateFromAllCellInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                telephonyManager.requestCellInfoUpdate(mainExecutor, object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        cellInfo.forEach { cellRepository.addLogLine(it.toString()) }
                        val cells = cellInfo.mapNotNull { parseCellInfo(it) }
                        cellRepository.updateCells(cells)
                    }

                    override fun onError(errorCode: Int, detail: Throwable?) {
                        getLegacyCellInfo()
                    }
                })
            } catch (e: SecurityException) {
                getLegacyCellInfo()
            } catch (e: Exception) {
                getLegacyCellInfo()
            }
        } else {
            getLegacyCellInfo()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun getLegacyCellInfo() {
        try {
            val list = telephonyManager.allCellInfo ?: return
            list.forEach { cellRepository.addLogLine(it.toString()) }
            val cells = list.mapNotNull { parseCellInfo(it) }
            cellRepository.updateCells(cells)
        } catch (e: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "CellFire Scanning", NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CellFire Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }

    @SuppressLint("MissingPermission")
    private fun parseCellInfo(info: CellInfo): Cell? {
        // ... (Parsing logic remains the same, assuming it was working correctly)
        // I will keep the original parsing logic but wrap it in a try-catch to avoid crashes from malformed CellInfo on new hardware
        return try {
            doParse(info)
        } catch (e: Exception) {
            null
        }
    }

    private fun doParse(info: CellInfo): Cell? {
        return when (info) {
            is CellInfoLte -> {
                val identity = info.cellIdentity
                val strength = info.cellSignalStrength

                var carrier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val longName = identity.operatorAlphaLong?.toString()
                    val shortName = identity.operatorAlphaShort?.toString()
                    if (!longName.isNullOrBlank()) longName else if (!shortName.isNullOrBlank()) shortName else "Unknown"
                } else "Unknown"

                if (carrier == "Unknown" || carrier == "null") {
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
                    carrier = if (mcc != null && mnc != null && mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) {
                        plmnToCarrier(mcc, mnc)
                    } else "Unknown"
                }

                val pci = if (identity.pci == Int.MAX_VALUE) 0 else identity.pci
                if (carrier == "Unknown" || carrier == "null") {
                    carrier = pciToCarrier(pci)
                }

                var snr = if (strength.rssnr != Int.MAX_VALUE) strength.rssnr else strength.rsrq
                if (snr == Int.MAX_VALUE) snr = 0

                val bandwidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && identity.bandwidth != Int.MAX_VALUE) {
                    identity.bandwidth / 1000.0
                } else 0.0

                LteCell(
                    pci = pci,
                    arfcn = identity.earfcn,
                    band = earfcnToLteBand(identity.earfcn),
                    bandwidth = bandwidth,
                    signalStrength = strength.rsrp,
                    signalQuality = snr,
                    rsrq = strength.rsrq,
                    isRegistered = info.isRegistered,
                    carrier = carrier,
                    tac = if (identity.tac == Int.MAX_VALUE) 0 else identity.tac,
                    latitude = lastLatitude,
                    longitude = lastLongitude
                )
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                    val identity = info.cellIdentity as CellIdentityNr
                    val strength = info.cellSignalStrength as CellSignalStrengthNr

                    var carrier = identity.operatorAlphaLong?.toString() ?: identity.operatorAlphaShort?.toString() ?: "Unknown"

                    if (carrier == "Unknown" || carrier == "null" || carrier.isBlank()) {
                        val mcc = identity.mccString?.toIntOrNull()
                        val mnc = identity.mncString?.toIntOrNull()
                        carrier = if (mcc != null && mnc != null && mcc != Int.MAX_VALUE && mnc != Int.MAX_VALUE) {
                            plmnToCarrier(mcc, mnc)
                        } else "Unknown"
                    }

                    val pci = if (identity.pci == Int.MAX_VALUE) 0 else identity.pci
                    if (carrier == "Unknown" || carrier == "null" || carrier.isBlank()) {
                        carrier = pciToCarrier(pci)
                    }

                    val rsrp = if (strength.csiRsrp != Int.MAX_VALUE) strength.csiRsrp else strength.ssRsrp
                    var snr = if (strength.csiSinr != Int.MAX_VALUE) strength.csiSinr else strength.ssSinr
                    var rsrq = if (strength.csiRsrq != Int.MAX_VALUE) strength.csiRsrq else strength.ssRsrq

                    if (snr == Int.MAX_VALUE) snr = rsrq
                    if (rsrq == Int.MAX_VALUE) rsrq = 0
                    if (snr == Int.MAX_VALUE) snr = 0

                    NrCell(
                        pci = pci,
                        arfcn = identity.nrarfcn,
                        band = nrarfcnToNrBand(identity.nrarfcn),
                        bandwidth = 0.0, 
                        signalStrength = rsrp,
                        signalQuality = snr,
                        rsrq = rsrq,
                        isRegistered = info.isRegistered,
                        carrier = carrier,
                        tac = if (identity.tac == Int.MAX_VALUE) 0 else identity.tac,
                        latitude = lastLatitude,
                        longitude = lastLongitude
                    )
                } else if (info is CellInfoWcdma) {
                    val identity = info.cellIdentity
                    val strength = info.cellSignalStrength
                    var carrier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        identity.operatorAlphaLong?.toString() ?: identity.operatorAlphaShort?.toString() ?: "Unknown"
                    } else "Unknown"
                    WcdmaCell(
                        pci = identity.psc,
                        arfcn = identity.uarfcn,
                        band = "B?",
                        bandwidth = 0.0,
                        signalStrength = strength.dbm,
                        signalQuality = 0,
                        isRegistered = info.isRegistered,
                        carrier = carrier,
                        tac = if (identity.lac == Int.MAX_VALUE) 0 else identity.lac,
                        latitude = lastLatitude,
                        longitude = lastLongitude
                    )
                } else if (info is CellInfoGsm) {
                    val identity = info.cellIdentity
                    val strength = info.cellSignalStrength
                    var carrier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        identity.operatorAlphaLong?.toString() ?: identity.operatorAlphaShort?.toString() ?: "Unknown"
                    } else "Unknown"
                    GsmCell(
                        pci = identity.bsic,
                        arfcn = identity.arfcn,
                        band = "B?",
                        bandwidth = 0.0,
                        signalStrength = strength.dbm,
                        signalQuality = 0,
                        isRegistered = info.isRegistered,
                        carrier = carrier,
                        tac = if (identity.lac == Int.MAX_VALUE) 0 else identity.lac,
                        latitude = lastLatitude,
                        longitude = lastLongitude
                    )
                } else null
            }
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
