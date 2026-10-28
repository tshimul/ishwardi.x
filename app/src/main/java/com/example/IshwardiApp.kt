package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseInitializer
import com.example.data.repository.AdminRepository
import com.example.data.repository.ProviderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IshwardiApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ProviderRepository(database) }
    val adminRepository by lazy { AdminRepository(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("IshwardiApp", "Firebase initialization: ${e.message}")
        }
        // Pre-populate initial categories, localities, and demonstration data in Room
        applicationScope.launch {
            DatabaseInitializer.seedIfNeeded(database)
        }
    }
}
