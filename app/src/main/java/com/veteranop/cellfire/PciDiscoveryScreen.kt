package com.veteranop.cellfire

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun PciDiscoveryScreen(
    navController: NavController,
    viewModel: CellFireViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val discoveredPcis = state.discoveredPcis

    LazyColumn {
        items(discoveredPcis) { pci ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                    navController.navigate("pci_map/${pci.pci}")  // Navigate to map with PCI
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
