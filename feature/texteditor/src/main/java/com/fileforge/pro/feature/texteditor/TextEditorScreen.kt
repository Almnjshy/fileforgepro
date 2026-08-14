package com.fileforge.pro.feature.texteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.engine.text.SyntaxHighlighter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    file: FFile,
    viewModel: TextEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                        if (state.isDirty) Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        else if (state.isReadOnly) Text("Read only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.isDirty) { Icon(Icons.Outlined.Undo, contentDescription = "Undo") }
                    IconButton(onClick = viewModel::redo, enabled = state.isDirty) { Icon(Icons.Outlined.Redo, contentDescription = "Redo") }
                    IconButton(onClick = viewModel::openFind) { Icon(Icons.Outlined.FindReplace, contentDescription = "Find/Replace") }
                    IconButton(onClick = viewModel::save, enabled = state.canSave) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        else Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            state.errorMessage?.let { msg ->
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            if (state.isFindOpen) {
                FindReplaceBar(
                    findQuery = state.findQuery,
                    replaceQuery = state.replaceQuery,
                    onFindChange = viewModel::setFindQuery,
                    onReplaceChange = viewModel::setReplaceQuery,
                    onReplaceAll = viewModel::replaceAll,
                    onClose = viewModel::closeFind,
                )
            }
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
                val highlighted = SyntaxHighlighter.highlight(state.editedText, state.language)
                Text(
                    text = highlighted,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.4,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FindReplaceBar(
    findQuery: String,
    replaceQuery: String,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(value = findQuery, onValueChange = onFindChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Find…") }, singleLine = true)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = replaceQuery, onValueChange = onReplaceChange, modifier = Modifier.weight(1f), placeholder = { Text("Replace with…") }, singleLine = true)
            IconButton(onClick = onReplaceAll) { Icon(Icons.Outlined.FindReplace, contentDescription = "Replace all") }
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Undo, contentDescription = "Close find bar") }
        }
    }
}
