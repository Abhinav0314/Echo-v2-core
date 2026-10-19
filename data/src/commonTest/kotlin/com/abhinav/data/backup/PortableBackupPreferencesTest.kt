package com.abhinav.data.backup

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals

class PortableBackupPreferencesTest {
    @Test fun smartDownloadChoicesTravelWithoutDeviceSchedulingState() {
        val enabled = stringPreferencesKey("smart_downloads_enabled")
        val count = intPreferencesKey("smart_downloads_count")
        val lastRun = longPreferencesKey("smart_downloads_last_run")
        val live = mutablePreferencesOf(enabled to "TRUE", count to 25, lastRun to 123456L)
        val portable = PortableBackupPreferences.sanitize(live)
        assertEquals("TRUE", portable[enabled])
        assertEquals(25, portable[count])
        assertEquals(null, portable[lastRun])
        assertEquals(123456L, live[lastRun])
    }

    @Test fun exportsOnlyPortableSettingsAndDoesNotMutateLiveCredentials() {
        val theme = stringPreferencesKey("theme_mode")
        val quality = stringPreferencesKey("quality")
        val secretNames = listOf("cookie", "sp_dc", "discord_token", "ai_gemini_api_key",
            "lastfm_session_key", "spotify_personal_token", "spotify_client_token",
            "tidal_client_secret", "proxy_password", "custom_openai_headers", "new_provider_secret")
        val live = mutablePreferencesOf(theme to "dark", quality to "high")
        secretNames.forEach { live[stringPreferencesKey(it)] = "synthetic-secret-$it" }
        val portable = PortableBackupPreferences.sanitize(live)
        assertEquals(setOf("theme_mode", "quality"), portable.asMap().keys.map { it.name }.toSet())
        assertEquals("dark", portable[theme])
        assertEquals("high", portable[quality])
        secretNames.forEach { assertEquals("synthetic-secret-$it", live[stringPreferencesKey(it)]) }
    }
}
