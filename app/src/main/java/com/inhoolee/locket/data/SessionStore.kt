package com.inhoolee.locket.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.Instant

private val Context.locketSessionDataStore by preferencesDataStore(name = "locket_session")

class SessionStore(private val context: Context) {
    private object Keys {
        val AccessToken = stringPreferencesKey("access_token")
        val RefreshToken = stringPreferencesKey("refresh_token")
        val UserId = stringPreferencesKey("user_id")
        val Email = stringPreferencesKey("email")
        val ExpiresAt = longPreferencesKey("expires_at")
    }

    suspend fun load(): AuthSession? {
        val prefs = context.locketSessionDataStore.data.first()
        val accessToken = prefs[Keys.AccessToken] ?: return null
        val userId = prefs[Keys.UserId] ?: return null
        return AuthSession(
            accessToken = accessToken,
            refreshToken = prefs[Keys.RefreshToken],
            userId = userId,
            email = prefs[Keys.Email],
            expiresAt = prefs[Keys.ExpiresAt]?.let(Instant::ofEpochSecond)
        )
    }

    suspend fun save(session: AuthSession) {
        context.locketSessionDataStore.edit { prefs ->
            prefs[Keys.AccessToken] = session.accessToken
            prefs[Keys.UserId] = session.userId
            session.refreshToken?.let { prefs[Keys.RefreshToken] = it } ?: prefs.remove(Keys.RefreshToken)
            session.email?.let { prefs[Keys.Email] = it } ?: prefs.remove(Keys.Email)
            session.expiresAt?.epochSecond?.let { prefs[Keys.ExpiresAt] = it } ?: prefs.remove(Keys.ExpiresAt)
        }
    }

    suspend fun clear() {
        context.locketSessionDataStore.edit { it.clear() }
    }
}
