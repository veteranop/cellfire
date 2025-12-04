package com.veteranop.cellfire.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_pcis")
data class DiscoveredPci(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pci: Int,
    var carrier: String,
    var band: String,
    var discoveryCount: Int,
    var lastSeen: Long,
    var isIgnored: Boolean = false,
    var isTargeted: Boolean = false
)
