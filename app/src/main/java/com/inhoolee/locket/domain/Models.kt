package com.inhoolee.locket.domain

import java.time.Instant

enum class NoteKind(val wireValue: String) {
    Text("text"),
    Checklist("checklist");

    companion object {
        fun fromWire(value: String?): NoteKind =
            entries.firstOrNull { it.wireValue == value } ?: Text
    }
}

enum class Workspace {
    Notes,
    Archive
}

enum class NoteColor(val wireValue: String) {
    Default("default"),
    Yellow("yellow"),
    Green("green"),
    Blue("blue"),
    Red("red"),
    Gray("gray");

    companion object {
        fun fromWire(value: String?): NoteColor =
            entries.firstOrNull { it.wireValue == value } ?: Default
    }
}

data class NoteLabel(
    val id: String,
    val name: String,
    val createdAt: Instant
)

data class ChecklistItem(
    val id: String,
    val noteId: String,
    val content: String,
    val isChecked: Boolean,
    val indentLevel: Int,
    val sortOrder: Double,
    val checkedAt: Instant?,
    val createdAt: Instant
)

data class LocketNote(
    val id: String,
    val title: String,
    val body: String,
    val kind: NoteKind,
    val color: NoteColor,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val sortOrder: Double,
    val createdAt: Instant,
    val updatedAt: Instant,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val labels: List<NoteLabel> = emptyList()
) {
    val displayTitle: String
        get() = title.trim().ifEmpty { "Untitled" }

    val labelText: String
        get() = labels.joinToString(", ") { it.name }
}
