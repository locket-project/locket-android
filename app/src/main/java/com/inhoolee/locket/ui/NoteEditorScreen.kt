package com.inhoolee.locket.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind

@Composable
fun NoteEditorScreen(
    draft: EditorDraft,
    isLoading: Boolean,
    onDraftChange: (EditorDraft) -> Unit,
    onFinish: () -> Unit
) {
    BackHandler {
        if (!isLoading) {
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFinish, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = draft.title,
            onValueChange = { onDraftChange(draft.copy(title = it)) },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindButton("Note", draft.kind == NoteKind.Text) {
                onDraftChange(draft.copy(kind = NoteKind.Text, checklistItems = emptyList()))
            }
            KindButton("Checklist", draft.kind == NoteKind.Checklist) {
                onDraftChange(
                    draft.copy(
                        kind = NoteKind.Checklist,
                        checklistItems = draft.checklistItems.ifEmpty { listOf(ChecklistEditorItem()) }
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ColorPicker(selected = draft.color) {
            onDraftChange(draft.copy(color = it))
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (draft.kind == NoteKind.Text) {
            OutlinedTextField(
                value = draft.body,
                onValueChange = { onDraftChange(draft.copy(body = it)) },
                label = { Text("Body") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ChecklistEditor(
                items = draft.checklistItems,
                onItemsChange = { onDraftChange(draft.copy(checklistItems = it)) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = draft.labelsCsv,
            onValueChange = { onDraftChange(draft.copy(labelsCsv = it)) },
            label = { Text("Labels, comma separated") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KindButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun ColorPicker(selected: NoteColor, onSelect: (NoteColor) -> Unit) {
    val isDarkTheme = isLocketDarkTheme()

    Column {
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteColor.entries.forEach { color ->
                val borderText = if (selected == color) "x" else ""
                val swatch = color.swatch(isDarkTheme)
                OutlinedButton(onClick = { onSelect(color) }) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(swatch, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(borderText, color = swatch.contentColor())
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: List<ChecklistEditorItem>,
    onItemsChange: (List<ChecklistEditorItem>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        onItemsChange(items.replaceAt(index, item.copy(isChecked = checked)))
                    }
                )
                OutlinedTextField(
                    value = item.content,
                    onValueChange = { content ->
                        onItemsChange(items.replaceAt(index, item.copy(content = content)))
                    },
                    label = { Text("Item") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onItemsChange(items.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item")
                }
            }
        }
        OutlinedButton(onClick = { onItemsChange(items + ChecklistEditorItem()) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add item")
        }
    }
}

private fun List<ChecklistEditorItem>.replaceAt(index: Int, item: ChecklistEditorItem): List<ChecklistEditorItem> =
    mapIndexed { currentIndex, current -> if (currentIndex == index) item else current }

private fun NoteColor.swatch(isDarkTheme: Boolean): Color =
    if (isDarkTheme) {
        when (this) {
            NoteColor.Default -> Color(0xFFBDAF9D)
            NoteColor.Yellow -> Color(0xFFFFD54F)
            NoteColor.Green -> Color(0xFF85D89C)
            NoteColor.Blue -> Color(0xFF8AB8F5)
            NoteColor.Red -> Color(0xFFF48A98)
            NoteColor.Gray -> Color(0xFFAEB4BD)
        }
    } else {
        when (this) {
            NoteColor.Default -> Color(0xFFEDE4D8)
            NoteColor.Yellow -> Color(0xFFFFD54F)
            NoteColor.Green -> Color(0xFF85D89C)
            NoteColor.Blue -> Color(0xFF8AB8F5)
            NoteColor.Red -> Color(0xFFF48A98)
            NoteColor.Gray -> Color(0xFF7B8088)
        }
    }

private fun Color.contentColor(): Color =
    if (luminance() > 0.5f) Color.Black else Color.White
