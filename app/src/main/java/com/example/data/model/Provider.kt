package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "providers",
    indices = [
        Index(value = ["contactNumber"]),
        Index(value = ["primaryCategory"]),
        Index(value = ["locationArea"]),
        Index(value = ["isActive"]),
        Index(value = ["isVerified"])
    ]
)
data class ProviderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val banglaName: String = "",
    val primaryCategory: String,
    val additionalCategories: String = "", // Comma-separated categories
    val contactNumber: String,
    val additionalContact: String = "",
    val locationArea: String,
    val specificAddress: String = "",
    val landmark: String = "",
    val chargeEstimate: String = "",
    val description: String = "",
    val whatsappNumber: String = "",
    val facebookUrl: String = "",
    val mapCoordinates: String = "", // "lat,lng" or map search query
    val workingHours: String = "",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val isDemoData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getAllCategories(): List<String> {
        val list = mutableListOf(primaryCategory)
        if (additionalCategories.isNotBlank()) {
            list.addAll(additionalCategories.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != primaryCategory })
        }
        return list
    }
}
