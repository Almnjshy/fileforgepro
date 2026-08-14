package com.fileforge.pro.feature.texteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.engine.text.SyntaxLanguage
import com.fileforge.pro.engine.text.TextEditorEngine
import com.fileforge.pro.engine.text.TextFileContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextEditorUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val file: FFile? = null,
    val content: TextFileContent? = null,
    val editedText: String = "",
    val language: SyntaxLanguage = SyntaxLanguage.PLAIN_TEXT,
    val errorMessage: String? = null,
    val isReadOnly: Boolean = false,
    val isDirty: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val isFindOpen: Boolean = false,
) {
    val canSave: Boolean get() = isDirty && !isReadOnly && !isSaving
}

@HiltViewModel
class TextEditorViewModel @Inject constructor(
    private val engine: TextEditorEngine,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextEditorUiState())
    val uiState: StateFlow<TextEditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    fun load(file: FFile) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    file = file,
                    language = com.fileforge.pro.engine.text.SyntaxLanguage.fromFile(file),
                    errorMessage = null,
                )
            }
            when (val r = engine.load(file)) {
                is Result.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            content = r.value,
                            editedText = r.value.text,
                            isDirty = false,
                            errorMessage = r.value.truncatedMessage,
                        )
                    }
                }
                is Result.Err -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = r.error.message)
                    }
                }
            }
        }
    }

    fun updateText(newText: String) {
        if (newText == _uiState.value.editedText) return
        undoStack.addLast(_uiState.value.editedText)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        _uiState.update { it.copy(editedText = newText, isDirty = true) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.editedText
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        _uiState.update { it.copy(editedText = previous, isDirty = previous != _uiState.value.content?.text) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.editedText
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _uiState.update { it.copy(editedText = next, isDirty = next != _uiState.value.content?.text) }
    }

    fun save() {
        val state = _uiState.value
        val file = state.file ?: return
        if (!state.canSave) return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSaving = true) }
            when (val r = engine.save(file, state.editedText)) {
                is Result.Ok -> _uiState.update {
                    it.copy(isSaving = false, isDirty = false, content = it.content?.copy(text = state.editedText))
                }
                is Result.Err -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = r.error.message)
                }
            }
        }
    }

    fun saveAs(newFile: FFile) {
        val state = _uiState.value
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSaving = true) }
            when (val r = engine.save(newFile, state.editedText)) {
                is Result.Ok -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        isDirty = false,
                        file = newFile,
                        content = it.content?.copy(path = newFile.path, text = state.editedText),
                    )
                }
                is Result.Err -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = r.error.message)
                }
            }
        }
    }

    fun openFind() { _uiState.update { it.copy(isFindOpen = true) } }
    fun closeFind() { _uiState.update { it.copy(isFindOpen = false, findQuery = "", replaceQuery = "") } }
    fun setFindQuery(q: String) { _uiState.update { it.copy(findQuery = q) } }
    fun setReplaceQuery(q: String) { _uiState.update { it.copy(replaceQuery = q) } }

    fun replaceAll() {
        val state = _uiState.value
        if (state.findQuery.isEmpty()) return
        val newText = state.editedText.replace(state.findQuery, state.replaceQuery)
        updateText(newText)
    }
}
