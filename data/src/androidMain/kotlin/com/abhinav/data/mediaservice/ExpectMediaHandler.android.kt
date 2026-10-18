package com.abhinav.data.mediaservice

import com.abhinav.domain.repository.AnalyticsRepository

actual fun createMediaServiceHandler(
    dataStoreManager: com.abhinav.domain.manager.DataStoreManager,
    songRepository: com.abhinav.domain.repository.SongRepository,
    streamRepository: com.abhinav.domain.repository.StreamRepository,
    localPlaylistRepository: com.abhinav.domain.repository.LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.abhinav.domain.mediaservice.handler.MediaPlayerHandler =
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        analyticsRepository,
        coroutineScope,
    )