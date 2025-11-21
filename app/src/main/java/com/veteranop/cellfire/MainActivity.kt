package com.veteranop.cellfire

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.veteranop.cellfire.ui.theme.CellFireTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val vm: CellFireViewModel = hiltViewModel()
                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") { StartScreen(navController) }
                        composable("scan") { ScanScreen(navController, vm) }
                        composable("map") { MapScreen(vm) }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") { AboutScreen() }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }
}

// START SCREEN
@Composable
fun StartScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CellFire", style = MaterialTheme.typography.headlineLarge)
        Text("v1.0-veteranop", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("scan") }, modifier = Modifier.fillMaxWidth()) {
            Text("Start Scan")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("raw") }, modifier = Modifier.fillMaxWidth()) {
            Text("Raw Phone Output")
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

// SCAN SCREEN — TACTICAL BATTLEFIELD WITH LTE/5G TOGGLE
@Composable
fun ScanScreen(navController: NavController, vm: CellFireViewModel) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()

    val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.values.all { it }
        vm.onPermissionsGranted(allGranted)
        if (allGranted) {
            vm.startMonitoring()
        }
    }

    var showLte by remember { mutableStateOf(true) }
    var show5g by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            vm.onPermissionsGranted(true)
            vm.startMonitoring()
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }

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

        // LTE / 5G TOGGLE BUTTONS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showLte = !showLte },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (showLte) Color(0xFF00A8E8) else Color.Transparent
                )
            ) {
                Text("LTE", color = if (showLte) Color.White else Color.LightGray)
            }
            OutlinedButton(
                onClick = { show5g = !show5g },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (show5g) Color(0xFFE20074) else Color.Transparent
                )
            ) {
                Text("5G", color = if (show5g) Color.White else Color.LightGray)
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
            (it.type == "LTE" && showLte) || (it.type == "5G NR" && show5g)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredCells) { cell ->
                val backgroundColor = when (cell.carrier.lowercase()) {
                    "t-mobile", "t mobile" -> Color(0xFFE20074).copy(alpha = 0.3f)
                    "verizon" -> Color(0xFFCC0000).copy(alpha = 0.3f)
                    "at&t", "att" -> Color(0xFF00A8E8).copy(alpha = 0.3f)
                    "firstnet" -> Color.Black.copy(alpha = 0.5f)
                    "dish", "dish wireless" -> Color(0xFFFF6200).copy(alpha = 0.3f)
                    else -> Color.DarkGray.copy(alpha = 0.2f)
                }

                val fontWeight = if (cell.isRegistered) FontWeight.ExtraBold else FontWeight.Normal

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .clickable { navController.navigate("map") }
                        .padding(12.dp)
                ) {
                    Text(cell.carrier.uppercase(), Modifier.weight(1.6f), fontWeight = fontWeight, color = Color.White)
                    Text(cell.band, Modifier.weight(0.8f), fontWeight = fontWeight, color = Color.White)
                    Text(cell.pci.toString(), Modifier.weight(0.8f), fontWeight = fontWeight, color = Color.White)
                    Text(
                        cell.signalStrength.toString(),
                        Modifier.weight(1f),
                        fontWeight = fontWeight,
                        color = if (cell.signalStrength > -80) Color.Green else if (cell.signalStrength > -100) Color.Yellow else Color.Red
                    )
                    Text(cell.signalQuality.toString(), Modifier.weight(1f), fontWeight = fontWeight, color = Color.White)
                }
            }
        }

        Text("v1.0-veteranop • VeteranOp Industries", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
    }
}

// MAP SCREEN — YOUR LOCATION + BLUE DOT
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
                    myLocationOverlay.runOnFirstFix {
                        ctx.mainExecutor.execute {
                            myLocationOverlay.myLocation?.let {
                                controller.animateTo(it)
                            }
                        }
                    }
                    overlays.add(myLocationOverlay)
                }
            }
        )

        FloatingActionButton(
            onClick = { /* re-center */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text("↗", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        }
    }
}

// RAW LOG SCREEN
@Composable
fun RawLogScreen(vm: CellFireViewModel) {
    val state by vm.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(state.logLines) { line ->
            Text(text = line, style = MaterialTheme.typography.bodySmall)
        }
    }
}


// SETTINGS SCREEN
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var apiKey by remember { mutableStateOf(sharedPreferences.getString(API_KEY_NAME, "") ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("OpenCellID API Key") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            with(sharedPreferences.edit()) {
                putString(API_KEY_NAME, apiKey)
                apply()
            }
        }) {
            Text("Save")
        }
    }
}

// ABOUT SCREEN
@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CellFire", style = MaterialTheme.typography.headlineLarge)
        Text("v1.0-veteranop", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Made by VeteranOp Industries", style = MaterialTheme.typography.bodyMedium)
    }
}
