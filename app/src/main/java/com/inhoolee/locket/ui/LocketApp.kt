package com.inhoolee.locket.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LocketApp(viewModel: LocketViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editorDraft = state.editorDraft
    LocketTheme(themeMode = state.themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !state.isConfigured -> CenterMessage(state.errorMessage ?: "Supabase is not configured.")
                state.isCheckingSession -> LoadingScreen()
                !state.isSignedIn -> LoginScreen(
                    themeMode = state.themeMode,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onThemeModeChange = viewModel::setThemeMode,
                    onSignIn = viewModel::signIn
                )
                editorDraft != null -> NoteEditorScreen(
                    themeMode = state.themeMode,
                    draft = editorDraft,
                    isLoading = state.isLoading,
                    onThemeModeChange = viewModel::setThemeMode,
                    onDraftChange = viewModel::updateDraft,
                    onClose = viewModel::closeEditor,
                    onSave = viewModel::saveEditor
                )
                else -> NotesScreen(
                    themeMode = state.themeMode,
                    state = state,
                    onThemeModeChange = viewModel::setThemeMode,
                    onRefresh = viewModel::refreshNotes,
                    onSearchChange = viewModel::setSearchText,
                    onWorkspaceChange = viewModel::setWorkspace,
                    onNewNote = viewModel::openNewNote,
                    onEditNote = viewModel::openEditor,
                    onDeleteNote = viewModel::deleteNote,
                    onTogglePin = viewModel::togglePin,
                    onToggleArchive = viewModel::toggleArchive,
                    onSignOut = viewModel::signOut
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenterMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}
