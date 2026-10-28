package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_areas")
data class LocationAreaEntity(
    @PrimaryKey
    val id: String, // e.g. "ishwardi_sadar"
    val name: String,
    val banglaName: String = "",
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true
)
