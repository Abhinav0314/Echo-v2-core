package com.abhinav.data.backup

import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Portable settings are opt-in: newly added credentials can never silently enter a backup. */
object PortableBackupPreferences {
    private val allowedNames = setOf(
        "app_version", "quality", "download_quality", "video_download_quality",
        "normalize_volume", "skip_silent", "save_state_of_playback", "save_recent_song",
        "recent_song_media_id", "recent_song_position", "shuffle_key", "repeat_key",
        "from_saved_playlist", "kill_service_on_exit", "crossfade_enabled", "crossfade_duration",
        "crossfade_dj_mode", "crossfade_skip_album", "auto_download_liked_songs", "lyrics_provider",
        "translation_language", "use_translation_language", "sponsor_block_enabled", "maxSongCacheSize",
        "watch_video_instead_of_playing_audio", "radio_audio_only", "video_quality", "player_volume",
        "spotify_lyrics", "equalizer_autoeq_profile", "equalizer_bands", "equalizer_enabled",
        "equalizer_type", "equalizer_preamp", "delay_enabled", "delay_time_ms", "delay_feedback",
        "delay_mix", "reverb_enabled", "reverb_preset", "reverb_mix", "spotify_canvas",
        "am_animated_artwork", "home_limit", "chart_key", "theme_mode", "theme_color_source",
        "custom_theme_color", "now_playing_style", "lyrics_style", "romanization_languages",
        "lyrics_offset_ms", "endless_queue", "keep_youtube_playlist_offline",
        "combine_local_and_youtube_liked", "should_show_log_in_required_alert", "auto_check_for_updates",
        "update_channel", "playback_speed", "pitch", "ai_provider", "custom_model_id",
        "use_ai_translation", "local_playlist_filter", "youtube_subtitle_language",
        "preferred_audio_language", "backup_downloaded", "liquid_glass", "explicit_content_enabled",
        "local_tracking_enabled", "blog_notification_enabled", "auto_backup_enabled",
        "auto_backup_frequency", "auto_backup_max_files",
    )

    fun sanitize(preferences: Preferences): Preferences = preferences.toMutablePreferences().apply {
        preferences.asMap().keys.filter { it.name !in allowedNames }.forEach { remove(it) }
    }.toPreferences()
}

/** Uses the already bundled SQLite, including VACUUM INTO on Android 8 and 9. */
object PortableBackupDatabase {
    fun snapshot(sourcePath: String, destinationPath: String) {
        require(sourcePath != destinationPath) { "Backup staging must differ from the live database." }
        BundledSQLiteDriver().open(sourcePath).use { source ->
            source.prepare("PRAGMA busy_timeout=5000").use { it.step() }
            // A consistent SQLite snapshot includes committed WAL rows without copying a live file.
            source.prepare("VACUUM INTO ?").use { statement ->
                statement.bindText(1, destinationPath)
                statement.step()
            }
        }
        sanitize(destinationPath)
    }

    /** Also sanitize legacy imported backups before their files can replace live state. */
    fun sanitize(path: String) {
        BundledSQLiteDriver().open(path).use { database ->
            // Room owns schema behavior. Never execute triggers supplied by an imported
            // archive: they could copy cookies elsewhere during sanitization. Room's
            // onOpen callback recreates the trusted playlist-order trigger after restore.
            val hasViews = database.prepare(
                "SELECT 1 FROM sqlite_master WHERE type='view' LIMIT 1",
            ).use { it.step() }
            require(!hasViews) { "Backup contains unsupported database views." }
            val triggers = database.prepare("SELECT name FROM sqlite_master WHERE type='trigger'").use { statement ->
                buildList { while (statement.step()) add(statement.getText(0)) }
            }
            triggers.forEach { name -> database.execute("DROP TRIGGER \"${name.replace("\"", "\"\"")}\"") }
            database.execute("PRAGMA journal_mode=DELETE")
            database.execute("PRAGMA secure_delete=ON")
            val hasAccounts = database.prepare(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='GoogleAccountEntity' COLLATE NOCASE",
            ).use { it.step() }
            if (hasAccounts) database.execute("DELETE FROM GoogleAccountEntity")
            // Remove old cookies from free pages, not just from query results.
            database.execute("VACUUM")
        }
    }

    private fun SQLiteConnection.execute(sql: String) { prepare(sql).use { it.step() } }
}
