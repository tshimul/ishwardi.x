package com.example.data.model

data class SyncStatus(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTimeMillis: Long = System.currentTimeMillis(),
    val syncMessage: String = "Local Room Cache Active (Offline Ready)",
    val totalSyncedCount: Int = 0
)
