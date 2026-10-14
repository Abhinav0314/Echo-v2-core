package com.maxrave.media3.exoplayer

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.media3.service.callback.SimpleMediaSessionCallback
import com.maxrave.media3.service.mediasourcefactory.MergingMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

/** Actual queue owner and controller path; the existing Cast handoff seam avoids network/audio. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(UnstableApi::class)
class PausedNextTransportTest {
    @Test
    fun controllerNextWhilePausedAdvancesAndStartsExactlyOnce() = withPlayer { fixture ->
        assertTrue(fixture.controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        fixture.controller.seekToNext()
        fixture.assertNextStarted()
    }

    @Test
    fun controllerNextMediaItemWhilePausedAdvancesAndStartsExactlyOnce() = withPlayer { fixture ->
        assertTrue(fixture.controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        fixture.controller.seekToNextMediaItem()
        fixture.assertNextStarted()
    }

    @Test
    fun appNextCommandWhilePausedAdvancesAndStartsExactlyOnce() = withPlayer { fixture ->
        fixture.adapter.skipToNextAndPlay()
        fixture.assertNextStarted()
    }

    @Test
    fun ordinaryQueueNavigationAndSeekPreservePause() = withPlayer { fixture ->
        fixture.adapter.seekToNext()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(LoadRequest(1, 0L, false)), fixture.loads)
        assertFalse(fixture.adapter.playWhenReady)
        fixture.loads.clear()
        fixture.adapter.seekTo(2, 1500L)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(LoadRequest(2, 1500L, false)), fixture.loads)
        assertFalse(fixture.adapter.playWhenReady)
    }

    @Test
    fun userNextAtQueueEndDoesNotRestartOrResume() = withPlayer { fixture ->
        fixture.adapter.seekTo(2, 0L)
        shadowOf(Looper.getMainLooper()).idle()
        fixture.loads.clear()
        fixture.adapter.skipToNextAndPlay()
        // Include direct session-player entry even if a stale controller still offers Next.
        fixture.adapter.forwardingPlayer.seekToNext()
        fixture.adapter.forwardingPlayer.seekToNextMediaItem()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fixture.loads.isEmpty())
        assertEquals(2, fixture.adapter.currentMediaItemIndex)
        assertFalse(fixture.adapter.playWhenReady)
        assertFalse(fixture.receiver.playWhenReady)
        assertEquals(0, fixture.handlerDispatches())
    }

    private data class LoadRequest(val index: Int, val position: Long, val play: Boolean)

    private class Fixture(
        val adapter: CrossfadeExoPlayerAdapter,
        val receiver: ExoPlayer,
        val controller: MediaController,
        val loads: MutableList<LoadRequest>,
        val handlerDispatches: () -> Int,
    ) {
        fun assertNextStarted() {
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf(LoadRequest(1, 0L, true)), loads)
            assertEquals(1, adapter.currentMediaItemIndex)
            assertTrue(adapter.playWhenReady)
            assertFalse("the old receiver item must not receive a separate Play command", receiver.playWhenReady)
            assertEquals("session admission must not enqueue a duplicate handler skip", 0, handlerDispatches())
        }
    }

    private fun withPlayer(test: (Fixture) -> Unit) {
        val context = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settings = interfaceStub<DataStoreManager> { method ->
            when (method) {
                "getCrossfadeDuration" -> flowOf(1000)
                else -> flowOf(DataStoreManager.FALSE)
            }
        }
        val defaultSources = DefaultMediaSourceFactory(context)
        val sources = MergingMediaSourceFactory(defaultSources, defaultSources, settings, context, interfaceStub<Cache>())
        val adapter = CrossfadeExoPlayerAdapter(context, scope, settings, sources, AudioAttributes.DEFAULT, interfaceStub<StreamRepository>())
        val receiver = ExoPlayer.Builder(context).build()
        val loads = mutableListOf<LoadRequest>()
        adapter.setCastActive(receiver, "Test receiver")
        adapter.castPlaybackRouter = { index, position, play -> loads += LoadRequest(index, position, play) }
        adapter.playWhenReady = false
        adapter.setMediaItem(item(0))
        adapter.addMediaItem(item(1))
        adapter.addMediaItem(item(2))
        // Crossfade's session player exposes one item, while the adapter owns all three.
        // No prepare call: this timeline fixture never opens a stream.
        adapter.forwardingPlayer.setMediaItem(MediaItem.fromUri("https://example.invalid/0"))
        loads.clear()
        var handlerDispatches = 0
        val handler = interfaceStub<MediaPlayerHandler> { method ->
            if (method == "onPlayerEvent") { handlerDispatches++; Unit } else null
        }
        val callback = SimpleMediaSessionCallback(
            context, scope, handler,
            interfaceStub<SearchRepository>(), interfaceStub<SongRepository>(),
            interfaceStub<LocalPlaylistRepository>(), interfaceStub<PlaylistRepository>(),
            interfaceStub<HomeRepository>(), interfaceStub<StreamRepository>(),
        )
        val session = MediaLibrarySession.Builder(context, adapter.forwardingPlayer, callback).build()
        val controllerFuture = MediaController.Builder(context, session.token).buildAsync()
        var controller: MediaController? = null
        try {
            shadowOf(Looper.getMainLooper()).idle()
            controller = controllerFuture.get(5, TimeUnit.SECONDS)
            test(Fixture(adapter, receiver, controller, loads) { handlerDispatches })
        } finally {
            controller?.release() ?: MediaController.releaseFuture(controllerFuture)
            session.release()
            adapter.release()
            receiver.release()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun item(index: Int) = GenericMediaItem("track-$index", "https://example.invalid/$index", GenericMediaMetadata.EMPTY)

    private inline fun <reified T> interfaceStub(noinline onCall: (String) -> Any? = { null }): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            onCall(method.name)
        } as T
}
