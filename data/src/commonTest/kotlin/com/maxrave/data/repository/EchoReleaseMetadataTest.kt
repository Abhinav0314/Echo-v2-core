package com.maxrave.data.repository

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EchoReleaseMetadataTest {
    private val row = """{"version_code":20009,"version_name":"2.0.9","apk_url":"https://echo.vibescape.club/echo-v2.apk","application_id":"club.vibescape.echo","release_channel":"echo-v2"}"""

    @Test fun catalogCreationTimeSurvivesTheUpdateMapping() {
        val timestamp = "2026-10-18T10:23:00+00:00"
        val metadata = Json.decodeFromString<EchoReleaseMetadata>(row.dropLast(1) + ",\"created_at\":\"$timestamp\"}")
        val update = metadata.toUpdateData(forceUpdate = true)
        assertEquals(timestamp, update.releaseTime)
        assertEquals("2.0.9", update.tagName)
        assertTrue(update.forceUpdate)
    }

    @Test fun olderCatalogRowsWithoutADateRemainCompatible() {
        assertNull(Json.decodeFromString<EchoReleaseMetadata>(row).toUpdateData(false).releaseTime)
    }
}
