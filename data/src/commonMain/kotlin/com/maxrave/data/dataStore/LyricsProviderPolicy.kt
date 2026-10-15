package com.maxrave.data.dataStore

import com.maxrave.domain.manager.DataStoreManager

/** Existing preferences and cached lyrics are preserved when a service is disabled. */
internal fun effectiveLyricsProvider(saved: String?, communityEnabled: Boolean): String =
    when {
        !communityEnabled && (saved == null || saved == DataStoreManager.SIMPMUSIC) -> DataStoreManager.LRCLIB
        else -> saved ?: DataStoreManager.SIMPMUSIC
    }
