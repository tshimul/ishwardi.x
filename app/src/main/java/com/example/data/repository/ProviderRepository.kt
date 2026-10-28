package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryWithCount
import com.example.data.model.FavoriteEntity
import com.example.data.model.LocationAreaEntity
import com.example.data.model.ProviderEntity
import com.example.data.model.SyncStatus
import com.example.util.CsvImportResult
import com.example.util.CsvParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SortOption {
    NAME_ASC,
    RECENTLY_UPDATED,
    VERIFIED_FIRST
}

class ProviderRepository(private val database: AppDatabase) {

    private val providerDao = database.providerDao()
    private val categoryDao = database.categoryDao()
    private val locationAreaDao = database.locationAreaDao()
    private val favoriteDao = database.favoriteDao()

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var realtimeListenerRegistration: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow(
        SyncStatus(
            isOnline = true,
            isSyncing = false,
            lastSyncTimeMillis = System.currentTimeMillis(),
            syncMessage = "Connecting to Firestore...",
            totalSyncedCount = 0
        )
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val allActiveProviders: Flow<List<ProviderEntity>> = providerDao.getAllActiveProviders()
    val allAdminProviders: Flow<List<ProviderEntity>> = providerDao.getAllProvidersAdmin()
    val pinnedProviders: Flow<List<ProviderEntity>> = providerDao.getPinnedProviders()
    val enabledCategories: Flow<List<CategoryEntity>> = categoryDao.getEnabledCategories()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val enabledLocationAreas: Flow<List<LocationAreaEntity>> = locationAreaDao.getEnabledAreas()
    val allLocationAreas: Flow<List<LocationAreaEntity>> = locationAreaDao.getAllAreas()
    val activeCount: Flow<Int> = providerDao.getActiveCount()

    fun getProviderById(id: String): Flow<ProviderEntity?> = providerDao.getProviderById(id)

    fun isFavorite(providerId: String): Flow<Boolean> = favoriteDao.isFavorite(providerId)

    /**
     * Categories with live active provider counts
     */
    val categoriesWithCounts: Flow<List<CategoryWithCount>> = combine(
        enabledCategories,
        allActiveProviders
    ) { categories, providers ->
        categories.map { cat ->
            val count = providers.count { p ->
                p.primaryCategory.equals(cat.name, ignoreCase = true) ||
                    p.getAllCategories().any { it.equals(cat.name, ignoreCase = true) }
            }
            CategoryWithCount(cat, count)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Search and multi-filter with sorting
     */
    fun filterProviders(
        searchQuery: String,
        selectedCategory: String?,
        selectedLocation: String?,
        sortOption: SortOption
    ): Flow<List<ProviderEntity>> {
        return allActiveProviders.map { list ->
            var filtered = list

            val query = searchQuery.trim()
            if (query.isNotBlank()) {
                val qLower = query.lowercase()
                filtered = filtered.filter { p ->
                    p.name.lowercase().contains(qLower) ||
                        p.banglaName.contains(query, ignoreCase = true) ||
                        p.primaryCategory.lowercase().contains(qLower) ||
                        p.additionalCategories.lowercase().contains(qLower) ||
                        p.contactNumber.contains(query) ||
                        p.additionalContact.contains(query) ||
                        p.locationArea.lowercase().contains(qLower) ||
                        p.specificAddress.lowercase().contains(qLower) ||
                        p.landmark.lowercase().contains(qLower) ||
                        p.description.lowercase().contains(qLower)
                }
            }

            if (!selectedCategory.isNullOrBlank() && selectedCategory != "All") {
                filtered = filtered.filter { p ->
                    p.primaryCategory.equals(selectedCategory, ignoreCase = true) ||
                        p.getAllCategories().any { it.equals(selectedCategory, ignoreCase = true) }
                }
            }

            if (!selectedLocation.isNullOrBlank() && selectedLocation != "All") {
                filtered = filtered.filter { p ->
                    p.locationArea.equals(selectedLocation, ignoreCase = true) ||
                        p.specificAddress.contains(selectedLocation, ignoreCase = true)
                }
            }

            when (sortOption) {
                SortOption.NAME_ASC -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                SortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
                SortOption.VERIFIED_FIRST -> filtered.sortedWith(
                    compareByDescending<ProviderEntity> { it.isVerified }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Starts Firestore Real-time synchronization.
     * Public mode queries: isActive == true && isVerified == true (matching Security Rules).
     * Admin mode queries: all records in /providers.
     * Updates Room local cache automatically.
     */
    fun startRealtimeSync(isAdmin: Boolean = false) {
        realtimeListenerRegistration?.remove()

        val query: Query = if (isAdmin && auth.currentUser != null) {
            firestore.collection("providers")
        } else {
            firestore.collection("providers")
                .whereEqualTo("isActive", true)
                .whereEqualTo("isVerified", true)
        }

        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = true,
            syncMessage = if (isAdmin) "Listening to Cloud Directory (Admin Mode)..." else "Connecting to Cloud Directory..."
        )

        realtimeListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreSync", "Snapshot listener error: ${error.message}", error)
                _syncStatus.value = _syncStatus.value.copy(
                    isOnline = false,
                    isSyncing = false,
                    syncMessage = "Offline Mode (Cached): ${error.localizedMessage ?: "Permission or connection issue"}"
                )
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val cloudProviders = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToProvider(doc)
                }

                repoScope.launch {
                    try {
                        if (cloudProviders.isNotEmpty()) {
                            providerDao.insertAll(cloudProviders)
                        }
                        val localTotal = providerDao.getTotalCountDirect()
                        _syncStatus.value = SyncStatus(
                            isOnline = true,
                            isSyncing = false,
                            lastSyncTimeMillis = System.currentTimeMillis(),
                            syncMessage = "Directory Synchronized (${cloudProviders.size} live in cloud, $localTotal cached)",
                            totalSyncedCount = cloudProviders.size
                        )
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Failed to cache snapshot into Room: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun stopRealtimeSync() {
        realtimeListenerRegistration?.remove()
        realtimeListenerRegistration = null
    }

    /**
     * One-time directory refresh from Cloud Firestore into Room cache.
     */
    suspend fun syncDirectory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                syncMessage = "Refreshing from Cloud Directory..."
            )
            val query: Query = if (auth.currentUser != null) {
                firestore.collection("providers")
            } else {
                firestore.collection("providers")
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("isVerified", true)
            }
            val snapshot = query.get().await()
            val cloudProviders = snapshot.documents.mapNotNull { mapDocumentToProvider(it) }
            if (cloudProviders.isNotEmpty()) {
                providerDao.insertAll(cloudProviders)
            }
            val localTotal = providerDao.getTotalCountDirect()
            _syncStatus.value = SyncStatus(
                isOnline = true,
                isSyncing = false,
                lastSyncTimeMillis = System.currentTimeMillis(),
                syncMessage = "Directory Synchronized (${cloudProviders.size} live, $localTotal cached)",
                totalSyncedCount = cloudProviders.size
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Manual sync fetch failed: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isOnline = false,
                isSyncing = false,
                syncMessage = "Offline Mode (Cached): ${e.localizedMessage ?: "Sync error"}"
            )
            Result.failure(e)
        }
    }

    /**
     * Toggles bookmark/favorite. Enforces maximum of 5 favorites rule.
     */
    suspend fun toggleFavorite(providerId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavoriteDirect(providerId)
        if (isFav) {
            favoriteDao.deleteFavorite(providerId)
            Result.success(false)
        } else {
            val count = favoriteDao.getFavoriteCount()
            if (count >= 5) {
                Result.failure(IllegalStateException("Maximum of 5 pinned providers allowed. Please unpin a provider first."))
            } else {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        providerId = providerId,
                        pinnedOrder = count + 1,
                        pinnedAt = System.currentTimeMillis()
                    )
                )
                Result.success(true)
            }
        }
    }

    /**
     * Creates a new provider.
     * Attempts to write to Firestore first using provider.id as document key.
     * On success, writes to local Room cache.
     * On failure, returns Result.failure without false success reporting.
     */
    suspend fun addProvider(provider: ProviderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (provider.name.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be blank"))
        if (provider.primaryCategory.isBlank()) return@withContext Result.failure(IllegalArgumentException("Primary category is required"))
        if (provider.contactNumber.isBlank()) return@withContext Result.failure(IllegalArgumentException("Contact number is required"))
        if (provider.locationArea.isBlank()) return@withContext Result.failure(IllegalArgumentException("Location area is required"))

        val timestamp = System.currentTimeMillis()
        val preparedProvider = provider.copy(
            createdAt = if (provider.createdAt == 0L) timestamp else provider.createdAt,
            updatedAt = timestamp
        )

        // If authenticated with Firebase, write to Firestore
        if (auth.currentUser != null) {
            try {
                val docMap = providerToMap(preparedProvider)
                firestore.collection("providers")
                    .document(preparedProvider.id)
                    .set(docMap)
                    .await()
            } catch (e: Exception) {
                Log.e("ProviderRepository", "Firestore write failed: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

        // Write to local Room
        providerDao.insert(preparedProvider)
        Result.success(Unit)
    }

    /**
     * Updates an existing provider in Firestore and local Room cache.
     */
    suspend fun updateProvider(provider: ProviderEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (provider.name.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be blank"))
        if (provider.primaryCategory.isBlank()) return@withContext Result.failure(IllegalArgumentException("Primary category is required"))
        if (provider.contactNumber.isBlank()) return@withContext Result.failure(IllegalArgumentException("Contact number is required"))

        val prepared = provider.copy(updatedAt = System.currentTimeMillis())

        if (auth.currentUser != null) {
            try {
                val docMap = providerToMap(prepared)
                firestore.collection("providers")
                    .document(prepared.id)
                    .set(docMap)
                    .await()
            } catch (e: Exception) {
                Log.e("ProviderRepository", "Firestore update failed: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

        providerDao.update(prepared)
        Result.success(Unit)
    }

    /**
     * Deletes a provider from Firestore and local Room cache.
     */
    suspend fun deleteProvider(providerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser != null) {
            try {
                firestore.collection("providers").document(providerId).delete().await()
            } catch (e: Exception) {
                Log.e("ProviderRepository", "Firestore delete failed: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

        favoriteDao.deleteFavorite(providerId)
        providerDao.deleteById(providerId)
        Result.success(Unit)
    }

    suspend fun toggleActiveStatus(providerId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val p = providerDao.getProviderByIdDirect(providerId)
            ?: return@withContext Result.failure(IllegalArgumentException("Provider not found"))

        val updated = p.copy(isActive = isActive, updatedAt = System.currentTimeMillis())

        if (auth.currentUser != null) {
            try {
                firestore.collection("providers").document(providerId)
                    .update("isActive", isActive, "updatedAt", updated.updatedAt)
                    .await()
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        providerDao.update(updated)
        Result.success(Unit)
    }

    suspend fun toggleVerifiedStatus(providerId: String, isVerified: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val p = providerDao.getProviderByIdDirect(providerId)
            ?: return@withContext Result.failure(IllegalArgumentException("Provider not found"))

        val updated = p.copy(isVerified = isVerified, updatedAt = System.currentTimeMillis())

        if (auth.currentUser != null) {
            try {
                firestore.collection("providers").document(providerId)
                    .update("isVerified", isVerified, "updatedAt", updated.updatedAt)
                    .await()
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        providerDao.update(updated)
        Result.success(Unit)
    }

    suspend fun deleteDemoProviders() = withContext(Dispatchers.IO) {
        providerDao.deleteDemoProviders()
    }

    suspend fun checkForDuplicates(name: String, phone: String): List<ProviderEntity> = withContext(Dispatchers.IO) {
        providerDao.findDuplicates(name.trim(), phone.trim())
    }

    /**
     * Manual upload of local Room providers to Cloud Firestore (Admin authorized only).
     * Excludes demo records ("Noshid").
     */
    suspend fun uploadLocalProvidersToCloud(): Result<Pair<Int, List<String>>> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            return@withContext Result.failure(SecurityException("Please sign in with your Firebase Administrator account first."))
        }

        val allLocal = providerDao.getAllProvidersAdmin().first()
        val nonDemo = allLocal.filter { !it.isDemoData }

        if (nonDemo.isEmpty()) {
            return@withContext Result.success(Pair(0, listOf("No local non-demo providers to upload.")))
        }

        var successCount = 0
        val errors = mutableListOf<String>()

        for (provider in nonDemo) {
            try {
                val map = providerToMap(provider)
                firestore.collection("providers").document(provider.id).set(map).await()
                successCount++
            } catch (e: Exception) {
                errors.add("Failed to upload '${provider.name}': ${e.localizedMessage}")
            }
        }

        Result.success(Pair(successCount, errors))
    }

    suspend fun importFromCsv(csvText: String): CsvImportResult = withContext(Dispatchers.IO) {
        val parsed = CsvParser.parseProviderCsv(csvText)
        if (parsed.validProviders.isNotEmpty()) {
            providerDao.insertAll(parsed.validProviders)
        }
        parsed
    }

    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val all = providerDao.getAllProvidersAdmin().first()
        CsvParser.exportToCsv(all)
    }

    // Category and Locality management
    suspend fun addCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    suspend fun toggleCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.update(category.copy(isEnabled = !category.isEnabled))
    }

    suspend fun addLocationArea(area: LocationAreaEntity) = withContext(Dispatchers.IO) {
        locationAreaDao.insert(area)
    }

    suspend fun updateLocationArea(area: LocationAreaEntity) = withContext(Dispatchers.IO) {
        locationAreaDao.update(area)
    }

    suspend fun toggleLocationArea(area: LocationAreaEntity) = withContext(Dispatchers.IO) {
        locationAreaDao.update(area.copy(isEnabled = !area.isEnabled))
    }

    /**
     * Map a Room ProviderEntity to a Firestore document Map.
     */
    private fun providerToMap(p: ProviderEntity): Map<String, Any?> {
        return mapOf(
            "id" to p.id,
            "name" to p.name,
            "banglaName" to p.banglaName,
            "primaryCategory" to p.primaryCategory,
            "additionalCategories" to p.additionalCategories,
            "contactNumber" to p.contactNumber,
            "additionalContact" to p.additionalContact,
            "locationArea" to p.locationArea,
            "specificAddress" to p.specificAddress,
            "landmark" to p.landmark,
            "chargeEstimate" to p.chargeEstimate,
            "description" to p.description,
            "whatsappNumber" to p.whatsappNumber,
            "facebookUrl" to p.facebookUrl,
            "mapCoordinates" to p.mapCoordinates,
            "workingHours" to p.workingHours,
            "isVerified" to p.isVerified,
            "isActive" to p.isActive,
            "isDemoData" to p.isDemoData,
            "createdAt" to p.createdAt,
            "updatedAt" to p.updatedAt
        )
    }

    /**
     * Map a Firestore DocumentSnapshot to a Room ProviderEntity safely.
     */
    private fun mapDocumentToProvider(doc: DocumentSnapshot): ProviderEntity? {
        val name = doc.getString("name") ?: return null
        val category = doc.getString("primaryCategory") ?: return null
        val contact = doc.getString("contactNumber") ?: return null
        val location = doc.getString("locationArea") ?: return null

        return ProviderEntity(
            id = doc.getString("id") ?: doc.id,
            name = name,
            banglaName = doc.getString("banglaName") ?: "",
            primaryCategory = category,
            additionalCategories = doc.getString("additionalCategories") ?: "",
            contactNumber = contact,
            additionalContact = doc.getString("additionalContact") ?: "",
            locationArea = location,
            specificAddress = doc.getString("specificAddress") ?: "",
            landmark = doc.getString("landmark") ?: "",
            chargeEstimate = doc.getString("chargeEstimate") ?: "",
            description = doc.getString("description") ?: "",
            whatsappNumber = doc.getString("whatsappNumber") ?: "",
            facebookUrl = doc.getString("facebookUrl") ?: "",
            mapCoordinates = doc.getString("mapCoordinates") ?: "",
            workingHours = doc.getString("workingHours") ?: "",
            isVerified = doc.getBoolean("isVerified") ?: false,
            isActive = doc.getBoolean("isActive") ?: true,
            isDemoData = doc.getBoolean("isDemoData") ?: false,
            createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
