package com.inhoolee.locket.ui

import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDraftTest {
    @Test
    fun emptyDraftHasNoContent() {
        assertFalse(EditorDraft().hasContent())
    }

    @Test
    fun whitespaceOnlyDraftHasNoContent() {
        val draft = EditorDraft(
            title = "   ",
            body = "\n\t ",
            kind = NoteKind.Checklist,
            checklistItems = listOf(ChecklistEditorItem(content = "   "))
        )

        assertFalse(draft.hasContent())
    }

    @Test
    fun titleMakesDraftSavable() {
        assertTrue(EditorDraft(title = "T").hasContent())
    }

    @Test
    fun textBodyMakesDraftSavable() {
        assertTrue(EditorDraft(body = "B").hasContent())
    }

    @Test
    fun checklistItemTextMakesDraftSavable() {
        val draft = EditorDraft(
            kind = NoteKind.Checklist,
            checklistItems = listOf(ChecklistEditorItem(content = "Task"))
        )

        assertTrue(draft.hasContent())
    }

    @Test
    fun labelsColorAndCheckedStateOnlyDoNotMakeDraftSavable() {
        val draft = EditorDraft(
            kind = NoteKind.Checklist,
            color = NoteColor.Yellow,
            labelsCsv = "Work",
            checklistItems = listOf(ChecklistEditorItem(isChecked = true))
        )

        assertFalse(draft.hasContent())
    }
}
