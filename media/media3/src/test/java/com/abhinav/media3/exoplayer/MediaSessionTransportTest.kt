package com.abhinav.media3.exoplayer

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.abhinav.domain.mediaservice.handler.MediaPlayerHandler
import com.abhinav.domain.repository.HomeRepository
import com.abhinav.domain.repository.LocalPlaylistRepository
import com.abhinav.domain.repository.PlaylistRepository
import com.abhinav.domain.repository.SearchRepository
import com.abhinav.domain.repository.SongRepository
import com.abhinav.domain.repository.StreamRepository
import com.abhinav.media3.service.callback.SimpleMediaSessionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

/** Real controller/session routing: command admission must not schedule a second skip. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(UnstableApi::class)
class MediaSessionTransportTest {
    @Test
    fun controllerNextDispatchesExactlyOnceToThePlaylistOwner() = assertSingleDispatch(next = true)

    @Test
    fun controllerPreviousDispatchesExactlyOnceToThePlaylistOwner() = assertSingleDispatch(next = false)

    private fun assertSingleDispatch(next: Boolean) {
        val context = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val delegate = ExoPlayer.Builder(context).build()
        delegate.setMediaItems(List(3) { MediaItem.fromUri("https://example.invalid/$it") }, 1, 0L)
        var nextDispatches = 0
        var previousDispatches = 0
        var handlerDispatches = 0
        val forwardingPlayer = DelegatingForwardingPlayer(delegate).apply {
            playlistNavigationProvider = object : DelegatingForwardingPlayer.PlaylistNavigationProvider {
                override fun hasNextMediaItem() = true
                override fun hasPreviousMediaItem() = true
                override fun seekToNext() { nextDispatches++ }
                override fun seekToPrevious() { previousDispatches++ }
                override fun play() = Unit
                override fun pause() = Unit
                override fun setPlayWhenReady(playWhenReady: Boolean) = Unit
            }
        }
        val handler = interfaceStub<MediaPlayerHandler> { method ->
            if (method == "onPlayerEvent") {
                handlerDispatches++
                Unit
            } else null
        }
        val callback = SimpleMediaSessionCallback(
            context, scope, handler,
            interfaceStub<SearchRepository>(), interfaceStub<SongRepository>(),
            interfaceStub<LocalPlaylistRepository>(), interfaceStub<PlaylistRepository>(),
            interfaceStub<HomeRepository>(), interfaceStub<StreamRepository>(),
        )
        val session = MediaLibrarySession.Builder(context, forwardingPlayer, callback).build()
        val controllerFuture = MediaController.Builder(context, session.token).buildAsync()
        var controller: MediaController? = null
        try {
            shadowOf(Looper.getMainLooper()).idle()
            controller = controllerFuture.get(5, TimeUnit.SECONDS)
            val command = if (next) Player.COMMAND_SEEK_TO_NEXT else Player.COMMAND_SEEK_TO_PREVIOUS
            assertTrue("the real controller must offer the transport command", controller.isCommandAvailable(command))
            if (next) controller.seekToNext() else controller.seekToPrevious()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(if (next) 1 else 0, nextDispatches)
            assertEquals(if (next) 0 else 1, previousDispatches)
            assertEquals("session admission must not enqueue another handler skip", 0, handlerDispatches)
        } finally {
            controller?.release() ?: MediaController.releaseFuture(controllerFuture)
            session.release()
            delegate.release()
            scope.cancel()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private inline fun <reified T> interfaceStub(noinline onCall: (String) -> Any? = { null }): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            onCall(method.name)
        } as T
}
