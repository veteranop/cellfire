package com.veteranop.cellfire

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PciDiscoveryScreen(
    navController: NavController,
    viewModel: CellFireViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val discoveredPcis = state.discoveredPcis

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Discovered PCIs", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(discoveredPcis) { pci ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            navController.navigate("pci_map/${pci.pci}")
                        }
                ) {
                    ListItem(
                        headlineContent = { Text("PCI: ${pci.pci}") },
                        supportingContent = { Text("Carrier: ${pci.carrier} | Band: ${pci.band}") }
                    )
                }
            }
        }
    }
}
