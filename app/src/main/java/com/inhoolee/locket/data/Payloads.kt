package com.inhoolee.locket.data

import com.inhoolee.locket.domain.ChecklistItem
import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import java.util.UUID

data class NoteDraftPayload(
    val title: String,
    val body: String,
    val kind: NoteKind,
    val color: NoteColor,
    val labelNames: List<String>,
    val checklistItems: List<ChecklistDraftPayload>
)

data class ChecklistDraftPayload(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isChecked: Boolean,
    val indentLevel: Int = 0
)

fun normalizedLabelNames(names: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    return names.map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { seen.add(it.lowercase()) }
}

fun noteInsertPayload(
    id: String,
    userId: String,
    draft: NoteDraftPayload,
    isPinned: Boolean = false,
    sortOrder: Double = -(System.currentTimeMillis().toDouble())
): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "title" to draft.title,
    "body" to draft.body,
    "type" to draft.kind.wireValue,
    "color" to draft.color.wireValue,
    "is_pinned" to isPinned,
    "sort_order" to sortOrder
)

fun notePatchPayload(draft: NoteDraftPayload): Map<String, Any?> = mapOf(
    "title" to draft.title,
    "body" to draft.body,
    "body_rich" to null,
    "type" to draft.kind.wireValue,
    "color" to draft.color.wireValue
)

fun checklistInsertPayload(
    id: String,
    noteId: String,
    userId: String,
    item: ChecklistDraftPayload,
    sortOrder: Int
): Map<String, Any?> = mapOf(
    "id" to id,
    "note_id" to noteId,
    "user_id" to userId,
    "content" to item.content,
    "content_rich" to null,
    "is_checked" to item.isChecked,
    "indent_level" to item.indentLevel.coerceIn(0, 4),
    "sort_order" to sortOrder,
    "checked_at" to if (item.isChecked) isoNow() else null
)

fun checklistCheckedPayload(isChecked: Boolean): Map<String, Any?> = mapOf(
    "is_checked" to isChecked,
    "checked_at" to if (isChecked) isoNow() else null
)

fun ChecklistItem.toDraftPayload(): ChecklistDraftPayload =
    ChecklistDraftPayload(
        id = id,
        content = content,
        isChecked = isChecked,
        indentLevel = indentLevel
    )
