package com.veteranop.cellfire

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "CellFire-VeteranOp/1.0"

        setContent {
            CellFireTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
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
                            CellDetailScreen(vm, pci, arfcn, navController)
                        }
                        composable("map") { MapScreen(vm) }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") { AboutScreen() }
                        composable("settings") { SettingsScreen() }
                        composable("pci_table") { PciTableScreen(navController, vm) }
                        composable("pci_carrier_list/{carrier}") { backStackEntry ->
                            val carrier = backStackEntry.arguments?.getString("carrier") ?: ""
                            PciCarrierListScreen(vm, carrier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreen(navController: NavController) {
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
            Text("v1.0.1.2.export_alpha", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate("scan") }, modifier = Modifier.fillMaxWidth()) { Text("Start Scan") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("raw") }, modifier = Modifier.fillMaxWidth()) { Text("Raw Phone Output") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("pci_table") }, modifier = Modifier.fillMaxWidth()) { Text("Discovered PCIs") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("about") }, modifier = Modifier.fillMaxWidth()) { Text("About") }
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { vm.toggleMonitoring() }) {
                        Text(if (state.isMonitoring) "CEASE FIRE" else "ENGAGE TARGETS", color = Color.White)
                    }
                    ElevatedButton(
                        onClick = { vm.toggleDeepScan(!deepScanActive) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (deepScanActive) Color(0xFFFF6B00) else Color(0xFF2D2D2D),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        ),
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(32.dp))
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

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("CARRIER", Modifier.weight(1.6f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("BAND", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("PCI", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("RSRP", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text("SNR", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                }

                val filteredCells = state.cells.filter {
                    val discoveredPci = state.discoveredPcis.find { pci -> pci.pci == it.pci && pci.band == it.band }
                    val isIgnored = discoveredPci?.isIgnored ?: false
                    !isIgnored
                }

                SwipeRefresh(
                    state = rememberSwipeRefreshState(state.isRefreshing),
                    onRefresh = { vm.refresh() },
                ) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredCells, key = { "${it.pci}-${it.arfcn}" }) { cell ->
                            val discoveredPci = state.discoveredPcis.find { it.pci == cell.pci && it.band == cell.band }
                            val cellColor = when (cell.carrier.lowercase()) {
                                "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                                "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                                "at&t" -> Color(0xFF00A8E8)
                                "firstnet" -> Color.Black
                                "dish wireless" -> Color(0xFFFF6200)
                                else -> Color.DarkGray
                            }
                            val finalColor = if (discoveredPci?.isTargeted == true) Color.Cyan else cellColor

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
                }

                Text(text = "v1.0.1.2.export_alpha • VeteranOp Industries", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
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
                    Text("RSRQ: ${currentCell.rsrq}", color = Color.White)
                    Text("TAC: ${currentCell.tac}", color = Color.White)
                    Text("Registered: ${currentCell.isRegistered}", color = Color.White)
                    Text("Latitude: ${currentCell.latitude}", color = Color.White)
                    Text("Longitude: ${currentCell.longitude}", color = Color.White)
                    val freq = when (currentCell) {
                        is LteCell -> FrequencyCalculator.earfcnToFrequency(currentCell.arfcn)
                        else -> null
                    }
                    if (freq != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DL Freq: ${String.format("%.1f", freq.first)} MHz", color = Color.White)
                        if (freq.second > 0) {
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
fun PciCarrierListScreen(vm: CellFireViewModel, carrier: String) {
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
                        modifier = Modifier.fillMaxWidth().height(60.dp)
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
        Text("v1.0.1.2.export_alpha", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Made by VeteranOp LLC", style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
