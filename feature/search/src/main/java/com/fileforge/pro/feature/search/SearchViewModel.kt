package com.fileforge.pro.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileFilter
import com.fileforge.pro.domain.repository.SearchHistoryRepository
import com.fileforge.pro.domain.repository.SearchRepository
import com.fileforge.pro.domain.repository.SearchResult
import com.fileforge.pro.domain.repository.StorageSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<FFile> = emptyList(),
    val scannedPaths: Int = 0,
    val elapsedMs: Long = 0,
    val isComplete: Boolean = false,
    val searchScope: FPath? = null,
    val history: List<String> = emptyList(),
    val suggestions: List<String> = listOf(
        "*.pdf", "*.mp4", "*.jpg", "*.zip",
        "name:project", "type:image", "size>500MB",
        "modified:today", "modified:this_week",
    ),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepo: SearchRepository,
    private val historyRepo: SearchHistoryRepository,
    private val storageSourceRepo: StorageSourceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Default scope = first available storage source
        viewModelScope.launch(dispatchers.io) {
            val sources = storageSourceRepo.getSources()
            val firstSource = sources.firstOrNull()
            if (firstSource != null) {
                _uiState.update { it.copy(searchScope = FPath.root(firstSource.id)) }
            }
        }
        // Subscribe to search history
        viewModelScope.launch(dispatchers.io) {
            historyRepo.observe(20).collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun setScope(path: FPath) {
        _uiState.update { it.copy(searchScope = path) }
    }

    fun performSearch() {
        val state = _uiState.value
        val query = state.query.trim()
        val scope = state.searchScope ?: return
        if (query.isEmpty()) return

        searchJob?.cancel()
        _uiState.update {
            it.copy(isSearching = true, results = emptyList(), isComplete = false, scannedPaths = 0)
        }

        // Persist in history
        viewModelScope.launch(dispatchers.io) {
            historyRepo.add(query)
        }

        searchJob = viewModelScope.launch(dispatchers.io) {
            searchRepo.search(query, scope, FileFilter.EMPTY).collect { result: SearchResult ->
                _uiState.update {
                    it.copy(
                        results = result.items,
                        scannedPaths = result.scannedPaths,
                        elapsedMs = result.elapsedMs,
                        isComplete = result.isComplete,
                        isSearching = !result.isComplete,
                    )
                }
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        viewModelScope.launch(dispatchers.io) {
            searchRepo.cancel()
        }
        _uiState.update { it.copy(isSearching = false, isComplete = true) }
    }

    fun clearHistory() {
        viewModelScope.launch(dispatchers.io) {
            historyRepo.clear()
        }
    }

    fun useSuggestion(suggestion: String) {
        _uiState.update { it.copy(query = suggestion) }
        performSearch()
    }
}
