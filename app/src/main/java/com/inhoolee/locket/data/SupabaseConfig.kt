package com.inhoolee.locket.data

data class SupabaseConfig(
    val url: String,
    val anonKey: String
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank()
}
