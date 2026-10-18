package com.abhinav.kotlinytmusicscraper.extractor

import dev.maxrave.pipepipe.extractor.NewPipe
import dev.maxrave.pipepipe.extractor.downloader.CancellableCall
import dev.maxrave.pipepipe.extractor.downloader.Downloader
import dev.maxrave.pipepipe.extractor.downloader.Request
import dev.maxrave.pipepipe.extractor.downloader.Response
import dev.maxrave.pipepipe.extractor.localization.ContentCountry
import dev.maxrave.pipepipe.extractor.localization.Localization
import javax.net.ssl.HttpsURLConnection
import kotlin.test.Test
import kotlin.test.assertSame

class PipePipeTlsInitializationTest {
    private val downloader = object : Downloader() {
        override fun execute(request: Request): Response =
            error("Extractor initialization must not issue a network request")

        override fun executeAsync(request: Request, callback: AsyncCallback): CancellableCall =
            error("Extractor initialization must not issue an asynchronous network request")
    }

    @Test
    fun everyInitializationOverloadPreservesPlatformHttpsValidation() {
        val hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        val socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
        val localization = Localization("en", "US")
        val country = ContentCountry("US")
        val initialize = listOf<() -> Unit>(
            { NewPipe.init(downloader) },
            { NewPipe.init(downloader, localization) },
            { NewPipe.init(downloader, localization, country) },
        )
        for (init in initialize) {
            init()
            assertSame(downloader, NewPipe.getDownloader())
            assertSame(hostnameVerifier, HttpsURLConnection.getDefaultHostnameVerifier())
            assertSame(socketFactory, HttpsURLConnection.getDefaultSSLSocketFactory())
        }
        assertSame(localization, NewPipe.getPreferredLocalization())
        assertSame(country, NewPipe.getPreferredContentCountry())
    }

    @Test
    fun legacyCompatibilityEntryPointCannotDisableHttpsValidation() {
        val hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        val socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
        NewPipe.trustEveryone()
        assertSame(hostnameVerifier, HttpsURLConnection.getDefaultHostnameVerifier())
        assertSame(socketFactory, HttpsURLConnection.getDefaultSSLSocketFactory())
    }
}
