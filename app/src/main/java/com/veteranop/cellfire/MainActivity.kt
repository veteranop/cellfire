package com.veteranop.cellfire

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import com.veteranop.cellfire.ui.theme.CellFireTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PREFS_NAME = "CellFirePrefs"
const val API_KEY_NAME = "opencellid_api_key"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FrequencyCalculator.init(applicationContext)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "CellFire-VeteranOp/1.0"

        setContent {
            CellFireTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val vm: CellFireViewModel = hiltViewModel()
                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") { StartScreen(navController) }
                        composable("scan") { ScanScreen(navController, vm) }
                        composable(
                            "detail/{pci}/{arfcn}",
                            arguments = listOf(
                                navArgument("pci") { type = NavType.IntType },
                                navArgument("arfcn") { type = NavType.IntType },
                            )
                        ) { backStackEntry ->
                            val pci = backStackEntry.arguments?.getInt("pci") ?: 0
                            val arfcn = backStackEntry.arguments?.getInt("arfcn") ?: 0
                            CellDetailScreen(vm, pci, arfcn)
                        }
                        composable("map") { MapScreen(vm) }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") { AboutScreen() }
                        composable("settings") { SettingsScreen() }
                        composable("pci_table") { PciTableScreen(vm) }
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CellFire", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("v1.0-veteranop", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("scan") }, modifier = Modifier.fillMaxWidth()) {
            Text("Start Scan")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("raw") }, modifier = Modifier.fillMaxWidth()) {
            Text("Raw Phone Output")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("pci_table") }, modifier = Modifier.fillMaxWidth()) {
            Text("Discovered PCIs")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("about") }, modifier = Modifier.fillMaxWidth()) {
            Text("About")
        }
    }
}

@Composable
fun ScanScreen(navController: NavController, vm: CellFireViewModel) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val deepScanActive by vm.deepScanActive.collectAsState()

    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.all { it }
        vm.onPermissionsResult(granted)
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            vm.onPermissionsResult(true)
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }

    var showLte by remember { mutableStateOf(true) }
    var show5g by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("CELLFIRE // VETERANOP", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text(
            text = if (state.allPermissionsGranted) "LOCKED ON TARGET" else "ACQUIRING PERMISSIONS...",
            color = if (state.allPermissionsGranted) Color.Green else Color.Red,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedButton(onClick = { vm.toggleMonitoring() }) {
                Text(if (state.isMonitoring) "CEASE FIRE" else "ENGAGE TARGETS", color = Color.White)
            }
            ElevatedButton(onClick = { navController.navigate("map") }) {
                Text("TOWER MAP", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        ElevatedButton(
            onClick = { vm.toggleDeepScan(!deepScanActive) },
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = if (deepScanActive) Color(0xFFFF2D00) else Color(0xFF444444)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DEEP SCAN: ${if (deepScanActive) "ACTIVE (5s)" else "OFF"}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showLte = !showLte },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (showLte) Color(0xFF00A8E8) else Color.Transparent)
            ) { Text("LTE", color = if (showLte) Color.White else Color.LightGray) }
            OutlinedButton(
                onClick = { show5g = !show5g },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (show5g) Color(0xFFE20074) else Color.Transparent)
            ) { Text("5G", color = if (show5g) Color.White else Color.LightGray) }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("CARRIER", Modifier.weight(1.6f), fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("BAND", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("PCI", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("RSRP", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("SNR", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
        }

        val filteredCells = state.cells.filter {
            val discoveredPci = state.discoveredPcis.find { pci -> pci.pci == it.pci }
            val isIgnored = discoveredPci?.isIgnored ?: false
            !isIgnored && ((it.type == "LTE" && showLte) || (it.type == "5G NR" && show5g))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredCells, key = { "${it.pci}-${it.arfcn}" }) { cell ->
                val discoveredPci = state.discoveredPcis.find { it.pci == cell.pci }
                val cellColor = when (cell.carrier.lowercase()) {
                    "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                    "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                    "at&t" -> Color(0xFF00A8E8)
                    "firstnet" -> Color.Black
                    "dish wireless" -> Color(0xFFFF6200)
                    else -> Color.DarkGray
                }
                val finalColor = if(discoveredPci?.isTargeted == true) Color.Cyan else cellColor

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(finalColor.copy(alpha = 0.4f))
                        .clickable { navController.navigate("detail/${cell.pci}/${cell.arfcn}") }
                        .padding(12.dp)
                ) {
                    Text(text = cell.carrier.uppercase(), modifier = Modifier.weight(1.6f), fontWeight = if (cell.isRegistered) FontWeight.ExtraBold else FontWeight.Normal, color = Color.White)
                    Text(text = cell.band, modifier = Modifier.weight(0.8f), color = Color.White)
                    Text(text = cell.pci.toString(), modifier = Modifier.weight(0.8f), color = Color.White)
                    Text(
                        text = cell.signalStrength.toString(),
                        modifier = Modifier.weight(1f),
                        color = if (cell.signalStrength > -85) Color.Green else if (cell.signalStrength > -100) Color.Yellow else Color.Red
                    )
                    Text(text = cell.signalQuality.toString(), modifier = Modifier.weight(1f), color = Color.White)
                }
            }
        }

        Text(text = "v1.0-veteranop • VeteranOp Industries", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellDetailScreen(vm: CellFireViewModel, pci: Int, arfcn: Int) {
    val state by vm.uiState.collectAsState()
    val cell by remember(state.cells, pci, arfcn) { derivedStateOf { state.cells.firstOrNull { it.pci == pci && it.arfcn == arfcn } } }
    val history by remember(state.signalHistory, pci, arfcn) { derivedStateOf { state.signalHistory[Pair(pci, arfcn)] ?: emptyList() } }
    var showDialog by remember { mutableStateOf(false) }

    val currentCell = cell
    if (currentCell == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Cell not found. Waiting for data...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var selectedCarrier by remember(currentCell) { mutableStateOf(currentCell.carrier) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cell Details", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), color = Color.White)
                    Button(onClick = { showDialog = true }) {
                        Text("Edit")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Carrier: ${currentCell.carrier}", color = Color.White)
                Text("PCI: ${currentCell.pci}", color = Color.White)
                Text("ARFCN: ${currentCell.arfcn}", color = Color.White)
                Text("Band: ${currentCell.band}", color = Color.White)
                Text("Type: ${currentCell.type}", color = Color.White)
                Text("RSRP: ${currentCell.signalStrength}", color = Color.White)
                Text("SINR: ${currentCell.signalQuality}", color = Color.White)
                Text("TAC: ${currentCell.tac}", color = Color.White)
                Text("Registered: ${currentCell.isRegistered}", color = Color.White)
                val freq = when (currentCell) {
                    is LteCell -> FrequencyCalculator.getLteFrequency(currentCell.arfcn)
                    is NrCell -> FrequencyCalculator.getNrFrequency(currentCell.arfcn)
                }
                if (freq != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("DL Freq: ${String.format("%.1f", freq.first)} MHz", color = Color.White)
                    Text("UL Freq: ${String.format("%.1f", freq.second)} MHz", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SignalChart(history = history)
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
                            vm.updatePci(pci, isIgnored = true)
                            showDialog = false 
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF600000))) { Text("Ignore") }
                        
                        val isTargeted = state.discoveredPcis.find { it.pci == pci }?.isTargeted == true
                        Button(onClick = { 
                            vm.updatePci(pci, isTargeted = !isTargeted)
                            showDialog = false
                        }, modifier = Modifier.weight(1f)) { Text(if (isTargeted) "Untarget" else "Target") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.updateCarrier(pci, arfcn, selectedCarrier)
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
    Card(modifier = Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
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

                chart.data = LineData(rsrpDataSet, sinrDataSet)
                chart.xAxis.labelCount = 5
                chart.notifyDataSetChanged()
                chart.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MapScreen(vm: CellFireViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(true)
                    controller.setZoom(16.0)
                    val provider = GpsMyLocationProvider(ctx)
                    val myLocationOverlay = MyLocationNewOverlay(provider, this)
                    myLocationOverlay.enableMyLocation()
                    myLocationOverlay.enableFollowLocation()
                    overlays.add(myLocationOverlay)
                }
            }
        )
    }
}

@Composable
fun RawLogScreen(vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(state.logLines) { line ->
            Text(text = line, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}

@Composable
fun PciTableScreen(vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Discovered PCIs", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PCI", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Text("Carrier", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2f))
                    Text("Count", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Text("Last Seen", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2f))
                    Text("Flags", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                }
                Divider(color = Color.Gray)
            }
            items(state.discoveredPcis.sortedByDescending { it.lastSeen }) { pci ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(pci.pci.toString(), color = Color.White, modifier = Modifier.weight(1f))
                    Text(pci.carrier, color = Color.White, modifier = Modifier.weight(2f))
                    Text(pci.discoveryCount.toString(), color = Color.White, modifier = Modifier.weight(1f))
                    Text(SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault()).format(Date(pci.lastSeen)), color = Color.White, modifier = Modifier.weight(2f))
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pci.isIgnored) Icon(Icons.Default.Block, contentDescription = "Ignored", tint = Color.Red, modifier=Modifier.size(16.dp))
                        if (pci.isTargeted) Icon(Icons.Default.Star, contentDescription = "Targeted", tint = Color.Yellow, modifier=Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var apiKey by remember { mutableStateOf(sharedPreferences.getString(API_KEY_NAME, "") ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style=MaterialTheme.typography.headlineLarge, color = Color.White)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("OpenCellID API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                with(sharedPreferences.edit()) {
                    putString(API_KEY_NAME, apiKey)
                    apply()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CellFire", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("v1.0-veteranop", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Made by VeteranOp LLC", style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
