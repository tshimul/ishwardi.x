package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.LocationAreaEntity
import com.example.data.model.ProviderEntity
import com.example.data.model.SyncStatus
import com.example.data.repository.AdminRepository
import com.example.data.repository.ProviderRepository
import com.example.util.CsvImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AdminTab {
    PROVIDERS,
    ADD_EDIT,
    CSV_SYNC,
    CATEGORIES_LOCALITIES
}

data class ProviderFormState(
    val id: String = "",
    val name: String = "",
    val banglaName: String = "",
    val primaryCategory: String = "",
    val additionalCategories: String = "",
    val contactNumber: String = "",
    val additionalContact: String = "",
    val locationArea: String = "",
    val specificAddress: String = "",
    val landmark: String = "",
    val chargeEstimate: String = "",
    val description: String = "",
    val whatsappNumber: String = "",
    val facebookUrl: String = "",
    val mapCoordinates: String = "",
    val workingHours: String = "",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val isEditing: Boolean = false
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    val isAuthenticated: StateFlow<Boolean> = adminRepository.isAuthenticated
    val adminIdentifier: StateFlow<String?> = adminRepository.adminIdentifier
    val adminUid: StateFlow<String?> = adminRepository.adminUid
    val isAllowlistVerified: StateFlow<Boolean> = adminRepository.isAllowlistVerified
    val authMethod: StateFlow<String> = adminRepository.authMethod
    val isFirstTimeSetup: StateFlow<Boolean> = adminRepository.isFirstTimeSetup

    val allProviders: StateFlow<List<ProviderEntity>> = providerRepository.allAdminProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = providerRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLocationAreas: StateFlow<List<LocationAreaEntity>> = providerRepository.allLocationAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncStatus: StateFlow<SyncStatus> = providerRepository.syncStatus

    private val _selectedTab = MutableStateFlow(AdminTab.PROVIDERS)
    val selectedTab: StateFlow<AdminTab> = _selectedTab.asStateFlow()

    private val _formState = MutableStateFlow(ProviderFormState())
    val formState: StateFlow<ProviderFormState> = _formState.asStateFlow()

    private val _duplicateWarnings = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val duplicateWarnings: StateFlow<List<ProviderEntity>> = _duplicateWarnings.asStateFlow()

    private val _csvImportPreview = MutableStateFlow<CsvImportResult?>(null)
    val csvImportPreview: StateFlow<CsvImportResult?> = _csvImportPreview.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _isUploadingCloud = MutableStateFlow(false)
    val isUploadingCloud: StateFlow<Boolean> = _isUploadingCloud.asStateFlow()

    fun setSelectedTab(tab: AdminTab) {
        _selectedTab.value = tab
    }

    fun setAdminSearchQuery(q: String) {
        _adminSearchQuery.value = q
    }

    fun loginWithEmail(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.loginWithEmail(email, pass)
            result.onSuccess { msg ->
                // Switch repository to admin sync mode so all records (including drafts) sync
                providerRepository.startRealtimeSync(isAdmin = true)
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Authentication failed")
            }
        }
    }

    fun loginWithPasscode(passcode: String, onResult: (Boolean, String) -> Unit) {
        val result = adminRepository.loginWithPasscode(passcode)
        result.onSuccess { msg ->
            onResult(true, msg)
        }.onFailure { err ->
            onResult(false, err.message ?: "Authentication failed")
        }
    }

    fun logout() {
        adminRepository.logout()
        // Switch back to public sync mode
        providerRepository.startRealtimeSync(isAdmin = false)
    }

    fun initNewProviderForm() {
        _formState.value = ProviderFormState(id = UUID.randomUUID().toString(), isEditing = false)
        _duplicateWarnings.value = emptyList()
        _selectedTab.value = AdminTab.ADD_EDIT
    }

    fun editProvider(provider: ProviderEntity) {
        _formState.value = ProviderFormState(
            id = provider.id,
            name = provider.name,
            banglaName = provider.banglaName,
            primaryCategory = provider.primaryCategory,
            additionalCategories = provider.additionalCategories,
            contactNumber = provider.contactNumber,
            additionalContact = provider.additionalContact,
            locationArea = provider.locationArea,
            specificAddress = provider.specificAddress,
            landmark = provider.landmark,
            chargeEstimate = provider.chargeEstimate,
            description = provider.description,
            whatsappNumber = provider.whatsappNumber,
            facebookUrl = provider.facebookUrl,
            mapCoordinates = provider.mapCoordinates,
            workingHours = provider.workingHours,
            isVerified = provider.isVerified,
            isActive = provider.isActive,
            isEditing = true
        )
        _duplicateWarnings.value = emptyList()
        _selectedTab.value = AdminTab.ADD_EDIT
    }

    fun updateFormField(updater: (ProviderFormState) -> ProviderFormState) {
        val updated = updater(_formState.value)
        _formState.value = updated

        if (updated.contactNumber.length >= 5 || updated.name.length >= 3) {
            viewModelScope.launch {
                val dupes = providerRepository.checkForDuplicates(updated.name, updated.contactNumber)
                    .filter { it.id != updated.id }
                _duplicateWarnings.value = dupes
            }
        } else {
            _duplicateWarnings.value = emptyList()
        }
    }

    fun saveProvider(onComplete: (Boolean, String) -> Unit) {
        val state = _formState.value
        if (state.name.isBlank()) {
            onComplete(false, "Provider name is required")
            return
        }
        if (state.primaryCategory.isBlank()) {
            onComplete(false, "Primary category is required")
            return
        }
        if (state.contactNumber.isBlank()) {
            onComplete(false, "Contact phone number is required")
            return
        }
        if (state.locationArea.isBlank()) {
            onComplete(false, "Location or service area is required")
            return
        }

        val provider = ProviderEntity(
            id = if (state.id.isBlank()) UUID.randomUUID().toString() else state.id,
            name = state.name.trim(),
            banglaName = state.banglaName.trim(),
            primaryCategory = state.primaryCategory.trim(),
            additionalCategories = state.additionalCategories.trim(),
            contactNumber = state.contactNumber.trim(),
            additionalContact = state.additionalContact.trim(),
            locationArea = state.locationArea.trim(),
            specificAddress = state.specificAddress.trim(),
            landmark = state.landmark.trim(),
            chargeEstimate = state.chargeEstimate.trim(),
            description = state.description.trim(),
            whatsappNumber = state.whatsappNumber.trim(),
            facebookUrl = state.facebookUrl.trim(),
            mapCoordinates = state.mapCoordinates.trim(),
            workingHours = state.workingHours.trim(),
            isVerified = state.isVerified,
            isActive = state.isActive,
            isDemoData = false,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = if (state.isEditing) {
                providerRepository.updateProvider(provider)
            } else {
                providerRepository.addProvider(provider)
            }

            result.onSuccess {
                _selectedTab.value = AdminTab.PROVIDERS
                onComplete(true, "Provider saved successfully to Cloud & Local cache.")
            }.onFailure { err ->
                onComplete(false, "Failed to save: ${err.localizedMessage}")
            }
        }
    }

    fun deleteProvider(providerId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = providerRepository.deleteProvider(providerId)
            res.onSuccess {
                onComplete(true, "Provider deleted.")
            }.onFailure { err ->
                onComplete(false, "Delete failed: ${err.localizedMessage}")
            }
        }
    }

    fun toggleProviderStatus(providerId: String, isActive: Boolean, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = providerRepository.toggleActiveStatus(providerId, isActive)
            res.onSuccess {
                onComplete(true, "Status updated to ${if (isActive) "Active" else "Inactive"}")
            }.onFailure { err ->
                onComplete(false, "Failed to update status: ${err.localizedMessage}")
            }
        }
    }

    fun toggleVerifiedStatus(providerId: String, isVerified: Boolean, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = providerRepository.toggleVerifiedStatus(providerId, isVerified)
            res.onSuccess {
                onComplete(true, "Status updated to ${if (isVerified) "Verified" else "Unverified"}")
            }.onFailure { err ->
                onComplete(false, "Failed to update verification: ${err.localizedMessage}")
            }
        }
    }

    fun deleteDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            providerRepository.deleteDemoProviders()
            onComplete()
        }
    }

    /**
     * Manual upload of existing local contacts into Firestore (Admin only)
     */
    fun uploadLocalContacts(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isUploadingCloud.value = true
            val res = providerRepository.uploadLocalProvidersToCloud()
            _isUploadingCloud.value = false
            res.onSuccess { (count, errors) ->
                if (errors.isEmpty()) {
                    onComplete(true, "Successfully uploaded $count provider(s) to Firestore.")
                } else {
                    onComplete(true, "Uploaded $count provider(s). Errors: ${errors.joinToString("; ")}")
                }
            }.onFailure { err ->
                onComplete(false, "Upload failed: ${err.localizedMessage}")
            }
        }
    }

    // CSV Handling
    fun previewCsvImport(csvContent: String) {
        viewModelScope.launch {
            val preview = providerRepository.importFromCsv(csvContent)
            _csvImportPreview.value = preview
        }
    }

    fun clearCsvPreview() {
        _csvImportPreview.value = null
    }

    fun exportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = providerRepository.exportToCsv()
            onResult(csv)
        }
    }

    // Category and Locality management
    fun addCategory(name: String, banglaName: String, iconKey: String) {
        val id = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        viewModelScope.launch {
            providerRepository.addCategory(
                CategoryEntity(
                    id = id,
                    name = name.trim(),
                    banglaName = banglaName.trim(),
                    iconKey = iconKey,
                    isCustom = true
                )
            )
        }
    }

    fun toggleCategory(cat: CategoryEntity) {
        viewModelScope.launch {
            providerRepository.toggleCategory(cat)
        }
    }

    fun addLocation(name: String, banglaName: String) {
        val id = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        viewModelScope.launch {
            providerRepository.addLocationArea(
                LocationAreaEntity(
                    id = id,
                    name = name.trim(),
                    banglaName = banglaName.trim()
                )
            )
        }
    }

    fun toggleLocation(loc: LocationAreaEntity) {
        viewModelScope.launch {
            providerRepository.toggleLocationArea(loc)
        }
    }

    fun triggerSync() {
        providerRepository.startRealtimeSync(isAdmin = adminRepository.isCloudAdmin())
    }
}
