package org.simpmusic.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackQueueStateTest {
    private val current = TrackInfo(id = "current", title = "Current")
    private val upcoming = TrackInfo(id = "upcoming", title = "Upcoming")
    private val original = ListenTogetherState(
        roomCode = "ROOM", currentTrack = current, isPlaying = true, position = 42_000L,
        queue = listOf(current, upcoming), queueTitle = "Original queue",
    )

    @Test
    fun explicitEmptyQueueSyncRemovesUpcomingTracksAndTitleWithoutPausingOrSeeking() {
        val cleared = original.applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.SYNC_QUEUE, queue = emptyList(), queueTitle = "", position = 0L,
        ))
        assertEquals(emptyList(), cleared.queue)
        assertEquals("", cleared.queueTitle)
        assertEquals(current, cleared.currentTrack)
        assertTrue(cleared.isPlaying)
        assertEquals(42_000L, cleared.position)
    }

    @Test
    fun playPauseAndSeekWithOmittedQueueRetainItsOrderAndTitle() {
        var state = original
        state = state.applyPlaybackAction(PlaybackActionPayload(action = PlaybackActions.PAUSE, position = 45_000L))
        assertFalse(state.isPlaying)
        state = state.applyPlaybackAction(PlaybackActionPayload(action = PlaybackActions.SEEK, position = 60_000L))
        assertFalse(state.isPlaying)
        assertEquals(60_000L, state.position)
        state = state.applyPlaybackAction(PlaybackActionPayload(action = PlaybackActions.PLAY, position = 60_000L))
        assertTrue(state.isPlaying)
        assertEquals(original.queue, state.queue)
        assertEquals("Original queue", state.queueTitle)
        assertEquals(current, state.currentTrack)
    }

    @Test
    fun trackChangeAppliesTheHostQueueAndMetadataFromTheSameCommand() {
        val next = TrackInfo(id = "new-current", title = "Next")
        val state = original.applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.CHANGE_TRACK, trackInfo = next, position = 0L,
            queue = listOf(next, upcoming), queueTitle = "Next queue", capturedAtServerTime = 100L,
        ))
        assertEquals(next, state.currentTrack)
        assertEquals(listOf(next, upcoming), state.queue)
        assertEquals("Next queue", state.queueTitle)
        assertEquals(0L, state.position)
        assertEquals(100L, state.lastActionServerTime)
    }

    @Test
    fun compensatedPlaybackUsesServerReceiptTimeInsteadOfCountingHostTransitTwice() {
        val state = original.applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.PLAY, position = 45_200L,
            capturedAtServerTime = 1_000L, serverTime = 1_200L,
        ))
        assertEquals(45_200L, state.position)
        assertEquals(1_200L, state.lastActionServerTime)
    }

    @Test
    fun queueOnlyUpdatePreservesPlaybackTimingAnchor() {
        val state = original.copy(lastActionServerTime = 1_000L).applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.SYNC_QUEUE, serverTime = 2_000L, capturedAtServerTime = 1_900L,
        ))
        assertEquals(1_000L, state.lastActionServerTime)
    }

    @Test
    fun authoritativeSyncClearsDeletedQueueAndReplacesStaleClockAnchor() {
        val state = original.copy(lastActionServerTime = 1_000L).applySyncState(SyncStatePayload(
            currentTrack = current, position = 50_000L, isPlaying = true, lastUpdate = 5_000L,
            queue = emptyList(),
        ))
        assertEquals(emptyList(), state.queue)
        assertEquals("", state.queueTitle)
        assertEquals(5_000L, state.lastActionServerTime)
        assertEquals(50_000L, state.position)
    }

    @Test
    fun changeTrackPreparesPausedItemUntilExplicitHostPlay() {
        val changed = original.applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.CHANGE_TRACK, trackInfo = upcoming,
        ))
        assertFalse(changed.isPlaying)
        assertTrue(changed.applyPlaybackAction(PlaybackActionPayload(action = PlaybackActions.PLAY)).isPlaying)
    }

    @Test
    fun metrolistQueueAddDoesNotChangeCurrentSongOrRestartThePlayhead() {
        val state = original.copy(lastActionServerTime = 1_000L).applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.QUEUE_ADD, trackInfo = upcoming, queue = listOf(upcoming), serverTime = 2_000L,
        ))
        assertEquals(current, state.currentTrack)
        assertEquals(original.position, state.position)
        assertEquals(1_000L, state.lastActionServerTime)
        assertTrue(state.isPlaying)
    }

    @Test
    fun clearingUpcomingTracksDoesNotRestartOrPauseCurrentSong() {
        val state = original.copy(lastActionServerTime = 1_000L).applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.QUEUE_CLEAR, serverTime = 2_000L,
        ))
        assertEquals(emptyList(), state.queue)
        assertEquals(current, state.currentTrack)
        assertEquals(original.position, state.position)
        assertEquals(1_000L, state.lastActionServerTime)
        assertTrue(state.isPlaying)
    }

    @Test
    fun replacingWithUnnamedQueueDoesNotKeepThePreviousPlaylistsTitle() {
        val state = original.applyPlaybackAction(PlaybackActionPayload(
            action = PlaybackActions.SYNC_QUEUE, queue = listOf(current), queueTitle = "",
        ))
        assertEquals(listOf(current), state.queue)
        assertEquals("", state.queueTitle)
    }
}
