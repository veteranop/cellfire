package com.veteranop.cellfire

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

@Composable
fun PciMapScreen(
    navController: NavController,
    pci: Int,
    vm: CellFireViewModel = hiltViewModel()
) {
    Log.d("CellFire", "Map for PCI $pci")
    var points by remember { mutableStateOf(emptyList<DriveTestPoint>()) }
    var showAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(pci, showAll) {
        val list = if (showAll) {
            Log.d("CellFire", "Querying all points")
            vm.cellRepository.getAllPointsSync()
        } else {
            Log.d("CellFire", "Querying points for PCI $pci")
            vm.cellRepository.getPointsForPciSync(pci)
        }
        Log.d("CellFire", "Query result: ${list.size} points")
        points = list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Points: ${points.size}", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    scope.launch {
                        val file = vm.cellRepository.exportPciCsv(pci)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share CSV"))
                    }
                }) {
                    Text("Export CSV")
                }
            }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(43.489, -112.022)) // INL default
                    }
                },
                modifier = Modifier.weight(1f),
                update = { mv ->
                    // Optimization: Use tag to only update markers when points actually change.
                    // This prevents constant re-creation which causes lag and click failures.
                    val pointsKey = points.hashCode() + showAll.hashCode()
                    if (mv.tag != pointsKey) {
                        mv.tag = pointsKey
                        mv.overlays.clear()
                        
                        if (points.isNotEmpty()) {
                            // Create one info window instance to be shared/reused
                            val commonInfoWindow = MarkerInfoWindow(R.layout.bubble, mv)
                            
                            val latList = points.map { it.latitude }
                            val lonList = points.map { it.longitude }
                            val bounds = BoundingBox(
                                latList.maxOrNull() ?: 0.0,
                                lonList.maxOrNull() ?: 0.0,
                                latList.minOrNull() ?: 0.0,
                                lonList.minOrNull() ?: 0.0
                            )

                            points.forEach { point ->
                                val loc = GeoPoint(point.latitude, point.longitude)
                                val marker = Marker(mv)
                                marker.position = loc
                                marker.title = "PCI ${point.pci}"
                                marker.snippet = "RSRP: ${point.rsrp} dBm\nSNR: ${point.snr} dB\nBand: ${point.band}"
                                
                                val color = when {
                                    point.rsrp > -85 -> Color.GREEN
                                    point.rsrp > -105 -> Color.YELLOW
                                    else -> Color.RED
                                }
                                
                                val drawable = GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    setColor(color)
                                    setSize(24, 24)
                                    setStroke(1, Color.WHITE)
                                }
                                marker.icon = drawable
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                marker.infoWindow = commonInfoWindow
                                mv.overlays.add(marker)
                            }
                            
                            // Auto-zoom to fit the new data set
                            mv.post {
                                try {
                                    mv.zoomToBoundingBox(bounds, true, 100)
                                } catch (e: Exception) {
                                    Log.e("CellFire", "Zoom failed", e)
                                }
                            }
                        }
                        mv.invalidate()
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = { showAll = !showAll },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(if (showAll) "SHOW PCI" else "SHOW ALL")
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No drive data collected yet.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
