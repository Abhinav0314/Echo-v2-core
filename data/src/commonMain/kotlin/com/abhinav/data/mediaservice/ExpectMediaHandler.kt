package com.abhinav.data.mediaservice

import com.abhinav.domain.manager.DataStoreManager
import com.abhinav.domain.mediaservice.handler.MediaPlayerHandler
import com.abhinav.domain.repository.AnalyticsRepository
import com.abhinav.domain.repository.LocalPlaylistRepository
import com.abhinav.domain.repository.SongRepository
import com.abhinav.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler