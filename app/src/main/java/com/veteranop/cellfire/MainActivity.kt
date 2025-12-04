package com.veteranop.cellfire

const val PREFS_NAME = "CellFirePrefs"
const val API_KEY_NAME = "opencellid_api_key"

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
import com.veteranop.cellfire.data.local.entities.DiscoveredPci
import com.veteranop.cellfire.domain.model.Cell
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
                        composable("detail/{pci}/{arfcn}", arguments = listOf(navArgument("pci") { type = NavType.IntType }, navArgument("arfcn") { type = NavType.IntType })) {
                            val pci = it.arguments?.getInt("pci") ?: 0
                            val arfcn = it.arguments?.getInt("arfcn") ?: 0
                            CellDetailScreen(vm, pci, arfcn, navController)
                        }
                        composable("map") { MapScreen(vm) }
                        composable("raw") { RawLogScreen(vm) }
                        composable("about") { AboutScreen() }
                        composable("settings") { SettingsScreen() }
                        composable("pci_table") { PciTableScreen(navController, vm) }
                        composable("pci_carrier_list/{carrier}") { PciCarrierListScreen(vm, it.arguments?.getString("carrier") ?: "") }
                    }
                }
            }
        }
    }
}

// ← Paste ALL your @Composable functions (StartScreen, ScanScreen, etc.) BELOW this line
// Do NOT paste anything above this point except what is already here