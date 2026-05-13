package com.inhoolee.locket.data

import com.google.gson.annotations.SerializedName
import com.inhoolee.locket.domain.ChecklistItem
import com.inhoolee.locket.domain.LocketNote
import com.inhoolee.locket.domain.NoteColor
import com.inhoolee.locket.domain.NoteKind
import com.inhoolee.locket.domain.NoteLabel
import java.time.Instant

data class AuthTokenResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    val user: AuthUserResponse? = null
) {
    fun toSession(fallbackEmail: String? = null): AuthSession {
        val userId = user?.id ?: throw IllegalStateException("Supabase returned no user id.")
        return AuthSession(
            accessToken = accessToken ?: throw IllegalStateException("Supabase returned no access token."),
            refreshToken = refreshToken,
            userId = userId,
            email = user.email ?: fallbackEmail,
            expiresAt = expiresIn?.let { Instant.now().plusSeconds(it) }
        )
    }
}

data class AuthUserResponse(
    val id: String? = null,
    val email: String? = null
)

data class NoteRow(
    val id: String? = null,
    val user_id: String? = null,
    val title: String? = null,
    val body: String? = null,
    val reminder_at: String? = null,
    val type: String? = null,
    val color: String? = null,
    val is_pinned: Boolean? = null,
    val is_archived: Boolean? = null,
    val sort_order: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    fun toNote(
        checklistItems: List<ChecklistItem> = emptyList(),
        labels: List<NoteLabel> = emptyList()
    ): LocketNote = LocketNote(
        id = id.orEmpty(),
        title = title.orEmpty(),
        body = body.orEmpty(),
        kind = NoteKind.fromWire(type),
        color = NoteColor.fromWire(color),
        isPinned = is_pinned == true && is_archived != true,
        isArchived = is_archived == true,
        sortOrder = sort_order ?: 0.0,
        createdAt = parseSupabaseInstant(created_at),
        updatedAt = parseSupabaseInstant(updated_at),
        checklistItems = checklistItems,
        labels = labels.sortedBy { it.name.lowercase() }
    )
}

data class ChecklistItemRow(
    val id: String? = null,
    val note_id: String? = null,
    val content: String? = null,
    val is_checked: Boolean? = null,
    val indent_level: Int? = null,
    val sort_order: Double? = null,
    val checked_at: String? = null,
    val created_at: String? = null
) {
    fun toChecklistItem(): ChecklistItem = ChecklistItem(
        id = id.orEmpty(),
        noteId = note_id.orEmpty(),
        content = content.orEmpty(),
        isChecked = is_checked == true,
        indentLevel = (indent_level ?: 0).coerceIn(0, 4),
        sortOrder = sort_order ?: 0.0,
        checkedAt = checked_at?.let(::parseSupabaseInstant),
        createdAt = parseSupabaseInstant(created_at)
    )
}

data class LabelRow(
    val id: String? = null,
    val name: String? = null,
    val created_at: String? = null
) {
    fun toLabel(): NoteLabel = NoteLabel(
        id = id.orEmpty(),
        name = name.orEmpty(),
        createdAt = parseSupabaseInstant(created_at)
    )
}

data class ResolvedLabelRow(
    val id: String? = null,
    val name_normalized: String? = null
)

data class NoteLabelJoinRow(
    val note_id: String? = null,
    val label_id: String? = null
)

data class SortOrderRow(
    val sort_order: Double? = null
)
