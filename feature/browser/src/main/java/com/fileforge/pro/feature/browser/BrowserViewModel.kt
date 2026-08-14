package com.fileforge.pro.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileFilter
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.SortDirection
import com.fileforge.pro.domain.model.SortField
import com.fileforge.pro.domain.model.ViewMode
import com.fileforge.pro.domain.repository.FileOperationRepository
import com.fileforge.pro.domain.repository.FileRepository
import com.fileforge.pro.domain.repository.FileTypeRegistry
import com.fileforge.pro.domain.repository.ViewSettingsRepository
import com.fileforge.pro.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUiState(
    val currentPath: FPath? = null,
    val items: List<FFile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val viewMode: ViewMode = ViewMode.DEFAULT_PHONE,
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
    val showHidden: Boolean = false,
    val filter: FileFilter = FileFilter.EMPTY,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val breadcrumb: List<FPath> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val canGoUp: Boolean = false,
    val clipboardPaths: List<FPath> = emptyList(),
    val isCutMode: Boolean = false,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val fileRepo: FileRepository,
    private val viewSettingsRepo: ViewSettingsRepository,
    private val fileTypeRegistry: FileTypeRegistry,
    private val operationRepo: FileOperationRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val backStack = ArrayDeque<FPath>()
    private val forwardStack = ArrayDeque<FPath>()
    private var clipboard: List<FPath> = emptyList()
    private var isCutMode: Boolean = false

    init {
        viewModelScope.launch(dispatchers.io) {
            viewSettingsRepo.observeGlobal().collect { vs ->
                _uiState.update {
                    it.copy(
                        viewMode = vs.viewMode,
                        sortField = vs.sortField,
                        sortDirection = vs.sortDirection,
                        showHidden = vs.showHidden,
                    )
                }
                applySort()
            }
        }
    }

    fun loadDirectory(path: FPath, pushHistory: Boolean = true) {
        viewModelScope.launch(dispatchers.io) {
            if (pushHistory) {
                _uiState.value.currentPath?.let { backStack.addLast(it) }
                forwardStack.clear()
            }
            _uiState.update { it.copy(currentPath = path, isLoading = true, errorMessage = null) }
            when (val result = fileRepo.listDirectory(path)) {
                is Result.Ok -> {
                    val items = result.value.map { f -> f.copy(fileType = fileTypeRegistry.detect(f)) }
                    _uiState.update {
                        it.copy(
                            items = items,
                            isLoading = false,
                            breadcrumb = buildBreadcrumb(path),
                            canGoBack = backStack.isNotEmpty(),
                            canGoForward = forwardStack.isNotEmpty(),
                            canGoUp = !path.isRoot,
                        )
                    }
                    applySort()
                }
                is Result.Err -> {
                    Logger.e(LogTags.STORAGE, "loadDirectory failed: ${result.error.code}", result.error.cause)
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun goBack() {
        val current = _uiState.value.currentPath ?: return
        if (backStack.isEmpty()) return
        val previous = backStack.removeLast()
        forwardStack.addLast(current)
        loadDirectory(previous, pushHistory = false)
    }

    fun goForward() {
        if (forwardStack.isEmpty()) return
        val current = _uiState.value.currentPath ?: return
        val next = forwardStack.removeLast()
        backStack.addLast(current)
        loadDirectory(next, pushHistory = false)
    }

    fun goUp() {
        val current = _uiState.value.currentPath ?: return
        current.parent?.let { loadDirectory(it) }
    }

    fun refresh() {
        _uiState.value.currentPath?.let { loadDirectory(it, pushHistory = false) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        viewModelScope.launch(dispatchers.io) { viewSettingsRepo.setGlobalMode(mode) }
    }

    fun setSortField(field: SortField) {
        _uiState.update { it.copy(sortField = field) }
        applySort()
    }

    fun toggleSortDirection() {
        _uiState.update {
            it.copy(sortDirection = if (it.sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC)
        }
        applySort()
    }

    fun toggleSelection(file: FFile) {
        val id = file.path.displayPath
        _uiState.update { state ->
            val newSelection = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSelection, isSelectionMode = newSelection.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        val all = _uiState.value.items.map { it.path.displayPath }.toSet()
        _uiState.update { it.copy(selectedIds = all, isSelectionMode = all.isNotEmpty()) }
    }

    fun getSelectedFiles(): List<FFile> {
        val selected = _uiState.value.selectedIds
        return _uiState.value.items.filter { it.path.displayPath in selected }
    }

    fun copySelected() {
        val selected = getSelectedFiles().map { it.path }
        if (selected.isEmpty()) return
        clipboard = selected
        isCutMode = false
        _uiState.update { it.copy(clipboardPaths = clipboard, isCutMode = false) }
        clearSelection()
    }

    fun cutSelected() {
        val selected = getSelectedFiles().map { it.path }
        if (selected.isEmpty()) return
        clipboard = selected
        isCutMode = true
        _uiState.update { it.copy(clipboardPaths = clipboard, isCutMode = true) }
        clearSelection()
    }

    fun paste() {
        val dest = _uiState.value.currentPath ?: return
        if (clipboard.isEmpty()) return
        val sources = clipboard
        viewModelScope.launch(dispatchers.io) {
            if (isCutMode) operationRepo.enqueueMove(sources, dest)
            else operationRepo.enqueueCopy(sources, dest)
            if (isCutMode) {
                clipboard = emptyList()
                isCutMode = false
                _uiState.update { it.copy(clipboardPaths = emptyList(), isCutMode = false) }
            }
            kotlinx.coroutines.delay(300)
            refresh()
        }
    }

    fun clearClipboard() {
        clipboard = emptyList()
        isCutMode = false
        _uiState.update { it.copy(clipboardPaths = emptyList(), isCutMode = false) }
    }

    fun deleteSelected() {
        val selected = getSelectedFiles().map { it.path }
        if (selected.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueDelete(selected)
            clearSelection()
            kotlinx.coroutines.delay(300)
            refresh()
        }
    }

    fun renameSelected(newName: String) {
        val selected = getSelectedFiles()
        if (selected.size != 1) return
        val path = selected.first().path
        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueRename(path, newName)
            clearSelection()
            kotlinx.coroutines.delay(200)
            refresh()
        }
    }

    fun createNewFolder(parent: FPath, name: String) {
        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueCreateFolder(parent, name)
            kotlinx.coroutines.delay(200)
            refresh()
        }
    }

    fun createNewFile(parent: FPath, name: String) {
        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueCreateFile(parent, name)
            kotlinx.coroutines.delay(200)
            refresh()
        }
    }

    private fun applySort() {
        val state = _uiState.value
        val sorted = state.items.sortedWith(comparator(state.sortField, state.sortDirection))
        val filtered = applyFilter(sorted, state.filter, state.showHidden)
        _uiState.update { it.copy(items = filtered) }
    }

    private fun applyFilter(items: List<FFile>, filter: FileFilter, showHidden: Boolean): List<FFile> {
        var result = items
        if (!showHidden) result = result.filterNot { it.isHidden }
        if (filter.isEmpty) return result
        filter.fileTypes?.let { types ->
            result = result.filter { it.fileType in types || (it.isDirectory && FileType.FOLDER in types) }
        }
        filter.extensions?.let { exts ->
            val set = exts.map { it.lowercase().trimStart('.') }.toSet()
            result = result.filter { it.extension?.lowercase() in set }
        }
        filter.minSize?.let { min -> result = result.filter { it.isDirectory || it.size >= min } }
        filter.maxSize?.let { max -> result = result.filter { it.isDirectory || it.size <= max } }
        filter.modifiedAfter?.let { after -> result = result.filter { it.lastModified >= after } }
        filter.modifiedBefore?.let { before -> result = result.filter { it.lastModified <= before } }
        filter.searchQuery?.let { q ->
            val lower = q.lowercase()
            result = result.filter { it.name.lowercase().contains(lower) }
        }
        return result
    }

    private fun comparator(field: SortField, direction: SortDirection): Comparator<FFile> {
        val base: Comparator<FFile> = when (field) {
            SortField.NAME -> compareBy { it.name.lowercase() }
            SortField.SIZE -> compareBy { it.size }
            SortField.TYPE -> compareBy { it.fileType.name }
            SortField.MODIFIED -> compareBy { it.lastModified }
            SortField.CREATED -> compareBy { it.created ?: it.lastModified }
            SortField.ACCESSED -> compareBy { it.lastAccessed ?: it.lastModified }
        }
        val folderFirst: Comparator<FFile> = compareBy { !it.isDirectory }
        val final = folderFirst.then(base)
        return if (direction == SortDirection.ASC) final else final.reversed()
    }

    private fun buildBreadcrumb(path: FPath): List<FPath> {
        val result = mutableListOf<FPath>()
        var current: FPath? = path
        while (current != null) {
            result.add(0, current)
            current = current.parent
        }
        return result
    }
}
