package com.inhoolee.locket.data

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val client: SupabaseHttpClient,
    private val sessionStore: SessionStore
) {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session

    suspend fun restoreSession(): AuthSession? {
        val stored = sessionStore.load() ?: run {
            _session.value = null
            return null
        }
        val session = if (stored.isExpired() && stored.refreshToken != null) {
            refreshSession(stored)
        } else {
            stored
        }
        _session.value = session
        return session
    }

    suspend fun validSession(): AuthSession {
        val current = _session.value ?: sessionStore.load()
            ?: throw IllegalStateException("Sign in to sync with Supabase.")
        val session = if (current.isExpired()) {
            refreshSession(current)
        } else {
            current
        }
        _session.value = session
        return session
    }

    suspend fun signIn(email: String, password: String): AuthSession {
        val type = object : TypeToken<AuthTokenResponse>() {}.type
        val response: AuthTokenResponse = client.authRequest(
            path = "token",
            query = listOf("grant_type" to "password"),
            method = "POST",
            body = mapOf("email" to email, "password" to password),
            type = type
        )
        val session = response.toSession()
        sessionStore.save(session)
        _session.value = session
        return session
    }

    suspend fun signOut() {
        _session.value?.accessToken?.let { accessToken ->
            runCatching {
                client.authRequest<Unit>(
                    path = "logout",
                    method = "POST",
                    accessToken = accessToken,
                    type = Unit::class.java
                )
            }
        }
        sessionStore.clear()
        _session.value = null
    }

    private suspend fun refreshSession(stale: AuthSession): AuthSession {
        val refreshToken = stale.refreshToken
            ?: throw IllegalStateException("Session expired. Sign in again.")
        val type = object : TypeToken<AuthTokenResponse>() {}.type
        val response: AuthTokenResponse = client.authRequest(
            path = "token",
            query = listOf("grant_type" to "refresh_token"),
            method = "POST",
            body = mapOf("refresh_token" to refreshToken),
            type = type
        )
        val session = response.toSession(fallbackEmail = stale.email)
        sessionStore.save(session)
        return session
    }
}
