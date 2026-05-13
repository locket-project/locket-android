package com.inhoolee.locket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inhoolee.locket.domain.LocketNote
import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import com.inhoolee.locket.domain.Workspace

@Composable
fun NotesScreen(
    state: LocketUiState,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onWorkspaceChange: (Workspace) -> Unit,
    onNewNote: (NoteKind) -> Unit,
    onEditNote: (LocketNote) -> Unit,
    onDeleteNote: (LocketNote) -> Unit,
    onTogglePin: (LocketNote) -> Unit,
    onToggleArchive: (LocketNote) -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(onClick = { onNewNote(NoteKind.Checklist) }) {
                    Text("[]")
                }
                FloatingActionButton(onClick = { onNewNote(NoteKind.Text) }) {
                    Icon(Icons.Default.Add, contentDescription = "New note")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Locket", style = MaterialTheme.typography.headlineMedium)
                    Text(state.userEmail, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onSignOut) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign out")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.searchText,
                onValueChange = onSearchChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkspaceButton("Notes", state.workspace == Workspace.Notes) {
                    onWorkspaceChange(Workspace.Notes)
                }
                WorkspaceButton("Archive", state.workspace == Workspace.Archive) {
                    onWorkspaceChange(Workspace.Archive)
                }
            }
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (state.notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onEditNote(note) },
                            onDelete = { onDeleteNote(note) },
                            onTogglePin = { onTogglePin(note) },
                            onToggleArchive = { onToggleArchive(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun NoteCard(
    note: LocketNote,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = note.color.containerColor())
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(note.color.dotColor(), CircleShape)
                )
                Text(
                    text = note.displayTitle,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            val preview = if (note.kind == NoteKind.Checklist) {
                note.checklistItems.take(3).joinToString("\n") {
                    "${if (it.isChecked) "x" else " "} ${it.content}"
                }
            } else {
                note.body
            }
            if (preview.isNotBlank()) {
                Text(preview, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            if (note.labelText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note.labelText, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onTogglePin) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin")
                }
                IconButton(onClick = onToggleArchive) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

private fun NoteColor.containerColor(): Color = when (this) {
    NoteColor.Default -> Color(0xFFFFFCF7)
    NoteColor.Yellow -> Color(0xFFFFF2A8)
    NoteColor.Green -> Color(0xFFD8F2DF)
    NoteColor.Blue -> Color(0xFFDCEBFF)
    NoteColor.Red -> Color(0xFFFFDDE1)
    NoteColor.Gray -> Color(0xFFE2E3E6)
}

private fun NoteColor.dotColor(): Color = when (this) {
    NoteColor.Default -> Color(0xFF837464)
    NoteColor.Yellow -> Color(0xFFB08A00)
    NoteColor.Green -> Color(0xFF2F7D4E)
    NoteColor.Blue -> Color(0xFF286AAD)
    NoteColor.Red -> Color(0xFFC84B5C)
    NoteColor.Gray -> Color(0xFF555960)
}
