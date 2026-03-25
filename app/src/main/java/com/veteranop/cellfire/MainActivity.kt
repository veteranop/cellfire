package com.veteranop.cellfire

import android.Manifest
import android.app.Application
import com.veteranop.cellfire.BuildConfig
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.veteranop.cellfire.ui.theme.CellFireTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PREFS_NAME = "CellFirePrefs"
const val CROWDSOURCE_ENABLED_KEY = "crowdsource_enabled"
const val LAST_SYNC_KEY = "last_db_sync"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid setup for caching
        val osmConfig = Configuration.getInstance()
        osmConfig.osmdroidBasePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")
        osmConfig.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Log.d("CellFire", "OSMDroid cache set up at ${osmConfig.osmdroidBasePath}")

        Configuration.getInstance().userAgentValue = "CellFire-VeteranOp/1.0"

        setContent {
            CellFireTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    val navController = rememberNavController()
                    val vm: CellFireViewModel = hiltViewModel()
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") { SplashScreen(navController, vm) }
                        composable("start") { StartScreen(navController, vm) }
                        composable("scan") { ScanScreen(navController, vm) }
                        composable("pci_discovery") { PciDiscoveryScreen(navController, viewModel = vm) }
                        composable(
                            "detail/{pci}/{arfcn}",
                            arguments = listOf(
                                navArgument("pci") { type = NavType.IntType },
                                navArgument("arfcn") { type = NavType.IntType },
                            )
                        ) { backStackEntry ->
                            val pci = backStackEntry.arguments?.getInt("pci") ?: 0
                            val arfcn = backStackEntry.arguments?.getInt("arfcn") ?: 0
                            CellDetailScreen(vm, pci, arfcn, navController)
                        }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") {
                            CellfireAnalytics.aboutScreenOpened()
                            AboutScreen()
                        }
                        composable("settings") {
                            CellfireAnalytics.settingsScreenOpened()
                            SettingsScreen(vm)
                        }
                        composable("pci_table") { PciTableScreen(navController, vm) }
                        composable("pci_carrier_list/{carrier}") { backStackEntry ->
                            val carrier = backStackEntry.arguments?.getString("carrier") ?: ""
                            PciCarrierListScreen(navController, vm, carrier)
                        }
                        composable(
                            route = "pci_map/{pci}",
                            arguments = listOf(navArgument("pci") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val pci = backStackEntry.arguments?.getInt("pci") ?: 0
                            PciMapScreen(navController, pci, vm)
                        }
                        composable("login")   { AccountScreen(navController, vm) }
                        composable("account") { AccountManagementScreen(navController, vm) }
                    }
                }
            }
        }
    }
}

// ─── Splash / auth gate ───────────────────────────────────────────────────────
@Composable
fun SplashScreen(navController: NavController, vm: CellFireViewModel) {
    val authState by vm.authState.collectAsState()

    // Once auth state resolves, route accordingly — never let user past this without signing in
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> Unit // wait
            is AuthState.LoggedIn -> navController.navigate("start") {
                popUpTo("splash") { inclusive = true }
            }
            is AuthState.LoggedOut, is AuthState.Error -> navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.cfbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.4f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.app_name), contentDescription = "Cellfire")
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = Color(0xFFFF8C00))
        }
    }
}

@Composable
fun StartScreen(navController: NavController, vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()
    val authState by vm.authState.collectAsState()

    // Derive account button label from auth state
    val accountLabel = when (val s = authState) {
        is AuthState.LoggedIn -> "Account  ·  ${s.license.planLabel}"
        else -> "Account / Sign In"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.cfbackground),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.4f)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.app_name), contentDescription = "app logo")
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate("scan") }, modifier = Modifier.fillMaxWidth()) { Text("Start Scan") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { vm.toggleRecording() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isRecording) "Stop Recording Session" else "Record Session")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("raw") }, modifier = Modifier.fillMaxWidth()) { Text("Raw Phone Output") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("pci_table") }, modifier = Modifier.fillMaxWidth()) { Text("Discovered PCIs") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("about") }, modifier = Modifier.fillMaxWidth()) { Text("About") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("account") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (authState is AuthState.LoggedIn) Color(0xFFFF8C00) else MaterialTheme.colorScheme.primary,
                    contentColor = if (authState is AuthState.LoggedIn) Color.Black else Color.White
                )
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(accountLabel, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController, vm: CellFireViewModel) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val deepScanActive by vm.deepScanActive.collectAsState()

    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val allGranted = results.values.all { it }
        vm.onPermissionsResult(allGranted)
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            launcher.launch(permissions.toTypedArray())
        } else {
            vm.onPermissionsResult(true)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                Image(painter = painterResource(id = R.drawable.app_name), contentDescription = "app logo", modifier = Modifier.padding(bottom = 16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ElevatedButton(
                        onClick = { vm.toggleMonitoring() },
                        modifier = Modifier.weight(1f),
                        colors = if (state.isMonitoring) ButtonDefaults.elevatedButtonColors(containerColor = Color.Black, contentColor = Color.Red) else ButtonDefaults.elevatedButtonColors()
                    ) {
                        Text(if (state.isMonitoring) "CEASE FIRE" else "ENGAGE", maxLines = 1)
                    }
                    ElevatedButton(
                        onClick = { vm.toggleDeepScan(!deepScanActive) },
                        modifier = Modifier.weight(1f).shadow(12.dp, RoundedCornerShape(32.dp)),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (deepScanActive) Color(0xFFFF6B00) else Color(0xFF2D2D2D),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Text(
                            text = "DEEP SCAN: ${if (deepScanActive) "ACTIVE" else "PASSIVE"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                    Text("Drive Test Mode:", color = Color.White)
                    Switch(
                        checked = state.isDriveTestMode,
                        onCheckedChange = { vm.setDriveTestMode(it) }
        )
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("CARRIER", Modifier.weight(1.6f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("BAND", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("PCI", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("BW", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("RSRP", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("SNR", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                }

                val displayCells = state.cells.distinctBy { it.pci to it.arfcn }

                val filteredCells = displayCells.filter {
                    val discoveredPci = state.discoveredPcis.find { pci -> pci.pci == it.pci && pci.band == it.band }
                    val isIgnored = discoveredPci?.isIgnored ?: false
                    !isIgnored
                }.sortedByDescending { it.isRegistered }

                SwipeRefresh(
                    state = rememberSwipeRefreshState(state.isRefreshing),
                    onRefresh = { vm.refresh() },
                ) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredCells, key = { "${it.pci}-${it.arfcn}-${it.lastSeen}" }) { cell ->
                            val discoveredPci = state.discoveredPcis.find { it.pci == cell.pci && it.band == cell.band }
                            val cellColor = when (cell.carrier.lowercase()) {
                                "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                                "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                                "at&t" -> Color(0xFF00A8E8)
                                "firstnet", "firstnet/at&t", "firstnet (at&t)" -> Color.Black
                                "dish wireless", "dish (boost)", "dish" -> Color(0xFFFF6200)
                                "us cellular", "uscellular" -> Color(0xFF6A0DAD)
                                else -> Color.DarkGray
                }
                            val finalColor = if (discoveredPci?.isTargeted == true) Color.Cyan else cellColor

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(finalColor.copy(alpha = 0.4f))
                                    .then(
                                        if (cell.isRegistered) Modifier.border(1.dp, Color(0xFFFFD700))
                                        else Modifier
                                    )
                                    .clickable { navController.navigate("detail/${cell.pci}/${cell.arfcn}") }
                                    .padding(12.dp)
        ) {
                                val rowMatchLevel = remember(cell.pci, cell.tac) {
                                    CellfireDbManager.lookupMatchLevel(cell.pci, cell.tac)
                                }
                                val dotColor = when (rowMatchLevel) {
                                    DbMatchLevel.EXACT     -> Color(0xFF1B5E20)  // dark green — verified by registered user (conf=100)
                                    DbMatchLevel.HIGH_CONF -> Color(0xFF4CAF50)  // green — high confidence
                                    DbMatchLevel.MED_CONF  -> Color(0xFFFFC107)  // yellow — medium confidence
                                    DbMatchLevel.LOW_CONF  -> Color(0xFFF44336)  // red — low confidence
                                    DbMatchLevel.NONE      -> Color(0xFF9E9E9E)  // grey — no DB record
                                }
                                Text(
                                    text = "● ",
                                    color = dotColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Text(text = cell.carrier.uppercase(), modifier = Modifier.weight(1.6f), fontWeight = if (cell.isRegistered) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (cell.source == "pci_range") Color(0xFFFFC107) else Color.White)
                                Text(text = cell.band, modifier = Modifier.weight(0.8f), color = Color.White)
                                Text(text = cell.pci.toString(), modifier = Modifier.weight(0.8f), color = Color.White)
                                Text(text = if (cell.bandwidth > 0) "${cell.bandwidth.toInt()}" else "-", modifier = Modifier.weight(0.7f), color = Color.White)
                                Text(
                                    text = cell.signalStrength.toString(),
                                    modifier = Modifier.weight(1f),
                                    color = if (cell.signalStrength > -85) Color.Green else if (cell.signalStrength > -100) Color.Yellow else Color.Red
                                )
                                Text(text = cell.signalQuality.toString(), modifier = Modifier.weight(1f), color = Color.White)
        }
    }
}
                }

                Text(text = "${BuildConfig.VERSION_NAME} • Cellfire", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellDetailScreen(vm: CellFireViewModel, pci: Int, arfcn: Int, navController: NavController) {
    val state by vm.uiState.collectAsState()
    val cell by remember(state.cells, pci, arfcn) { derivedStateOf { state.cells.firstOrNull { it.pci == pci && it.arfcn == arfcn } } }
    val history by remember(state.signalHistory, pci, arfcn) { derivedStateOf { state.signalHistory[Pair(pci, arfcn)] ?: emptyList() } }
    var showDialog by remember { mutableStateOf(false) }

    val currentCell = cell
    if (currentCell == null) {
        Box(
                        modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            contentAlignment = Alignment.Center
                    ) {
            Text("Cell not found. Waiting for data...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
        return
                    }

    var selectedCarrier by remember(currentCell) { mutableStateOf(currentCell.carrier) }
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha=0.6f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cell Details", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), color = Color.White)
                        Button(onClick = { navController.navigate("pci_map/${currentCell.pci}") }) {
                            Text("Map")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showDialog = true }) {
                            Text("Edit")
                        }
                    }
        Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Carrier: ${currentCell.carrier}", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        val matchLevel = remember(currentCell.pci, currentCell.tac) {
                            CellfireDbManager.lookupMatchLevel(currentCell.pci, currentCell.tac)
                        }
                        Text(
                            "●",
                            color = when (matchLevel) {
                                DbMatchLevel.EXACT      -> Color(0xFF1B5E20)
                                DbMatchLevel.HIGH_CONF  -> Color(0xFF4CAF50)
                                DbMatchLevel.MED_CONF   -> Color(0xFFFFC107)
                                DbMatchLevel.LOW_CONF   -> Color(0xFFF44336)
                                DbMatchLevel.NONE       -> Color(0xFF9E9E9E)
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    val dbConf = remember(currentCell.pci, currentCell.tac) {
                        (CellfireDbManager.lookupByPciTac(currentCell.pci, currentCell.tac)
                            ?: CellfireDbManager.lookupByPciOnly(currentCell.pci))?.conf
                    }
                    if (dbConf != null && dbConf > 0) {
                        val confColor = when {
                            dbConf == 100 -> Color(0xFF1B5E20)
                            dbConf >= 75  -> Color(0xFF4CAF50)
                            dbConf >= 40  -> Color(0xFFFFC107)
                            else          -> Color(0xFFF44336)
                        }
                        Text(
                            "Carrier accuracy: $dbConf / 100",
                            color = confColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("PCI: ${currentCell.pci}", color = Color.White)
                    Text("ARFCN: ${currentCell.arfcn}", color = Color.White)
                    Text("Band: ${currentCell.band}${if (currentCell.bandwidth > 0) " (${currentCell.bandwidth.toInt()} MHz)" else ""}", color = Color.White)
                    Text("Type: ${currentCell.type}", color = Color.White)
                    Text("RSRP: ${currentCell.signalStrength}", color = Color.White)
                    Text("SINR: ${currentCell.signalQuality}", color = Color.White)
                    Text("RSRQ: ${currentCell.rsrq}", color = Color.White)
                    Text("TAC: ${currentCell.tac}", color = Color.White)
                    Text("Registered: ${currentCell.isRegistered}", color = Color.White)
                    Text("Latitude: ${currentCell.latitude}", color = Color.White)
                    Text("Longitude: ${currentCell.longitude}", color = Color.White)
                    val freq = when (currentCell) {
                        is LteCell -> FrequencyCalculator.earfcnToFrequency(currentCell.arfcn)
                        is NrCell  -> FrequencyCalculator.nrarfcnToFrequency(currentCell.arfcn)
                        else -> null
                    }
                    if (freq != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DL Freq: ${String.format("%.1f", freq.first)} MHz", color = Color.White)
                        if (freq.second > 0 && freq.second != freq.first) {
                            Text("UL Freq: ${String.format("%.1f", freq.second)} MHz", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SignalChart(history = history)
        }
    }

    if (showDialog) {
        AlertDialog(
            containerColor = Color.DarkGray,
            onDismissRequest = { showDialog = false },
            title = { Text("Edit PCI ${pci}", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var expanded by remember { mutableStateOf(false) }
                    Text("Change Carrier:", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCarrier,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.LightGray, cursorColor = Color.White)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.Gray)
                        ) {
                            state.selectedCarriers.forEach { carrier ->
                                DropdownMenuItem(
                                    text = { Text(carrier, color = Color.White) },
                                    onClick = {
                                        selectedCarrier = carrier
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Other Actions:", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            vm.updatePci(pci, currentCell.band, isIgnored = true)
                            showDialog = false
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF600000))) { Text("Ignore") }

                        val isTargeted = state.discoveredPcis.find { it.pci == pci && it.band == currentCell.band }?.isTargeted == true
                        Button(onClick = {
                            vm.updatePci(pci, currentCell.band, isTargeted = !isTargeted)
                            showDialog = false
                        }, modifier = Modifier.weight(1f)) { Text(if (isTargeted) "Untarget" else "Target") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.updateCarrier(pci, currentCell.band, selectedCarrier)
                        showDialog = false
                    }
                ) {
                    Text("Save Carrier")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SignalChart(history: List<SignalHistoryPoint>) {
    Card(modifier = Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha=0.6f))) {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.textColor = AndroidColor.WHITE
                    xAxis.setDrawGridLines(false)
                    xAxis.setDrawAxisLine(true)
                    axisLeft.textColor = AndroidColor.WHITE
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = AndroidColor.GRAY
                    axisRight.isEnabled = false
                    legend.textColor = AndroidColor.WHITE
                    setNoDataText("Waiting for signal history...")
                    setNoDataTextColor(AndroidColor.WHITE)
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                }
            },
            update = { chart ->
                if (history.isEmpty()) {
                    chart.clear()
                    chart.invalidate()
                    return@AndroidView
                }

                val rsrpEntries = history.mapIndexed { index, point -> Entry(index.toFloat(), point.rsrp.toFloat()) }
                val sinrEntries = history.mapIndexed { index, point -> Entry(index.toFloat(), point.sinr.toFloat()) }
                val rsrqEntries = history.mapIndexed { index, point -> Entry(index.toFloat(), point.rsrq.toFloat()) }

                val rsrpDataSet = LineDataSet(rsrpEntries, "RSRP (dBm)").apply {
                    color = AndroidColor.CYAN
                    valueTextColor = AndroidColor.TRANSPARENT
                    setDrawCircles(false)
                    lineWidth = 2f
                    isHighlightEnabled = false
                }

                val sinrDataSet = LineDataSet(sinrEntries, "SINR (dB)").apply {
                    color = AndroidColor.MAGENTA
                    valueTextColor = AndroidColor.TRANSPARENT
                    setDrawCircles(false)
                    lineWidth = 2f
                    isHighlightEnabled = false
                }

                val rsrqDataSet = LineDataSet(rsrqEntries, "RSRQ (dB)").apply {
                    color = AndroidColor.YELLOW
                    valueTextColor = AndroidColor.TRANSPARENT
                    setDrawCircles(false)
                    lineWidth = 2f
                    isHighlightEnabled = false
                }

                chart.data = LineData(rsrpDataSet, sinrDataSet, rsrqDataSet)
                chart.xAxis.labelCount = 5
                chart.notifyDataSetChanged()
                chart.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawLogScreen(vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Raw Logs", color = Color.White) },
                actions = {
                    Button(onClick = { vm.clearLog() }) {
                        Text("Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp), reverseLayout = true) {
                items(state.logLines) { log ->
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    Text(text = "[$timestamp] ${log.message}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PciTableScreen(navController: NavController, vm: CellFireViewModel) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val carriers = state.discoveredPcis.map { it.carrier }.distinct()

    LaunchedEffect(Unit) {
        vm.exportEvent.collect { file ->
            val uri = FileProvider.getUriForFile(context, "com.veteranop.cellfire.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV"))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Discovered PCIs by Carrier", color = Color.White) },
                actions = {
                    Button(onClick = { vm.exportAllWithHistory() }) {
                        Text("Export All")
                    }
                    Button(onClick = { vm.clearPciHistory() }) {
                        Text("Clear All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(carriers) { carrier ->
                    val pciCount = state.discoveredPcis.count { it.carrier == carrier }
                    val cellColor = when (carrier.lowercase()) {
                        "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                        "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                        "at&t" -> Color(0xFF00A8E8)
                        "firstnet" -> Color.Black
                        "dish wireless" -> Color(0xFFFF6200)
                        else -> Color.DarkGray
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cellColor.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { navController.navigate("pci_carrier_list/$carrier") }
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(carrier, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
                            Text("$pciCount PCIs", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PciCarrierListScreen(navController: NavController, vm: CellFireViewModel, carrier: String) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val pcis = state.discoveredPcis.filter { it.carrier == carrier }

    LaunchedEffect(Unit) {
        vm.exportEvent.collect { file ->
            val uri = FileProvider.getUriForFile(context, "com.veteranop.cellfire.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV"))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val cellColor = when (carrier.lowercase()) {
                        "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                        "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                        "at&t" -> Color(0xFF00A8E8)
                        "firstnet" -> Color.Black
                        "dish wireless" -> Color(0xFFFF6200)
                        else -> Color.DarkGray
                    }
                    Text(carrier, style = MaterialTheme.typography.headlineLarge, color = cellColor)
                },
                actions = {
                    Button(onClick = { vm.exportCarrierWithHistory(carrier) }) {
                        Text("Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("PCI", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        Text("Band", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        Text("Count", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        Text("Last Seen", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2f))
                        Text("Flags", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    }
                    Divider(color = Color.Gray)
                }
                items(pcis.sortedByDescending { it.lastSeen }) { pci ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { navController.navigate("pci_map/${pci.pci}") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pci.pci.toString(), color = Color.White, modifier = Modifier.weight(1f))
                            Text(pci.band, color = Color.White, modifier = Modifier.weight(1f))
                            Text(pci.discoveryCount.toString(), color = Color.White, modifier = Modifier.weight(1f))
                            Text(SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault()).format(Date(pci.lastSeen)), color = Color.White, modifier = Modifier.weight(2f))
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (pci.isIgnored) Icon(Icons.Default.AccountBox, contentDescription = "Ignored", tint = Color.Red, modifier = Modifier.size(16.dp))
                                if (pci.isTargeted) Icon(Icons.Default.Star, contentDescription = "Targeted", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SettingsScreen(vm: CellFireViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var crowdsourceEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean(CROWDSOURCE_ENABLED_KEY, true))
    }
    var lastSynced by remember { mutableStateOf(sharedPreferences.getLong(LAST_SYNC_KEY, 0L)) }
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Upload cell data", color = Color.White)
            Switch(
                checked = crowdsourceEnabled,
                onCheckedChange = { enabled ->
                    crowdsourceEnabled = enabled
                    sharedPreferences.edit().putBoolean(CROWDSOURCE_ENABLED_KEY, enabled).apply()
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Cell database", color = Color.White)
                Text(
                    if (lastSynced > 0L)
                        "Last synced: ${SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(lastSynced))}"
                    else
                        "Never synced",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    CellfireAnalytics.dbSyncRequested()
                    scope.launch {
                        isSyncing = true
                        CellfireDbManager.clearCache()
                        try {
                            val loc = suspendCancellableCoroutine { cont ->
                                fusedLocation.lastLocation
                                    .addOnSuccessListener { cont.resume(it) }
                                    .addOnFailureListener { cont.resume(null) }
                            }
                            if (loc != null) {
                                CellfireDbManager.refreshTiles(loc.latitude, loc.longitude)
                                val now = System.currentTimeMillis()
                                sharedPreferences.edit().putLong(LAST_SYNC_KEY, now).apply()
                                lastSynced = now
                            }
                        } finally {
                            isSyncing = false
                        }
                    }
                },
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "Syncing..." else "Sync DB")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Discovered PCIs
        var isUploading by remember { mutableStateOf(false) }
        var uploadResult by remember { mutableStateOf("") }
        val discoveredPcis = vm.uiState.collectAsState().value.discoveredPcis
        val validCount = discoveredPcis.count { it.tac > 0 }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Upload discovered PCIs", color = Color.White)
                Text(
                    "$validCount with valid TAC ready to upload" +
                        if (uploadResult.isNotEmpty()) "\n$uploadResult" else "",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    CellfireAnalytics.pciUploadRequested(validCount)
                    isUploading = true
                    uploadResult = ""
                    vm.uploadDiscovered { uploaded, skipped ->
                        isUploading = false
                        uploadResult = "Sent $uploaded" + if (skipped > 0) ", $skipped skipped (no GPS)" else ""
                        CellfireAnalytics.pciUploadCompleted(uploaded, skipped)
                    }
                },
                enabled = !isUploading && validCount > 0
            ) {
                Text(if (isUploading) "Uploading..." else "Upload")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Signal Rules
        var rulesVersion by remember { mutableStateOf(CarrierResolver.currentVersion()) }
        var rulesUpdating by remember { mutableStateOf(false) }
        var rulesStatus by remember { mutableStateOf("") }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Signal Rules", color = Color.White)
                Text(
                    rulesVersion + if (rulesStatus.isNotEmpty()) " · $rulesStatus" else "",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (rulesUpdating) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Button(onClick = {
                    CellfireAnalytics.signalRulesUpdateRequested()
                    rulesUpdating = true
                    rulesStatus = ""
                    scope.launch {
                        val newVersion = CarrierResolver.fetchUpdate()
                        rulesUpdating = false
                        if (newVersion != null) {
                            rulesVersion = newVersion
                            rulesStatus = "Updated"
                            CellfireAnalytics.signalRulesUpdated(newVersion, true)
                        } else {
                            rulesStatus = "Failed"
                            CellfireAnalytics.signalRulesUpdated(rulesVersion, false)
                        }
                    }
                }) {
                    Text("Update")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Update
        var updateStatus by remember { mutableStateOf("") }
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var isDownloadingUpdate by remember { mutableStateOf(false) }
        var pendingUpdate by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("App Update", color = Color.White)
                Text(
                    "Current: ${BuildConfig.VERSION_NAME}" +
                        if (updateStatus.isNotEmpty()) " · $updateStatus" else "",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isCheckingUpdate || isDownloadingUpdate) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else if (pendingUpdate != null) {
                Button(onClick = {
                    isDownloadingUpdate = true
                    updateStatus = "Downloading..."
                    scope.launch {
                        val ok = AppUpdater.downloadAndInstall(context, pendingUpdate!!.downloadUrl)
                        isDownloadingUpdate = false
                        updateStatus = if (ok) "Installing..." else "Download failed"
                    }
                }) {
                    Text("Install ${pendingUpdate!!.tagName}")
                }
            } else {
                Button(onClick = {
                    isCheckingUpdate = true
                    updateStatus = ""
                    pendingUpdate = null
                    scope.launch {
                        val info = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                        isCheckingUpdate = false
                        when {
                            info == null        -> updateStatus = "Check failed"
                            info.isNewer        -> { pendingUpdate = info; updateStatus = "${info.tagName} available" }
                            else                -> updateStatus = "Up to date"
                        }
                    }
                }) {
                    Text("Check")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Clear All Data
        var showClearConfirm by remember { mutableStateOf(false) }
        var isClearing by remember { mutableStateOf(false) }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear All Data?") },
                text = { Text("This will wipe all discovered PCIs, drive test points, tile cache, and settings. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirm = false
                            isClearing = true
                            sharedPreferences.edit().clear().apply()
                            crowdsourceEnabled = true
                            lastSynced = 0L
                            CellfireAnalytics.allDataCleared()
                            vm.clearAllData { isClearing = false }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Clear Everything")
                    }
                },
                dismissButton = {
                    Button(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clear All Data", color = Color.Red)
                Text(
                    "Wipes tile cache, discovered PCIs, drive test points, and settings",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = { showClearConfirm = true },
                enabled = !isClearing,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(if (isClearing) "Clearing..." else "Clear")
            }
        }
    }
}

@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("CELLFIRE", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                "Cellfire is a real-time LTE and 5G NR cell tower scanner. It uses your " +
                "device's radio hardware to identify nearby towers, resolve their carrier, " +
                "band, and signal metrics, and crowdsource that data to the Cellfire " +
                "community database to improve coverage for everyone.",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
        }

        item { Divider(color = Color.DarkGray) }

        // ── Dot color index ──────────────────────────────────────────────────
        item {
            Text("CONFIDENCE INDICATOR", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD700))
            Spacer(Modifier.height(8.dp))
            val dots = listOf(
                Triple(Color(0xFF2196F3), "Blue",   "Your registered (serving) cell — carrier confirmed directly by the modem."),
                Triple(Color(0xFF4CAF50), "Green",  "High confidence (75–99) — FCC band match or strong crowd-confirmed record."),
                Triple(Color(0xFFFFC107), "Yellow", "Medium confidence (40–74) — database or PCI inference. Likely correct."),
                Triple(Color(0xFFF44336), "Red",    "Low confidence (<40) — PCI-range guess only. Treat as approximate."),
                Triple(Color(0xFF9E9E9E), "Grey",   "Not in database — no record found for this tower in any loaded tile."),
            )
            dots.forEach { (color, label, desc) ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("● ", color = color, style = MaterialTheme.typography.bodyMedium)
                    Column {
                        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(desc, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Divider(color = Color.DarkGray) }

        // ── Measurement definitions ──────────────────────────────────────────
        item {
            Text("MEASUREMENTS", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD700))
            Spacer(Modifier.height(8.dp))
            val defs = listOf(
                "RSRP" to "Reference Signal Received Power (dBm). Raw signal strength from the tower. " +
                    "≥ −85 good  |  −85 to −100 fair  |  < −100 weak. Lower numbers mean weaker signal.",
                "RSRQ" to "Reference Signal Received Quality (dB). Signal quality relative to interference " +
                    "and noise. ≥ −10 good  |  −10 to −15 fair  |  < −15 poor.",
                "SNR / SINR" to "Signal-to-Interference-plus-Noise Ratio (dB). Indicator of data throughput " +
                    "potential. Higher is better. > 20 = excellent  |  0–20 = normal  |  < 0 = congested.",
                "Band" to "LTE or NR frequency band (e.g. B66 = AWS-3 ≈ 1700/2100 MHz, B12 = 700 MHz low-band, " +
                    "n71 = 600 MHz NR low-band). Low bands travel farther; high bands carry more data.",
                "EARFCN / ARFCN" to "Absolute Radio Frequency Channel Number. Encodes the exact downlink " +
                    "frequency channel within a band. Unique per carrier per market — the primary RF fingerprint " +
                    "used to distinguish T-Mobile from AT&T on shared bands like B66.",
                "UL Freq" to "Uplink frequency (MHz) — your phone transmitting to the tower.",
                "DL Freq" to "Downlink frequency (MHz) — tower transmitting to your phone.",
                "BW" to "Channel bandwidth in MHz (5, 10, 15, 20 MHz for LTE; up to 100 MHz for NR). " +
                    "Wider bandwidth = higher peak speeds.",
                "PCI" to "Physical Cell Identity (0–503). A local broadcast identifier used by the radio " +
                    "to distinguish towers. Not globally unique — different carriers can reuse the same PCI " +
                    "number on different frequencies.",
                "TAC" to "Tracking Area Code. A carrier-assigned network identifier for a geographic market. " +
                    "Reliable carrier fingerprint — if you know the TAC, you know the carrier.",
            )
            defs.forEach { (term, explanation) ->
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(term, color = Color.White, style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(explanation, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Divider(color = Color.DarkGray) }

        // ── Settings explanations ────────────────────────────────────────────
        item {
            Text("SETTINGS EXPLAINED", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD700))
            Spacer(Modifier.height(8.dp))
            val settings = listOf(
                "Upload cell data" to
                    "Automatically submits tower observations to the Cellfire community database " +
                    "when your phone is registered on a confirmed cell. Only sends when carrier, " +
                    "location, and TAC are all verified. Toggle off for privacy or to save data.",
                "Sync DB" to
                    "Downloads cell tower data tiles from cellfire.io for your current area " +
                    "(≈80-mile radius). Tiles are cached on-device and refreshed every 5 minutes " +
                    "while you're moving. Use this button to force an immediate refresh.",
                "Upload discovered PCIs" to
                    "Sends the list of unique PCIs your phone has seen — along with their " +
                    "confirmed TACs — to the community database. This helps seed new areas with " +
                    "carrier data even when crowd coverage is sparse.",
                "Signal Rules" to
                    "Downloads the latest carrier band-licensing rules from cellfire.io. These rules " +
                    "determine which carriers are valid on each frequency band (e.g. only T-Mobile " +
                    "can appear on B71, only Verizon on B13). The version tag (e.g. m26-1 = " +
                    "March 2026, revision 1) tells you what rule set is currently loaded. " +
                    "Tap Update to pull the latest version.",
                "Clear All Data" to
                    "Wipes all locally stored data: tile cache, discovered PCI list, drive test " +
                    "points, and all app settings. Use this to start fresh or if the app is " +
                    "showing stale data. This cannot be undone.",
                "App Update" to
                    "Checks GitHub for a newer release of Cellfire. Tap Check — if an update " +
                    "is available the button changes to Install with the version number. Tapping " +
                    "Install downloads the APK and hands it to the Android installer. No Play " +
                    "Store required.",
            )
            settings.forEach { (title, desc) ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(desc, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Divider(color = Color.DarkGray) }

        // ── Discovered PCIs ──────────────────────────────────────────────────
        item {
            Text("DISCOVERED PCIs", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD700))
            Spacer(Modifier.height(8.dp))
            Text(
                "The Discovered PCIs table is a log of every unique cell tower your phone has " +
                "detected — including neighbor cells that aren't your active connection. Each " +
                "entry shows the PCI, band, resolved carrier, TAC, and the source of the " +
                "carrier identification (e.g. alpha = modem confirmed, db = community database, " +
                "exclusive_band = spectrum law).",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tapping a PCI opens the detail view with signal history and a map pin. The " +
                "Upload button in Settings sends your confirmed discoveries to the Cellfire " +
                "community database, helping improve carrier identification for other users " +
                "in your area.",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
        }

        item { Divider(color = Color.DarkGray) }

        // ── How mapping works ────────────────────────────────────────────────
        item {
            Text("HOW MAPPING WORKS", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD700))
            Spacer(Modifier.height(8.dp))
            Text(
                "Cellfire divides the map into 0.5° grid tiles (roughly 30–35 miles square). " +
                "When you start the app, it downloads tiles for an ~80-mile radius around you " +
                "from cellfire.io. Each tile is a compressed list of known towers with their " +
                "PCI, TAC, carrier, GPS location, and a confidence score.",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "When a cell is detected, the app checks the tile database for a matching " +
                "PCI + TAC record. A match gives an instant carrier label without any network " +
                "request. Tiles are seeded from OpenCellID data and continuously improved by " +
                "Cellfire users who have Upload Cell Data enabled.",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Carrier accuracy score (1–100) reflects how many independent confirmations " +
                "a record has. Score 100 means a registered device confirmed the carrier " +
                "directly from the modem. Score 40–74 means the record came from the community " +
                "database or band inference and is likely correct. Score < 40 is a rough guess " +
                "based on PCI range only.",
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray
            )
        }

        item { Divider(color = Color.DarkGray) }

        item {
            Text("cellfire.io", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
