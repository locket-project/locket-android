package com.inhoolee.locket.data

import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadsTest {
    @Test
    fun normalizedLabelNamesTrimsAndDeduplicates() {
        val labels = normalizedLabelNames(listOf(" Work ", "work", "", "Home"))

        assertEquals(listOf("Work", "Home"), labels)
    }

    @Test
    fun noteInsertPayloadMatchesSupabaseColumns() {
        val payload = noteInsertPayload(
            userId = "user-1",
            draft = draft(),
            sortOrder = -100L
        )

        assertEquals("user-1", payload["user_id"])
        assertEquals("Title", payload["title"])
        assertEquals("Body", payload["body"])
        assertEquals("text", payload["type"])
        assertEquals("yellow", payload["color"])
        assertEquals(false, payload["is_pinned"])
        assertEquals(-100L, payload["sort_order"])
    }

    @Test
    fun notePatchPayloadClearsRichBody() {
        val payload = notePatchPayload(draft())

        assertTrue(payload.containsKey("body_rich"))
        assertNull(payload["body_rich"])
    }

    @Test
    fun uncheckedChecklistPayloadClearsCheckedAt() {
        val payload = checklistCheckedPayload(isChecked = false)

        assertEquals(false, payload["is_checked"])
        assertNull(payload["checked_at"])
    }

    @Test
    fun checklistInsertPayloadClampsIndentAndSetsCheckedAt() {
        val payload = checklistInsertPayload(
            noteId = "note-1",
            userId = "user-1",
            item = ChecklistDraftPayload(content = "Task", isChecked = true, indentLevel = 99),
            sortOrder = 3
        )

        assertEquals("note-1", payload["note_id"])
        assertEquals("user-1", payload["user_id"])
        assertEquals("Task", payload["content"])
        assertEquals(4, payload["indent_level"])
        assertEquals(3, payload["sort_order"])
        assertTrue(payload["checked_at"] is String)
    }

    private fun draft() = NoteDraftPayload(
        title = "Title",
        body = "Body",
        kind = NoteKind.Text,
        color = NoteColor.Yellow,
        labelNames = listOf("Work"),
        checklistItems = emptyList()
    )
}
