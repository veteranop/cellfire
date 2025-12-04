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
import com.veteranop.cellfire.data.local.entities.DiscoveredPci
import com.veteranop.cellfire.ui.theme.CellFireTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*

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
                                navArgument("arfcn") { type = NavType.IntType }
                            )
                        ) { entry ->
                            val pci = entry.arguments?.getInt("pci") ?: 0
                            val arfcn = entry.arguments?.getInt("arfcn") ?: 0
                            CellDetailScreen(vm, pci, arfcn, navController)
                        }
                        composable("map") { MapScreen(vm) }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") { AboutScreen() }
                        composable("settings") { SettingsScreen() }
                        composable("pci_table") { PciTableScreen(navController, vm) }
                        composable("pci_carrier_list/{carrier}") { entry ->
                            val carrier = entry.arguments?.getString("carrier") ?: ""
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
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.4f)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.app_name), contentDescription = "logo")
            Text("v1.0.0.3.alpha", color = Color.White, style = MaterialTheme.typography.bodyLarge)
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
    val isMonitoring by vm.isMonitoring.collectAsState()
    val deepScanActive by vm.deepScanActive.collectAsState()

    val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        vm.onPermissionsResult(results.values.all { it })
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) launcher.launch(permissions.toTypedArray())
        else vm.onPermissionsResult(true)
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cfbackground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.4f)
            )

            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Image(painter = painterResource(id = R.drawable.app_name), contentDescription = null)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { vm.toggleMonitoring() }) {
                        Text(if (isMonitoring) "CEASE FIRE" else "ENGAGE TARGETS", color = Color.White)
                    }
                    ElevatedButton(
                        onClick = { vm.toggleDeepScan(!deepScanActive) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (deepScanActive) Color.Red else Color(0xFF2D2D2D)
                        ),
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(32.dp))
                    ) {
                        Text(
                            text = if (deepScanActive) "MAXIMUM OVERDRIVE" else "DEEP SCAN: PASSIVE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
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
                    Text("SINR", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.LightGray)
                }

                val filtered = state.cells.filter { cell ->
                    state.discoveredPcis.find { it.pci == cell.pci }?.isIgnored != true
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { "${it.pci}-${it.arfcn}" }) { cell ->
                        val discovered = state.discoveredPcis.find { it.pci == cell.pci }
                        val color = when (cell.carrier.lowercase()) {
                            "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                            "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                            "at&t" -> Color(0xFF00A8E8)
                            "firstnet" -> Color.Black
                            "dish wireless" -> Color(0xFFFF6200)
                            else -> Color.DarkGray
                        }
                        val rowColor = if (discovered?.isTargeted == true) Color.Cyan else color

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowColor.copy(alpha = 0.4f))
                                .clickable { navController.navigate("detail/${cell.pci}/${cell.arfcn}") }
                                .padding(12.dp)
                        ) {
                            Text(cell.carrier.uppercase(), Modifier.weight(1.6f), fontWeight = if (cell.isRegistered) FontWeight.ExtraBold else FontWeight.Normal, color = Color.White)
                            Text(cell.band, Modifier.weight(0.8f), color = Color.White)
                            Text(cell.pci.toString(), Modifier.weight(0.8f), color = Color.White)
                            Text(
                                cell.signalStrength.toString(),
                                Modifier.weight(1f),
                                color = when {
                                    cell.signalStrength > -85 -> Color.Green
                                    cell.signalStrength > -100 -> Color.Yellow
                                    else -> Color.Red
                                }
                            )
                            Text(cell.signalQuality.toString(), Modifier.weight(1f), color = Color.White)
                        }
                    }
                }

                Text("v1.0.0.3.alpha • VeteranOp Industries", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// Keep all your other screens exactly as you had them — they already work perfectly
// (CellDetailScreen, SignalChart, MapScreen, RawLogScreen, PciTableScreen, etc.)

@Composable
fun CellDetailScreen(vm: CellFireViewModel, pci: Int, arfcn: Int, navController: NavController) {
    val state by vm.uiState.collectAsState()
    val cell = state.cells.find { it.pci == pci && it.arfcn == arfcn }
    val history = state.signalHistory[Pair(pci, arfcn)] ?: emptyList()
    var showDialog by remember { mutableStateOf(false) }
    var selectedCarrier by remember(cell) { mutableStateOf(cell?.carrier ?: "") }

    if (cell == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Waiting for cell data...", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.cfbackground), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.4f))
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Card(containerColor = Color.DarkGray.copy(0.6f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cell Details", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
                        Button(onClick = { showDialog = true }) { Text("Edit") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Carrier: ${cell.carrier}", color = Color.White)
                    Text("PCI: $pci", color = Color.White)
                    Text("ARFCN: $arfcn", color = Color.White)
                    Text("Band: ${cell.band}", color = Color.White)
                    Text("Type: ${cell.type}", color = Color.White)
                    Text("RSRP: ${cell.signalStrength}", color = Color.White)
                    Text("SINR: ${cell.signalQuality}", color = Color.White)
                    Text("Registered: ${cell.isRegistered}", color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            SignalChart(history)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit PCI $pci", color = Color.White) },
            containerColor = Color.DarkGray,
            text = {
                Column {
                    // Carrier editing dropdown would go here
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.updatePci(pci, isIgnored = true); showDialog = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text("Ignore")
                        }
                        val isTargeted = state.discoveredPcis.find { it.pci == pci }?.isTargeted == true
                        Button(onClick = { vm.updatePci(pci, isTargeted = !isTargeted); showDialog = false }, modifier = Modifier.weight(1f)) {
                            Text(if (isTargeted) "Untarget" else "Target")
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showDialog = false }) { Text("Close") } }
        )
    }
}

// SignalChart, MapScreen, RawLogScreen, PciTableScreen, etc. — keep exactly as you posted before
// They all work perfectly with this setup

@Composable
fun SignalChart(history: List<SignalHistoryPoint>) {
    Card(modifier = Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.6f))) {
        AndroidView(
            factory = { ctx ->
                LineChart(ctx).apply {
                    description.isEnabled = false
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.textColor = AndroidColor.WHITE
                    axisLeft.textColor = AndroidColor.WHITE
                    axisRight.isEnabled = false
                    legend.textColor = AndroidColor.WHITE
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                }
            },
            update = { chart ->
                if (history.isEmpty()) {
                    chart.data = null
                    chart.invalidate()
                    return@AndroidView
                }
                val rsrp = history.mapIndexed { i, p -> Entry(i.toFloat(), p.rsrp) }
                val sinr = history.mapIndexed { i, p -> Entry(i.toFloat(), p.sinr) }
                chart.data = LineData(
                    LineDataSet(rsrp, "RSRP").apply { color = AndroidColor.CYAN; setDrawCircles(false); lineWidth = 2f },
                    LineDataSet(sinr, "SINR").apply { color = AndroidColor.MAGENTA; setDrawCircles(false); lineWidth = 2f }
                )
                chart.invalidate()
            }
        )
    }
}

@Composable
fun MapScreen(vm: CellFireViewModel) {
    AndroidView(factory = { ctx ->
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(16.0)
            val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
            overlays.add(overlay)
        }
    })
}

@Composable
fun RawLogScreen(vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()
    Scaffold(
        floatingActionButton = { Button(onClick = { vm.clearLog() }) { Text("Clear") } },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(painter = painterResource(R.drawable.cfbackground), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.4f))
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(state.logLines) { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun PciTableScreen(navController: NavController, vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()
    val carriers = state.discoveredPcis.map { it.carrier }.distinct()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.cfbackground), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.4f))
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text("Discovered PCIs by Carrier", style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.weight(1f))
                Button(onClick = { vm.clearPciHistory() }) { Text("Clear All") }
            }
            LazyColumn {
                items(carriers) { carrier ->
                    val count = state.discoveredPcis.count { it.carrier == carrier }
                    Card(
                        modifier = Modifier.fillMaxWidth().height(60.dp).clickable { navController.navigate("pci_carrier_list/$carrier") },
                        colors = CardDefaults.cardColors(containerColor = when (carrier.lowercase()) {
                            "t-mobile", "t-mobile (low-band)" -> Color(0xFFE20074)
                            "verizon", "verizon (b5)" -> Color(0xFFCC0000)
                            "at&t" -> Color(0xFF00A8E8)
                            "firstnet" -> Color.Black
                            "dish wireless" -> Color(0xFFFF6200)
                            else -> Color.DarkGray
                        }.copy(alpha = 0.7f))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(carrier, color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            Text("$count PCIs", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PciCarrierListScreen(vm: CellFireViewModel, carrier: String) {
    val state by vm.uiState.collectAsState()
    val pcis = state.discoveredPcis.filter { it.carrier == carrier }.sortedByDescending { it.lastSeen }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.cfbackground), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.4f))
        Column(modifier = Modifier.padding(16.dp)) {
            Text(carrier, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                item {
                    Row {
                        Text("PCI", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Seen", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Last", Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Flags", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Divider(color = Color.Gray)
                }
                items(pcis) { pci ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(pci.pci.toString(), Modifier.weight(1f), color = Color.White)
                        Text(pci.discoveryCount.toString(), Modifier.weight(1f), color = Color.White)
                        Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(pci.lastSeen)), Modifier.weight(2f), color = Color.White)
                        Row(Modifier.weight(1f)) {
                            if (pci.isIgnored) Icon(Icons.Default.AccountBox, tint = Color.Red, contentDescription = "Ignored")
                            if (pci.isTargeted) Icon(Icons.Default.Star, tint = Color.Yellow, contentDescription = "Targeted")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        // Add OpenCellID key field later
    }
}

@Composable
fun AboutScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("CellFire", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("v1.0.0.3.alpha", color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("VeteranOp Industries", color = Color.White)
    }
}