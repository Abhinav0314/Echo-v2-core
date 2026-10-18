package com.abhinav.logger

import kotlin.test.*

class AppErrorMessagesTest {
    private class UnknownHostException(message: String) : Exception(message)
    private class SocketTimeoutException(message: String) : Exception(message)

    @Test fun errorsIdentifyTheFeatureAcrossTheApp() {
        for ((context, title) in mapOf("LyricsCanvasRepositoryImpl.getLyrics" to "Lyrics", "SearchRepositoryImpl.search" to "Search",
            "StreamRepositoryImpl.getStream" to "Music playback", "AutoBackupWorker.doWork" to "Backup and restore",
            "EchoAccountRepository.signIn" to "Sign-in and account", "LibrarySync.pull" to "Library sync")) {
            assertTrue(AppErrorMessages.format(context).startsWith("$title:"))
        }
    }

    @Test fun typedCausesAndHttpStatusesGiveActionableReasons() {
        assertContains(AppErrorMessages.format("Search", Exception("private", UnknownHostException("secret"))), "internet connection")
        assertContains(AppErrorMessages.format("Lyrics", SocketTimeoutException("secret")), "timed out")
        assertContains(AppErrorMessages.format("Account", Exception("secret"), 401), "Sign in again")
        assertContains(AppErrorMessages.format("Lyrics", null, 404), "not found")
        assertContains(AppErrorMessages.format("Search", null, 429), "Try again later")
        assertContains(AppErrorMessages.format("Playback", null, 503), "temporarily unavailable")
    }

    @Test fun sensitiveExceptionBodiesAndUnsafeContextNeverReachTheSummary() {
        val secret = "alice@example.com access_token=private Authorization: Bearer secret https://private.invalid?key=secret"
        val result = AppErrorMessages.format(secret, IllegalStateException(secret, Exception(secret)), 500)
        listOf("alice", "private", "secret", "Bearer", "invalid", "access_token").forEach { assertFalse(it in result) }
    }

    @Test fun callerFrameIgnoresRawHeadersAndLoggerFrames() {
        assertEquals("at com.abhinav.data.repository.SearchRepositoryImpl\$search\$1.invokeSuspend", AppErrorMessages.callerContext(
            "private header\n\tat com.abhinav.logger.Logger.e(Logger.kt:10)\n\tat com.abhinav.data.repository.SearchRepositoryImpl\$search\$1.invokeSuspend(Search.kt:25)"))
    }
}
