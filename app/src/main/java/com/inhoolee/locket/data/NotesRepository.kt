package com.inhoolee.locket.data

import com.google.gson.reflect.TypeToken
import com.inhoolee.locket.domain.ChecklistItem
import com.inhoolee.locket.domain.LocketNote
import com.inhoolee.locket.domain.NoteKind
import com.inhoolee.locket.domain.NoteLabel
import com.inhoolee.locket.domain.Workspace
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class NotesRepository(
    private val client: SupabaseHttpClient,
    private val authRepository: AuthRepository
) {
    private val noteRowsType = object : TypeToken<List<NoteRow>>() {}.type
    private val checklistRowsType = object : TypeToken<List<ChecklistItemRow>>() {}.type
    private val labelRowsType = object : TypeToken<List<LabelRow>>() {}.type
    private val noteLabelRowsType = object : TypeToken<List<NoteLabelJoinRow>>() {}.type
    private val resolvedLabelRowsType = object : TypeToken<List<ResolvedLabelRow>>() {}.type
    private val sortRowsType = object : TypeToken<List<SortOrderRow>>() {}.type
    private val noteSelect = "id,user_id,title,body,reminder_at,type,color,is_pinned,is_archived,sort_order,created_at,updated_at"

    suspend fun listNotes(workspace: Workspace, searchText: String): List<LocketNote> {
        val session = authRepository.validSession()
        val query = buildListQuery(session.userId, workspace)
        val noteRows: List<NoteRow> = client.postgrestRequest(
            table = "notes",
            query = query,
            accessToken = session.accessToken,
            type = noteRowsType
        )
        if (noteRows.isEmpty()) return emptyList()
        val notes = hydrateNotes(noteRows, session.accessToken)

        val queryText = searchText.trim()
        return notes.filter { note ->
            queryText.isEmpty() ||
                note.title.contains(queryText, ignoreCase = true) ||
                note.body.contains(queryText, ignoreCase = true) ||
                note.labels.any { it.name.contains(queryText, ignoreCase = true) } ||
                note.checklistItems.any { it.content.contains(queryText, ignoreCase = true) }
        }.sortedWith(compareBy<LocketNote> { it.sortOrder }.thenByDescending { it.updatedAt })
    }

    suspend fun saveNote(noteId: String?, draft: NoteDraftPayload): LocketNote {
        return if (noteId == null) {
            createNote(draft)
        } else {
            updateNote(noteId, draft)
        }
    }

    suspend fun deleteNote(note: LocketNote) {
        val session = authRepository.validSession()
        client.postgrestVoid(
            table = "notes",
            query = noteQuery(note.id, session.userId),
            method = "DELETE",
            accessToken = session.accessToken
        )
    }

    suspend fun togglePin(note: LocketNote) {
        val session = authRepository.validSession()
        val nextPinned = !note.isPinned
        val sortOrder = if (nextPinned) nextPinnedSortOrder(session.userId, session.accessToken) else -System.currentTimeMillis()
        client.postgrestVoid(
            table = "notes",
            query = noteQuery(note.id, session.userId),
            method = "PATCH",
            accessToken = session.accessToken,
            body = mapOf(
                "is_pinned" to nextPinned,
                "sort_order" to sortOrder
            )
        )
    }

    suspend fun toggleArchive(note: LocketNote) {
        val session = authRepository.validSession()
        client.postgrestVoid(
            table = "notes",
            query = noteQuery(note.id, session.userId),
            method = "PATCH",
            accessToken = session.accessToken,
            body = mapOf(
                "is_archived" to !note.isArchived,
                "is_pinned" to false,
                "sort_order" to -System.currentTimeMillis()
            )
        )
    }

    private suspend fun createNote(draft: NoteDraftPayload): LocketNote {
        val session = authRepository.validSession()
        val insertedRows = client.postgrestRequest<List<NoteRow>>(
            table = "notes",
            method = "POST",
            accessToken = session.accessToken,
            body = noteInsertPayload(session.userId, draft),
            type = noteRowsType
        )
        val insertedRow = insertedRows.firstOrNull()
            ?: throw IllegalStateException("Supabase returned no note after insert.")
        val id = insertedRow.id
            ?: throw IllegalStateException("Supabase returned no note id after insert.")
        syncNoteDetails(
            userId = session.userId,
            noteId = id,
            draft = draft,
            accessToken = session.accessToken,
            deleteExistingChecklist = false,
            deleteExistingLabels = false
        )
        return if (canUseNoteRowOnly(draft)) {
            insertedRow.toNote()
        } else {
            fetchNote(id, session.userId, session.accessToken)
        }
    }

    private suspend fun updateNote(noteId: String, draft: NoteDraftPayload): LocketNote {
        val session = authRepository.validSession()
        val updatedRows = client.postgrestRequest<List<NoteRow>>(
            table = "notes",
            query = noteQuery(noteId, session.userId),
            method = "PATCH",
            accessToken = session.accessToken,
            body = notePatchPayload(draft),
            type = noteRowsType
        )
        val updatedRow = updatedRows.firstOrNull()
            ?: throw IllegalStateException("Remote note was not found while updating.")
        syncNoteDetails(
            userId = session.userId,
            noteId = noteId,
            draft = draft,
            accessToken = session.accessToken,
            deleteExistingChecklist = true,
            deleteExistingLabels = true
        )
        return if (canUseNoteRowOnly(draft)) {
            updatedRow.toNote()
        } else {
            fetchNote(noteId, session.userId, session.accessToken)
        }
    }

    private suspend fun fetchNote(noteId: String, userId: String, accessToken: String): LocketNote {
        val noteRows: List<NoteRow> = client.postgrestRequest(
            table = "notes",
            query = noteQuery(noteId, userId) + ("select" to noteSelect),
            accessToken = accessToken,
            type = noteRowsType
        )
        return hydrateNotes(noteRows, accessToken).firstOrNull()
            ?: throw IllegalStateException("Remote note was not found after syncing.")
    }

    private fun buildListQuery(userId: String, workspace: Workspace): List<Pair<String, String>> {
        val query = mutableListOf(
            "select" to noteSelect,
            "user_id" to "eq.${userId.lowercase()}",
            "order" to "is_pinned.desc",
            "order" to "sort_order.asc",
            "order" to "updated_at.desc"
        )
        when (workspace) {
            Workspace.Notes -> query += "is_archived" to "eq.false"
            Workspace.Archive -> query += "is_archived" to "eq.true"
        }
        return query
    }

    private suspend fun hydrateNotes(noteRows: List<NoteRow>, accessToken: String): List<LocketNote> = coroutineScope {
        val noteIds = noteRows.mapNotNull { it.id }
        if (noteRows.isEmpty() || noteIds.isEmpty()) {
            noteRows.map { it.toNote() }
        } else {
            val checklistRowsDeferred = async {
                fetchRows<ChecklistItemRow>(
                    table = "note_checklist_items",
                    select = "id,note_id,content,is_checked,indent_level,sort_order,checked_at,created_at",
                    noteIds = noteIds,
                    accessToken = accessToken,
                    ordered = true,
                    type = checklistRowsType
                )
            }
            val joinRowsDeferred = async {
                fetchRows<NoteLabelJoinRow>(
                    table = "note_labels",
                    select = "note_id,label_id",
                    noteIds = noteIds,
                    accessToken = accessToken,
                    ordered = false,
                    type = noteLabelRowsType
                )
            }
            val checklistRows = checklistRowsDeferred.await()
            val joinRows = joinRowsDeferred.await()
            val labelsById = labelsById(joinRows.mapNotNull { it.label_id }, accessToken)
            val checklistByNote = checklistRows.map { it.toChecklistItem() }.groupBy { it.noteId }
            val labelsByNote = joinRows.groupBy { it.note_id.orEmpty() }

            noteRows.map { row ->
                val noteId = row.id.orEmpty()
                row.toNote(
                    checklistItems = checklistByNote[noteId].orEmpty(),
                    labels = labelsByNote[noteId].orEmpty().mapNotNull { labelsById[it.label_id] }
                )
            }
        }
    }

    private suspend fun <T> fetchRows(
        table: String,
        select: String,
        noteIds: List<String>,
        accessToken: String,
        ordered: Boolean,
        type: java.lang.reflect.Type
    ): List<T> {
        if (noteIds.isEmpty()) return emptyList()
        val query = mutableListOf(
            "select" to select,
            "note_id" to "in.(${noteIds.joinToString(",")})"
        )
        if (ordered) {
            query += "order" to "sort_order.asc"
            query += "order" to "created_at.asc"
        }
        return client.postgrestRequest(
            table = table,
            query = query,
            accessToken = accessToken,
            type = type
        )
    }

    private suspend fun labelsById(labelIds: List<String>, accessToken: String): Map<String?, NoteLabel> {
        val uniqueIds = labelIds.distinct()
        if (uniqueIds.isEmpty()) return emptyMap()
        val rows: List<LabelRow> = client.postgrestRequest(
            table = "labels",
            query = listOf(
                "select" to "id,name,created_at",
                "id" to "in.(${uniqueIds.joinToString(",")})"
            ),
            accessToken = accessToken,
            type = labelRowsType
        )
        return rows.associate { it.id to it.toLabel() }
    }

    private suspend fun syncNoteDetails(
        userId: String,
        noteId: String,
        draft: NoteDraftPayload,
        accessToken: String,
        deleteExistingChecklist: Boolean,
        deleteExistingLabels: Boolean
    ) = coroutineScope {
        val labelNames = normalizedLabelNames(draft.labelNames)
        val labelsDeferred = if (deleteExistingLabels || labelNames.isNotEmpty()) {
            async { syncLabels(userId, noteId, labelNames, accessToken, deleteExistingLabels) }
        } else {
            null
        }
        val checklistDeferred = when {
            draft.kind == NoteKind.Text && deleteExistingChecklist -> async {
                deleteChecklistItems(userId, noteId, accessToken)
            }
            draft.kind == NoteKind.Checklist -> async {
                replaceChecklistItems(
                    userId = userId,
                    noteId = noteId,
                    items = draft.checklistItems,
                    accessToken = accessToken,
                    deleteExisting = deleteExistingChecklist,
                    touchAfter = deleteExistingChecklist
                )
            }
            else -> null
        }

        labelsDeferred?.await()
        checklistDeferred?.await()
    }

    private suspend fun syncLabels(
        userId: String,
        noteId: String,
        normalized: List<String>,
        accessToken: String,
        deleteExisting: Boolean
    ) {
        if (deleteExisting) {
            client.postgrestVoid(
                table = "note_labels",
                query = listOf("user_id" to "eq.$userId", "note_id" to "eq.$noteId"),
                method = "DELETE",
                accessToken = accessToken
            )
        }
        if (normalized.isEmpty()) return

        val keys = normalized.map { it.lowercase() }
        val existing: List<ResolvedLabelRow> = client.postgrestRequest(
            table = "labels",
            query = listOf(
                "select" to "id,name_normalized",
                "user_id" to "eq.$userId",
                "name_normalized" to quotedInList(keys)
            ),
            accessToken = accessToken,
            type = resolvedLabelRowsType
        )
        val existingKeys = existing.mapNotNull { it.name_normalized }.toSet()
        val missingRows = normalized
            .filter { it.lowercase() !in existingKeys }
            .map { mapOf("user_id" to userId, "name" to it) }
        if (missingRows.isNotEmpty()) {
            client.postgrestVoid(
                table = "labels",
                query = listOf("on_conflict" to "user_id,name_normalized"),
                method = "POST",
                accessToken = accessToken,
                body = missingRows,
                prefer = "resolution=ignore-duplicates,return=minimal"
            )
        }

        val resolved: List<ResolvedLabelRow> = client.postgrestRequest(
            table = "labels",
            query = listOf(
                "select" to "id,name_normalized",
                "user_id" to "eq.$userId",
                "name_normalized" to quotedInList(keys)
            ),
            accessToken = accessToken,
            type = resolvedLabelRowsType
        )
        val joinRows = resolved.mapNotNull { row ->
            row.id?.let { labelId ->
                mapOf("user_id" to userId, "note_id" to noteId, "label_id" to labelId)
            }
        }
        if (joinRows.isNotEmpty()) {
            client.postgrestVoid(
                table = "note_labels",
                query = listOf("on_conflict" to "note_id,label_id"),
                method = "POST",
                accessToken = accessToken,
                body = joinRows,
                prefer = "resolution=ignore-duplicates,return=minimal"
            )
        }
    }

    private suspend fun replaceChecklistItems(
        userId: String,
        noteId: String,
        items: List<ChecklistDraftPayload>,
        accessToken: String,
        deleteExisting: Boolean = true,
        touchAfter: Boolean = true
    ) {
        if (deleteExisting) {
            deleteChecklistItems(userId, noteId, accessToken)
        }
        val rows = items
            .map { it.copy(content = it.content.trim()) }
            .filter { it.content.isNotEmpty() }
            .mapIndexed { index, item ->
                checklistInsertPayload(
                    noteId = noteId,
                    userId = userId,
                    item = item,
                    sortOrder = index
                )
            }
        if (rows.isNotEmpty()) {
            client.postgrestVoid(
                table = "note_checklist_items",
                method = "POST",
                accessToken = accessToken,
                body = rows,
                prefer = "return=minimal"
            )
        }
        if (touchAfter) {
            touchNote(userId, noteId, accessToken)
        }
    }

    private suspend fun deleteChecklistItems(userId: String, noteId: String, accessToken: String) {
        client.postgrestVoid(
            table = "note_checklist_items",
            query = listOf("user_id" to "eq.$userId", "note_id" to "eq.$noteId"),
            method = "DELETE",
            accessToken = accessToken
        )
    }

    private suspend fun nextPinnedSortOrder(userId: String, accessToken: String): Long {
        val rows: List<SortOrderRow> = client.postgrestRequest(
            table = "notes",
            query = listOf(
                "select" to "sort_order",
                "user_id" to "eq.$userId",
                "is_archived" to "eq.false",
                "is_pinned" to "eq.true",
                "order" to "sort_order.desc",
                "limit" to "1"
            ),
            accessToken = accessToken,
            type = sortRowsType
        )
        return ((rows.firstOrNull()?.sort_order ?: -1.0) + 1).toLong()
    }

    private suspend fun touchNote(userId: String, noteId: String, accessToken: String) {
        client.postgrestVoid(
            table = "notes",
            query = noteQuery(noteId, userId),
            method = "PATCH",
            accessToken = accessToken,
            body = mapOf("updated_at" to isoNow())
        )
    }

    private fun canUseNoteRowOnly(draft: NoteDraftPayload): Boolean =
        draft.kind == NoteKind.Text && normalizedLabelNames(draft.labelNames).isEmpty()

    private fun noteQuery(noteId: String, userId: String): List<Pair<String, String>> =
        listOf("user_id" to "eq.$userId", "id" to "eq.$noteId")

    private fun quotedInList(values: List<String>): String =
        values.joinToString(separator = ",", prefix = "in.(", postfix = ")") { value ->
            "\"${value.replace("\"", "\\\"")}\""
        }
}
