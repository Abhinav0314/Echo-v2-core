package com.abhinav.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: com.abhinav.domain.manager.DataStoreManager,
    songRepository: com.abhinav.domain.repository.SongRepository,
    streamRepository: com.abhinav.domain.repository.StreamRepository,
    localPlaylistRepository: com.abhinav.domain.repository.LocalPlaylistRepository,
    analyticsRepository: com.abhinav.domain.repository.AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.abhinav.domain.mediaservice.handler.MediaPlayerHandler {
    TODO("Not yet implemented")
}