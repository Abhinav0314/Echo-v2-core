package com.abhinav.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abhinav.domain.manager.DataStoreManager.Values.FALSE
import com.abhinav.domain.manager.DataStoreManager.Values.TRUE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackExitPreferencesTest {
    @Test
    fun unsetPreferenceStopsPlaybackOnTaskRemoval() = runBlocking {
        assertEquals(TRUE, DataStoreManagerImpl(MemoryPreferences()).killServiceOnExit.first())
    }

    @Test
    fun existingExplicitChoicesArePreserved() = runBlocking {
        val key = stringPreferencesKey("kill_service_on_exit")
        for (choice in listOf(TRUE, FALSE)) {
            val store = MemoryPreferences(mutablePreferencesOf(key to choice))
            assertEquals(choice, DataStoreManagerImpl(store).killServiceOnExit.first())
        }
    }

    @Test
    fun disablingAndReenablingSurvivesManagerRecreation() = runBlocking {
        val store = MemoryPreferences()
        DataStoreManagerImpl(store).setKillServiceOnExit(false)
        assertEquals(FALSE, DataStoreManagerImpl(store).killServiceOnExit.first())
        DataStoreManagerImpl(store).setKillServiceOnExit(true)
        assertEquals(TRUE, DataStoreManagerImpl(store).killServiceOnExit.first())
    }

    private class MemoryPreferences(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        override val data = MutableStateFlow(initial)

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
}
