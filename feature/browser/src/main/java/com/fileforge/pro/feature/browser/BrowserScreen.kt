package com.fileforge.pro.feature.browser

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.ViewMode
import com.fileforge.pro.domain.model.WindowType
import com.fileforge.pro.domain.model.WindowPayload
import com.fileforge.pro.feature.browser.components.Breadcrumb
import com.fileforge.pro.feature.browser.components.FileDetailsRow
import com.fileforge.pro.feature.browser.components.FileGridItem
import com.fileforge.pro.feature.browser.components.FileListItem
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialSourceId: String = "internal",
    initialPath: String = "root",
    paneKey: String? = null,
    onPathChanged: ((FPath) -> Unit)? = null,
    onPaneFocused: (() -> Unit)? = null,
    onNavigate: (String) -> Unit = { },
    onOpenInWindow: ((WindowType, FFile) -> Unit)? = null,
    viewModel: BrowserViewModel = if (paneKey != null) hiltViewModel(key = paneKey) else hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialSourceId, initialPath) {
        if (state.currentPath == null) {
            val path = if (initialPath == "root") FPath.root(initialSourceId)
            else FPath.fromString(initialSourceId, initialPath)
            viewModel.loadDirectory(path)
        }
    }

    LaunchedEffect(state.currentPath) {
        state.currentPath?.let { onPathChanged?.invoke(it) }
    }

    // Hardware back navigates folder history instead of leaving app (Master Spec §7)
    BackHandler(enabled = state.canGoBack) {
        viewModel.goBack()
    }

    var showNewMenu by remember { mutableStateOf(false) }
    var showViewModePicker by remember { mutableStateOf(false) }
    val contextMenuState = remember { com.fileforge.pro.feature.browser.components.ContextMenuState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.fileforge.pro.feature.browser.components.SelectionActionBar(
                selectionCount = state.selectedIds.size,
                onClose = viewModel::clearSelection,
                onCopy = viewModel::copySelected,
                onMove = viewModel::cutSelected,
                onDelete = viewModel::deleteSelected,
                onRename = {
                    val selected = viewModel.getSelectedFiles()
                    if (selected.size == 1) viewModel.renameSelected("${selected.first().name}_renamed")
                },
                onShare = {
                    viewModel.getSelectedFiles().forEach { file -> shareFile(context, file) }
                    viewModel.clearSelection()
                },
                onSelectAll = viewModel::selectAll,
                onProperties = {
                    val selected = viewModel.getSelectedFiles()
                    if (selected.size == 1) {
                        val f = selected.first()
                        val encodedPath = URLEncoder.encode(f.path.displayPath, "UTF-8")
                        onNavigate("properties/${f.path.sourceId}/$encodedPath")
                    }
                    viewModel.clearSelection()
                },
            )

            BrowserToolbar(
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                canGoUp = state.canGoUp,
                onBack = viewModel::goBack,
                onForward = viewModel::goForward,
                onUp = viewModel::goUp,
                onRefresh = viewModel::refresh,
                onSearch = { onNavigate("search") },
                onMore = { showViewModePicker = true },
            )

            Breadcrumb(
                path = state.currentPath,
                breadcrumb = state.breadcrumb,
                onSegmentClick = { path -> viewModel.loadDirectory(path) },
            )

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.errorMessage != null -> Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                    state.items.isEmpty() -> Text(
                        text = "Folder is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> FileArea(
                        state = state,
                        onItemClick = { file ->
                            if (file.isDirectory) viewModel.loadDirectory(file.path)
                            else openFile(file, onNavigate)
                        },
                        onItemLongClick = { file -> contextMenuState.show(file) },
                        onViewModeChange = viewModel::setViewMode,
                    )
                }
            }
        }

        if (state.clipboardPaths.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = viewModel::paste,
                icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste") },
                text = { Text(if (state.isCutMode) "Move here" else "Paste here") },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }

        ExtendedFloatingActionButton(
            onClick = { showNewMenu = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = "New") },
            text = { Text("New") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )

        com.fileforge.pro.feature.browser.operations.OperationsPanel(viewModel = hiltViewModel())

        if (showNewMenu) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = { showNewMenu = false }, sheetState = sheetState) {
                NewItemMenu(
                    onNewFolder = {
                        showNewMenu = false
                        state.currentPath?.let { parent -> viewModel.createNewFolder(parent, "New Folder") }
                    },
                    onNewFile = {
                        showNewMenu = false
                        state.currentPath?.let { parent -> viewModel.createNewFile(parent, "New File.txt") }
                    },
                )
            }
        }

        if (showViewModePicker) {
            com.fileforge.pro.feature.browser.components.ViewModePickerDialog(
                currentMode = state.viewMode,
                currentItemSize = com.fileforge.pro.domain.model.ItemSize.DEFAULT,
                onModeSelected = viewModel::setViewMode,
                onItemSizeChanged = { },
                onDismiss = { showViewModePicker = false },
            )
        }

        com.fileforge.pro.feature.browser.components.FileContextMenu(
            expanded = contextMenuState.visible,
            onDismiss = { contextMenuState.dismiss() },
            file = contextMenuState.targetFile,
            onOpen = {
                contextMenuState.targetFile?.let { file ->
                    if (file.isDirectory) viewModel.loadDirectory(file.path)
                    else openFile(file, onNavigate)
                }
                contextMenuState.dismiss()
            },
            onOpenWith = {
                contextMenuState.targetFile?.let { file -> shareFile(context, file) }
                contextMenuState.dismiss()
            },
            onOpenInNewWindow = {
                contextMenuState.targetFile?.let { file ->
                    val winType = if (file.isDirectory) WindowType.FILE_BROWSER
                    else when (file.fileType) {
                        FileType.IMAGE, FileType.VIDEO, FileType.AUDIO -> WindowType.IMAGE_VIEWER
                        FileType.TEXT -> WindowType.TEXT_EDITOR
                        FileType.ARCHIVE -> WindowType.ARCHIVE
                        FileType.PDF -> WindowType.PDF_VIEWER
                        else -> WindowType.PROPERTIES
                    }
                    onOpenInWindow?.invoke(winType, file)
                }
                contextMenuState.dismiss()
            },
            onOpenInNewPane = { contextMenuState.dismiss() },
            onCopy = {
                contextMenuState.targetFile?.let { file ->
                    viewModel.toggleSelection(file)
                    viewModel.copySelected()
                }
                contextMenuState.dismiss()
            },
            onMove = {
                contextMenuState.targetFile?.let { file ->
                    viewModel.toggleSelection(file)
                    viewModel.cutSelected()
                }
                contextMenuState.dismiss()
            },
            onRename = {
                contextMenuState.targetFile?.let { file -> viewModel.renameSelected("${file.name}_renamed") }
                contextMenuState.dismiss()
            },
            onDelete = {
                contextMenuState.targetFile?.let { file ->
                    viewModel.toggleSelection(file)
                    viewModel.deleteSelected()
                }
                contextMenuState.dismiss()
            },
            onCompress = { contextMenuState.dismiss() },
            onShare = {
                contextMenuState.targetFile?.let { file -> shareFile(context, file) }
                contextMenuState.dismiss()
            },
            onAddToFavorites = { contextMenuState.dismiss() },
            onProperties = {
                contextMenuState.targetFile?.let { file ->
                    val encodedPath = URLEncoder.encode(file.path.displayPath, "UTF-8")
                    onNavigate("properties/${file.path.sourceId}/$encodedPath")
                }
                contextMenuState.dismiss()
            },
        )
    }
}

private fun openFile(file: FFile, onNavigate: (String) -> Unit) {
    val encodedPath = URLEncoder.encode(file.path.displayPath, "UTF-8")
    val route = when (file.fileType) {
        FileType.IMAGE, FileType.VIDEO, FileType.AUDIO -> "media/${file.path.sourceId}/$encodedPath"
        FileType.TEXT -> "texteditor/${file.path.sourceId}/$encodedPath"
        FileType.ARCHIVE -> "archive/${file.path.sourceId}/$encodedPath"
        FileType.APK -> "apk/${file.path.sourceId}/$encodedPath"
        FileType.PDF -> "media/${file.path.sourceId}/$encodedPath"
        FileType.FOLDER -> return
        FileType.OTHER -> "properties/${file.path.sourceId}/$encodedPath"
    }
    onNavigate(route)
}

private fun shareFile(context: android.content.Context, file: FFile) {
    val realPath = when (file.path.sourceId) {
        "internal" -> "/storage/emulated/0/${file.path.displayPath}".trimEnd('/')
        else -> {
            val volId = file.path.sourceId.substringAfter('-')
            "/storage/$volId/${file.path.displayPath}".trimEnd('/')
        }
    }
    val realFile = java.io.File(realPath)
    if (!realFile.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", realFile)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = file.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
    } catch (e: Exception) {
        // FileProvider not available or file not accessible
    }
}

@Composable
private fun BrowserToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    canGoUp: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
        IconButton(onClick = onForward, enabled = canGoForward) { Icon(Icons.Outlined.ArrowForward, contentDescription = "Forward") }
        IconButton(onClick = onUp, enabled = canGoUp) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "Up") }
        IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh") }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMore) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
    }
}

@Composable
private fun FileArea(
    state: BrowserUiState,
    onItemClick: (FFile) -> Unit,
    onItemLongClick: (FFile) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
) {
    when (state.viewMode) {
        ViewMode.LARGE_GRID, ViewMode.MEDIUM_GRID, ViewMode.SMALL_GRID, ViewMode.THUMBNAIL -> {
            val cols = when (state.viewMode) {
                ViewMode.LARGE_GRID -> 2
                ViewMode.MEDIUM_GRID -> 3
                ViewMode.SMALL_GRID -> 4
                ViewMode.THUMBNAIL -> 3
                else -> 3
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(state.items, key = { it.path.displayPath }) { file ->
                    FileGridItem(
                        file = file,
                        isSelected = file.path.displayPath in state.selectedIds,
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) },
                    )
                }
            }
        }
        ViewMode.LIST, ViewMode.COMPACT_LIST -> {
            val isCompact = state.viewMode == ViewMode.COMPACT_LIST
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.items, key = { it.path.displayPath }) { file ->
                    FileListItem(
                        file = file,
                        isSelected = file.path.displayPath in state.selectedIds,
                        isCompact = isCompact,
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) },
                    )
                }
            }
        }
        ViewMode.DETAILS -> {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.items, key = { it.path.displayPath }) { file ->
                    FileDetailsRow(
                        file = file,
                        isSelected = file.path.displayPath in state.selectedIds,
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewItemMenu(onNewFolder: () -> Unit, onNewFile: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onNewFolder() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
            Text("New Folder", style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onNewFile() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.NoteAdd, contentDescription = null)
            Text("New File", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
