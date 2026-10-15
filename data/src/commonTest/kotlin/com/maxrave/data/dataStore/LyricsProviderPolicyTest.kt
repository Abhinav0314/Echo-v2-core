package com.maxrave.data.dataStore

import com.maxrave.domain.manager.DataStoreManager
import kotlin.test.*

class LyricsProviderPolicyTest {
    @Test fun newAndLegacyCommunitySelectionsUseWorkingAlternativeWhenDisabled() {
        assertEquals(DataStoreManager.LRCLIB, effectiveLyricsProvider(null, false))
        assertEquals(DataStoreManager.LRCLIB, effectiveLyricsProvider(DataStoreManager.SIMPMUSIC, false))
    }

    @Test fun otherChoicesAndUpstreamDefaultRemainAvailable() {
        listOf(DataStoreManager.LRCLIB, DataStoreManager.YOUTUBE, DataStoreManager.BETTER_LYRICS).forEach {
            assertEquals(it, effectiveLyricsProvider(it, false))
            assertEquals(it, effectiveLyricsProvider(it, true))
        }
        assertEquals(DataStoreManager.SIMPMUSIC, effectiveLyricsProvider(null, true))
    }
}
