package com.maxrave.data.di

import com.maxrave.common.AppIdentity
import kotlin.test.Test
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.simpmusic.listentogether.ListenTogetherClient
import org.simpmusic.listentogether.ListenTogetherSession
import org.simpmusic.listentogether.ListenTogetherTransport

class ListenTogetherModuleTest {
    @Test
    fun productionModuleResolvesSessionWithTheRegisteredClient() {
        // Resolve the real session through Koin: directly constructing it with a fake
        // transport misses a missing interface binding and lets a launch crash ship.
        val application = koinApplication(createEagerInstances = false) {
            modules(
                module { single { AppIdentity("club.vibescape.echo", "test", "Android") } },
                listenTogetherModule,
            )
        }
        var session: ListenTogetherSession? = null
        try {
            session = application.koin.get<ListenTogetherSession>()
            assertSame(session, application.koin.get<ListenTogetherSession>())
            assertSame(
                application.koin.get<ListenTogetherClient>(),
                application.koin.get<ListenTogetherTransport>(),
            )
        } finally {
            session?.release()
            application.close()
        }
    }
}
