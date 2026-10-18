package com.abhinav.data.di

import DatabaseDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.abhinav.common.AppIdentity
import com.abhinav.data.dataStore.DataStoreManagerImpl
import com.abhinav.data.dataStore.createDataStoreInstance
import com.abhinav.data.db.Converters
import com.abhinav.data.db.MusicDatabase
import com.abhinav.data.db.datasource.AnalyticsDatasource
import com.abhinav.data.db.datasource.LocalDataSource
import com.abhinav.data.db.getDatabaseBuilder
import com.abhinav.domain.manager.DataStoreManager
import com.abhinav.kotlinytmusicscraper.YouTube
import com.abhinav.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import org.abhinav.echo.aiservice.AiClient
import org.abhinav.echo.lyrics.SimpMusicLyricsClient
import kotlin.time.ExperimentalTime
import org.abhinav.echo.autoeq.AutoEq

@OptIn(ExperimentalTime::class)
val databaseModule =
    module {
        single(createdAtStart = true) {
            Converters()
        }
        // Database
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>(), get<MusicDatabase>())
        }
        // AnalyticsDatasource
        single(createdAtStart = true) {
            AnalyticsDatasource(get<DatabaseDao>())
        }
        // Datastore
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
        // DatastoreManager
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>(), get<AppIdentity>().communityLyricsEnabled)
        }

        // Move YouTube from Singleton to Koin DI
        single(createdAtStart = true) {
            YouTube()
        }

        single(createdAtStart = true) {
            Spotify()
        }

        single(createdAtStart = true) {
            AiClient()
        }

        single(createdAtStart = true) {
            SimpMusicLyricsClient(communityApiEnabled = get<AppIdentity>().communityLyricsEnabled)
        }

        // Not created at start, unlike the rest: nothing needs it until someone opens the AutoEq
        // picker, and it holds an HTTP client the vast majority of sessions never use.
        single {
            AutoEq()
        }
    }
