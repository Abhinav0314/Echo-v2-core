package com.maxrave.media3.service.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteOpenHelper
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.common.Config.CANVAS_CACHE
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.PLAYER_CACHE
import com.maxrave.common.Config.SERVICE_SCOPE
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import com.maxrave.media3.service.SimpleMediaService
import com.maxrave.media3.service.download.DownloadUtils
import com.maxrave.media3.service.download.MusicDownloadService
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
        context.stopService(Intent(context, MusicDownloadService::class.java))
        context.stopService(Intent(context, SimpleMediaService::class.java))
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
