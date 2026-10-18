package com.abhinav.data.di

import com.abhinav.common.AppIdentity
import com.abhinav.common.Config.SERVICE_SCOPE
import com.abhinav.data.io.fileDir
import com.abhinav.data.repository.AccountRepositoryImpl
import com.abhinav.data.repository.AlbumRepositoryImpl
import com.abhinav.data.repository.AnalyticsRepositoryImpl
import com.abhinav.data.repository.ArtistRepositoryImpl
import com.abhinav.data.repository.AutoEqRepositoryImpl
import com.abhinav.data.lyrics.LyricsRomanizerRepositoryImpl
import com.abhinav.data.repository.CommonRepositoryImpl
import com.abhinav.data.repository.HomeRepositoryImpl
import com.abhinav.data.repository.ImportRepositoryImpl
import com.abhinav.data.repository.LocalPlaylistRepositoryImpl
import com.abhinav.data.repository.LyricsCanvasRepositoryImpl
import com.abhinav.data.repository.PlaylistRepositoryImpl
import com.abhinav.data.repository.PodcastRepositoryImpl
import com.abhinav.data.repository.SearchRepositoryImpl
import com.abhinav.data.repository.SongRepositoryImpl
import com.abhinav.data.repository.StreamRepositoryImpl
import com.abhinav.data.repository.TasteRepositoryImpl
import com.abhinav.data.repository.UpdateRepositoryImpl
import com.abhinav.domain.repository.AccountRepository
import com.abhinav.domain.repository.AlbumRepository
import com.abhinav.domain.repository.AnalyticsRepository
import com.abhinav.domain.repository.ArtistRepository
import com.abhinav.domain.repository.AutoEqRepository
import com.abhinav.domain.repository.LyricsRomanizerRepository
import com.abhinav.domain.repository.CommonRepository
import com.abhinav.domain.repository.HomeRepository
import com.abhinav.domain.repository.ImportRepository
import com.abhinav.domain.repository.LocalPlaylistRepository
import com.abhinav.domain.repository.LyricsCanvasRepository
import com.abhinav.domain.repository.PlaylistRepository
import com.abhinav.domain.repository.PodcastRepository
import com.abhinav.domain.repository.SearchRepository
import com.abhinav.domain.repository.SongRepository
import com.abhinav.domain.repository.StreamRepository
import com.abhinav.domain.repository.TasteRepository
import com.abhinav.domain.repository.UpdateRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get(), get<AppIdentity>().isDevBuild)
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        // Lazy: constructing it costs a few File.length() calls (plus, while the pack is missing,
        // one background read of the stored selection), but the kuromoji dictionary behind it is
        // loaded on first Japanese line, or right after a download installs it, and never at
        // startup — so this must NOT be createdAtStart, or every launch pays for a feature most
        // listeners leave off. The path is where Android keeps the downloaded ipadic pack (the
        // APK no longer bundles it); Desktop and iOS ignore it.
        single<LyricsRomanizerRepository> {
            LyricsRomanizerRepositoryImpl("${fileDir()}/kuromoji-ipadic", get())
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get()) { get() }
        }

        single<AnalyticsRepository>(createdAtStart = true) {
            AnalyticsRepositoryImpl(get())
        }

        // Lazy: only the taste card in Library asks for it.
        single<TasteRepository> {
            TasteRepositoryImpl(get(), get())
        }
    }
