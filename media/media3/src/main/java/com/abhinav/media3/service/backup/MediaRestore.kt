package com.abhinav.media3.service.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import com.abhinav.common.Config.CANVAS_CACHE
import com.abhinav.common.Config.DOWNLOAD_CACHE
import com.abhinav.common.Config.PLAYER_CACHE
import com.abhinav.common.Config.SERVICE_SCOPE
import com.abhinav.domain.mediaservice.handler.DownloadHandler
import com.abhinav.domain.mediaservice.handler.MediaPlayerHandler
import com.abhinav.domain.mediaservice.player.MediaPlayerInterface
import com.abhinav.media3.service.EchoMediaService
import com.abhinav.media3.service.download.DownloadUtils
import com.abhinav.media3.service.download.EchoDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin

/** Only call after the archive was completely validated; restart after restore or failure. */
@OptIn(UnstableApi::class)
suspend fun quiesceMediaForRestore(context: Context) {
    val koin = getKoin()
    withContext(Dispatchers.Main) {
        context.stopService(Intent(context, com.maxrave.media3.service.download.MusicDownloadService::class.java))
        context.stopService(Intent(context, com.maxrave.media3.service.SimpleMediaService::class.java))
        koin.getOrNull<MediaPlayerHandler>()?.release()
        koin.getOrNull<MediaPlayerInterface>()?.let { player ->
            player.pause()
            player.stop()
            player.release()
        }
    }
    (koin.getOrNull<DownloadHandler>() as? DownloadUtils)?.quiesceForRestore()
    koin.getOrNull<CoroutineScope>(named(SERVICE_SCOPE))?.coroutineContext?.get(Job)?.let {
        it.cancel()
        it.join()
    }
    withContext(Dispatchers.IO) {
        listOf(DOWNLOAD_CACHE, PLAYER_CACHE, CANVAS_CACHE).forEach { name ->
            koin.getOrNull<SimpleCache>(named(name))?.release()
        }
        (koin.getOrNull<DatabaseProvider>() as? SQLiteOpenHelper)?.close()
    }
}
