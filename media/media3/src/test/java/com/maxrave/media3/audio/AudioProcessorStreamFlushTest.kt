package com.maxrave.media3.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.maxrave.domain.data.player.AudioEffects
import com.maxrave.domain.data.player.DelayEffect
import com.maxrave.domain.data.player.ReverbEffect
import com.maxrave.domain.data.player.ReverbPreset
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(UnstableApi::class)
class AudioProcessorStreamFlushTest {
    @Test
    fun echoFlushDropsEarlierStreamDelayButRetainsEffect() {
        assertStreamFlush {
            EchoAudioProcessor { AudioEffects(DelayEffect(20, 0.8f, 0.7f), null) }
        }
    }

    @Test
    fun equalizerFlushDropsFilterHistoryButRetainsCurve() {
        assertStreamFlush {
            EqualizerAudioProcessor { EqualizerCurve(List(10) { 6f }, -12f) }
        }
    }

    @Test
    fun crossfadeFilterFlushDropsFilterHistoryButRetainsCutoff() {
        assertStreamFlush {
            CrossfadeFilterAudioProcessor().apply {
                enabled = true
                cutoffFrequencyHz = 300f
            }
        }
    }

    @Test
    fun reverbFlushDropsBothConvolutionHistoryAndPartialDryBacklog() {
        assertStreamFlush {
            ConvolutionReverbAudioProcessor {
                AudioEffects(null, ReverbEffect(ReverbPreset.ROOM, 0.7f))
            }
        }
    }

    private fun assertStreamFlush(factory: () -> BaseAudioProcessor) {
        val processor = factory()
        val fresh = factory()
        val format = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        fresh.configure(format)
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        fresh.flush(AudioProcessor.StreamMetadata.DEFAULT)

        // One complete convolution partition plus a partial partition populates every DSP's
        // history and leaves reverb's dry backlog pending. All of this belongs to the old stream.
        val oldOutput = process(processor, ShortArray((4_096 + 127) * 2) { 12_000 })
        assertTrue("the processor must have consumed an audible old stream", oldOutput.any { it != 0.toShort() })
        processor.flush(AudioProcessor.StreamMetadata.Builder().setPositionOffsetUs(20_000_000).build())
        assertEquals("flush also discards pending output", 0, processor.output.remaining())

        val silentOutput = process(processor, ShortArray(4_096 * 2))
        assertEquals("no old partial frames may precede the new stream", 4_096 * 2, silentOutput.size)
        assertArrayEquals("old song tails must not bleed across a seek", ShortArray(silentOutput.size), silentOutput)

        // Flushing must clear only history, not disable or reset the user's effect settings.
        processor.flush(AudioProcessor.StreamMetadata.Builder().setPositionOffsetUs(25_000_000).build())
        val newSignal = ShortArray(4_096 * 2) { if (it % 8 < 4) 8_000 else -8_000 }
        assertArrayEquals(process(fresh, newSignal), process(processor, newSignal))
        processor.reset()
        fresh.reset()
    }

    private fun process(processor: AudioProcessor, samples: ShortArray): ShortArray {
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        samples.forEach { input.putShort(it) }
        input.flip()
        processor.queueInput(input)
        assertEquals("processor must consume the complete buffer", 0, input.remaining())
        val output = processor.output.order(ByteOrder.nativeOrder())
        return ShortArray(output.remaining() / 2) { output.short }
    }
}
