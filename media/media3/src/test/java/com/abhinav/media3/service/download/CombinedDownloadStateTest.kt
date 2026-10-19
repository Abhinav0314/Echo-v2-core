package com.abhinav.media3.service.download

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.abhinav.domain.data.entities.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class CombinedDownloadStateTest {
    @Test fun eitherFailedComponentMakesTheDownloadRetryable() {
        for (other in listOf(null, Download.STATE_COMPLETED, Download.STATE_DOWNLOADING, Download.STATE_FAILED)) {
            assertEquals(DownloadState.STATE_NOT_DOWNLOADED, combinedDownloadState(Download.STATE_FAILED, other))
            assertEquals(DownloadState.STATE_NOT_DOWNLOADED, combinedDownloadState(other, Download.STATE_FAILED))
        }
    }

    @Test fun completionRequiresAudioAndEveryRequestedComponent() {
        assertEquals(DownloadState.STATE_DOWNLOADED, combinedDownloadState(Download.STATE_COMPLETED, null))
        assertEquals(DownloadState.STATE_DOWNLOADED, combinedDownloadState(Download.STATE_COMPLETED, Download.STATE_COMPLETED))
        assertEquals(DownloadState.STATE_DOWNLOADING, combinedDownloadState(null, Download.STATE_COMPLETED))
        assertEquals(DownloadState.STATE_DOWNLOADING, combinedDownloadState(Download.STATE_COMPLETED, Download.STATE_DOWNLOADING))
        assertEquals(DownloadState.STATE_PREPARING, combinedDownloadState(Download.STATE_QUEUED, null))
    }
}
