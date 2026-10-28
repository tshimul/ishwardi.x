package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String, // Normalized identifier, e.g. "plumbing"
    val name: String,
    val banglaName: String = "",
    val iconKey: String = "build", // Material icon identifier
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false
)

data class CategoryWithCount(
    val category: CategoryEntity,
    val providerCount: Int
)
