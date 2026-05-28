package com.inhoolee.locket.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.inhoolee.locket.domain.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.locketSettingsDataStore by preferencesDataStore(name = "locket_settings")

class ThemePreferenceStore(private val context: Context) {
    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.locketSettingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs -> ThemeMode.fromWire(prefs[Keys.ThemeMode]) }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.locketSettingsDataStore.edit { prefs ->
            prefs[Keys.ThemeMode] = themeMode.wireValue
        }
    }
}
