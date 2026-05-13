package com.inhoolee.locket.data

import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseRowsTest {
    @Test
    fun noteRowMapsWireValuesToDomainDefaults() {
        val note = NoteRow(
            id = "note-1",
            title = null,
            body = "body",
            type = "checklist",
            color = "blue",
            is_pinned = true,
            is_archived = false,
            sort_order = 2.0,
            created_at = "2026-05-12T00:00:00Z",
            updated_at = "2026-05-12T01:00:00Z"
        ).toNote(
            checklistItems = listOf(ChecklistItemRow(id = "item-1", note_id = "note-1", content = "task").toChecklistItem()),
            labels = listOf(LabelRow(id = "label-1", name = "Work", created_at = "2026-05-12T00:00:00Z").toLabel())
        )

        assertEquals("note-1", note.id)
        assertEquals("", note.title)
        assertEquals("body", note.body)
        assertEquals(NoteKind.Checklist, note.kind)
        assertEquals(NoteColor.Blue, note.color)
        assertTrue(note.isPinned)
        assertFalse(note.isArchived)
        assertEquals(1, note.checklistItems.size)
        assertEquals("Work", note.labels.single().name)
    }

    @Test
    fun archivedPinnedRowDoesNotExposePinnedState() {
        val note = NoteRow(
            id = "note-1",
            is_pinned = true,
            is_archived = true
        ).toNote()

        assertFalse(note.isPinned)
        assertTrue(note.isArchived)
    }
}
