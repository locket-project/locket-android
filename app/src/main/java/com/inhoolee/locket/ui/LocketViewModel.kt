package com.inhoolee.locket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inhoolee.locket.data.AuthRepository
import com.inhoolee.locket.data.ChecklistDraftPayload
import com.inhoolee.locket.data.NoteDraftPayload
import com.inhoolee.locket.data.NotesRepository
import com.inhoolee.locket.data.SupabaseConfig
import com.inhoolee.locket.data.toDraftPayload
import com.inhoolee.locket.domain.LocketNote
import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import com.inhoolee.locket.domain.Workspace
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocketUiState(
    val isConfigured: Boolean = true,
    val isCheckingSession: Boolean = true,
    val isSignedIn: Boolean = false,
    val userEmail: String = "",
    val notes: List<LocketNote> = emptyList(),
    val workspace: Workspace = Workspace.Notes,
    val searchText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editorDraft: EditorDraft? = null
)

data class EditorDraft(
    val noteId: String? = null,
    val title: String = "",
    val body: String = "",
    val kind: NoteKind = NoteKind.Text,
    val color: NoteColor = NoteColor.Default,
    val labelsCsv: String = "",
    val checklistItems: List<ChecklistEditorItem> = emptyList()
) {
    fun toPayload(): NoteDraftPayload =
        NoteDraftPayload(
            title = title.trim(),
            body = body.trim(),
            kind = kind,
            color = color,
            labelNames = labelsCsv.split(","),
            checklistItems = checklistItems.map {
                ChecklistDraftPayload(
                    id = it.id,
                    content = it.content,
                    isChecked = it.isChecked
                )
            }
        )
}

data class ChecklistEditorItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val isChecked: Boolean = false
)

class LocketViewModel(
    private val config: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LocketUiState(
            isConfigured = config.isConfigured,
            isCheckingSession = config.isConfigured,
            errorMessage = if (config.isConfigured) null else "Set SUPABASE_URL and SUPABASE_ANON_KEY in android/local.properties."
        )
    )
    val uiState: StateFlow<LocketUiState> = _uiState

    init {
        if (config.isConfigured) {
            restoreSession()
        }
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter email and password.") }
            return
        }
        viewModelScope.launch {
            runLoading {
                val session = authRepository.signIn(trimmedEmail, password)
                _uiState.update {
                    it.copy(isSignedIn = true, userEmail = session.email.orEmpty(), errorMessage = null)
                }
                loadNotes()
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                LocketUiState(isConfigured = config.isConfigured, isCheckingSession = false)
            }
        }
    }

    fun refreshNotes() {
        viewModelScope.launch {
            runLoading {
                loadNotes()
            }
        }
    }

    fun setWorkspace(workspace: Workspace) {
        _uiState.update { it.copy(workspace = workspace) }
        refreshNotes()
    }

    fun setSearchText(value: String) {
        _uiState.update { it.copy(searchText = value) }
        refreshNotes()
    }

    fun openNewNote(kind: NoteKind) {
        _uiState.update {
            it.copy(
                editorDraft = EditorDraft(
                    kind = kind,
                    checklistItems = if (kind == NoteKind.Checklist) listOf(ChecklistEditorItem()) else emptyList()
                )
            )
        }
    }

    fun openEditor(note: LocketNote) {
        _uiState.update {
            it.copy(
                editorDraft = EditorDraft(
                    noteId = note.id,
                    title = note.title,
                    body = note.body,
                    kind = note.kind,
                    color = note.color,
                    labelsCsv = note.labels.joinToString(", ") { label -> label.name },
                    checklistItems = note.checklistItems.map { item ->
                        val draft = item.toDraftPayload()
                        ChecklistEditorItem(id = draft.id, content = draft.content, isChecked = draft.isChecked)
                    }.ifEmpty {
                        if (note.kind == NoteKind.Checklist) listOf(ChecklistEditorItem()) else emptyList()
                    }
                )
            )
        }
    }

    fun updateDraft(editorDraft: EditorDraft) {
        _uiState.update { it.copy(editorDraft = editorDraft) }
    }

    fun closeEditor() {
        _uiState.update { it.copy(editorDraft = null) }
    }

    fun saveEditor() {
        val draft = _uiState.value.editorDraft ?: return
        viewModelScope.launch {
            runLoading {
                notesRepository.saveNote(draft.noteId, draft.toPayload())
                _uiState.update { it.copy(editorDraft = null, errorMessage = null) }
                loadNotes()
            }
        }
    }

    fun deleteNote(note: LocketNote) {
        viewModelScope.launch {
            runLoading {
                notesRepository.deleteNote(note)
                loadNotes()
            }
        }
    }

    fun togglePin(note: LocketNote) {
        viewModelScope.launch {
            runLoading {
                notesRepository.togglePin(note)
                loadNotes()
            }
        }
    }

    fun toggleArchive(note: LocketNote) {
        viewModelScope.launch {
            runLoading {
                notesRepository.toggleArchive(note)
                loadNotes()
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            runCatching {
                authRepository.restoreSession()
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isSignedIn = session != null,
                        userEmail = session?.email.orEmpty(),
                        errorMessage = null
                    )
                }
                if (session != null) {
                    loadNotes()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isSignedIn = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    private suspend fun runLoading(block: suspend () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { block() }
            .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun loadNotes() {
        val state = _uiState.value
        val notes = notesRepository.listNotes(state.workspace, state.searchText)
        _uiState.update { it.copy(notes = notes, errorMessage = null) }
    }
}
