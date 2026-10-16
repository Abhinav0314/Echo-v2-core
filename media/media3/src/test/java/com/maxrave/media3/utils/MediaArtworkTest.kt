package com.maxrave.media3.utils

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@androidx.annotation.OptIn(UnstableApi::class)
class MediaArtworkTest {
    @Test
    fun unavailableHighResolutionArtHasASameSongFallback() {
        assertEquals(
            Uri.parse("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"),
            youtubeArtworkFallback(Uri.parse("https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/maxresdefault.webp")),
        )
        assertNull(youtubeArtworkFallback(Uri.parse("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")))
        assertNull(youtubeArtworkFallback(Uri.parse("https://example.org/vi/dQw4w9WgXcQ/maxresdefault.jpg")))
        assertNull(youtubeArtworkFallback(Uri.parse("https://i.ytimg.com/vi/invalid/maxresdefault.jpg")))
    }

    @Test
    fun sessionLoaderProvidesBoundedUsableArtwork() {
        val context = RuntimeEnvironment.getApplication()
        val player = ExoPlayer.Builder(context).build()
        val bitmap = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        val loader = object : BitmapLoader {
            override fun supportsMimeType(mimeType: String) = true
            override fun decodeBitmap(data: ByteArray) = Futures.immediateFuture(bitmap)
            override fun loadBitmap(uri: Uri) = Futures.immediateFuture(bitmap)
        }
        val session = MediaSession.Builder(context, player)
            .setId("artwork-regression")
            .setBitmapLoader(sizeLimitedForSession(context, loader))
            .build()
        try {
            val cover = session.bitmapLoader.loadBitmap(Uri.parse("https://example.invalid/cover.jpg"))
                .get(5, TimeUnit.SECONDS)
            assertNotNull(cover)
            assertTrue(cover.width < 2048 && cover.height < 2048)
            assertTrue(!cover.isRecycled)
        } finally {
            session.release()
            player.release()
        }
    }
}
