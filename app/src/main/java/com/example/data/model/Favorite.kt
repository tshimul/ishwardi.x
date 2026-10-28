package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey
    val providerId: String,
    val pinnedOrder: Int = 0,
    val pinnedAt: Long = System.currentTimeMillis()
)
