package com.veteranop.cellfire.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

const val PREFS_NAME = "CellFirePrefs"
const val CROWDSOURCE_ENABLED_KEY = "crowdsource_enabled"

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var crowdsourceEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean(CROWDSOURCE_ENABLED_KEY, true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Upload cell data")
            Switch(
                checked = crowdsourceEnabled,
                onCheckedChange = { enabled ->
                    crowdsourceEnabled = enabled
                    sharedPreferences.edit().putBoolean(CROWDSOURCE_ENABLED_KEY, enabled).apply()
                }
            )
        }
    }
}
