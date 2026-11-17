package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "my_store_settings"
)

data class StorePreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val catalogUrl: String = SettingsDataStore.DEFAULT_CATALOG_URL,
    val notificationsEnabled: Boolean = true,
    val autoCheckUpdates: Boolean = true
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CATALOG_URL = stringPreferencesKey("catalog_url")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }

    val preferencesFlow: Flow<StorePreferences> = context.settingsDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.DARK)
        StorePreferences(
            themeMode = themeMode,
            catalogUrl = prefs[Keys.CATALOG_URL]?.takeIf { it.isNotBlank() }
                ?: DEFAULT_CATALOG_URL,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setCatalogUrl(rawInput: String) {
        val normalized = normalizeCatalogInput(rawInput)
        context.settingsDataStore.edit { it[Keys.CATALOG_URL] = normalized }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    companion object {
        const val DEFAULT_CATALOG_URL = "https://g4scumilla.github.io/21stfeb/data/catalog.json"

        /**
         * Accepts either a full https://.../catalog.json URL, a GitHub Pages base URL,
         * or a shorthand "username/repo" string and converts it cleanly to the GitHub Pages catalog URL.
         */
        fun normalizeCatalogInput(rawInput: String): String {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.startsWith("https://", ignoreCase = true)) {
                return if (trimmed.endsWith(".json", ignoreCase = true)) {
                    trimmed
                } else {
                    trimmed.trimEnd('/') + "/data/catalog.json"
                }
            }
            if (trimmed.startsWith("http://", ignoreCase = true)) {
                return trimmed // Rejected later by HTTPS-only validation with a clear message
            }
            // Allow "owner/repo" shorthand for beginners
            val parts = trimmed.trim('/').split("/")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return "https://${parts[0].lowercase()}.github.io/${parts[1]}/data/catalog.json"
            }
            return trimmed
        }
    }
}
