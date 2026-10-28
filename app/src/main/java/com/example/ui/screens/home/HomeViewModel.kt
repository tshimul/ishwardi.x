package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryWithCount
import com.example.data.model.LocationAreaEntity
import com.example.data.model.ProviderEntity
import com.example.data.model.SyncStatus
import com.example.data.repository.ProviderRepository
import com.example.data.repository.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeFilterState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedLocation: String? = null,
    val sortOption: SortOption = SortOption.VERIFIED_FIRST
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: ProviderRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(HomeFilterState())
    val filterState: StateFlow<HomeFilterState> = _filterState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    val totalActiveCount: StateFlow<Int> = repository.activeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pinnedProviders: StateFlow<List<ProviderEntity>> = repository.pinnedProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesWithCounts: StateFlow<List<CategoryWithCount>> = repository.categoriesWithCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationAreas: StateFlow<List<LocationAreaEntity>> = repository.enabledLocationAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repository.pinnedProviders
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredProviders: StateFlow<List<ProviderEntity>> = _filterState
        .flatMapLatest { filter ->
            repository.filterProviders(
                searchQuery = filter.searchQuery,
                selectedCategory = filter.selectedCategory,
                selectedLocation = filter.selectedLocation,
                sortOption = filter.sortOption
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setSelectedCategory(category: String?) {
        val current = _filterState.value.selectedCategory
        _filterState.value = _filterState.value.copy(
            selectedCategory = if (current == category) null else category
        )
    }

    fun setSelectedLocation(location: String?) {
        val current = _filterState.value.selectedLocation
        _filterState.value = _filterState.value.copy(
            selectedLocation = if (current == location) null else location
        )
    }

    fun setSortOption(sortOption: SortOption) {
        _filterState.value = _filterState.value.copy(sortOption = sortOption)
    }

    fun clearAllFilters() {
        _filterState.value = HomeFilterState()
    }

    fun toggleFavorite(providerId: String, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(providerId)
            result.onSuccess { isPinned ->
                if (isPinned) {
                    onMessage("Provider pinned to favorites")
                } else {
                    onMessage("Provider unpinned from favorites")
                }
            }.onFailure { error ->
                onMessage(error.message ?: "Could not update favorites")
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncDirectory()
        }
    }
}
