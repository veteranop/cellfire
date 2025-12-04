package com.veteranop.cellfire.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_pci")
data class DiscoveredPci(
    @PrimaryKey val pci: Int,
    var carrier: String = "Unknown",
    var discoveryCount: Int = 1,
    var lastSeen: Long = System.currentTimeMillis(),
    var isIgnored: Boolean = false,
    var isTargeted: Boolean = false
)