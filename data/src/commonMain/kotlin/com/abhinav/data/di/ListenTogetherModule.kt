package com.abhinav.data.di

import com.abhinav.common.AppIdentity
import club.vibescape.echo.contracts.EchoListenTogetherAuth
import club.vibescape.echo.contracts.EchoSessionSource
import com.abhinav.common.Config
import com.abhinav.data.listentogether.ListenTogetherPlaybackBridge
import com.abhinav.data.listentogether.ListenTogetherRepositoryImpl
import com.abhinav.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.abhinav.echo.listentogether.ListenTogetherClient
import org.abhinav.echo.listentogether.ListenTogetherSession
import org.abhinav.echo.listentogether.ListenTogetherTransport

/**
 * Listen Together lives for as long as the app does, not as long as its screen.
 *
 * The session was originally created inside the ViewModel, which meant leaving the screen tore the
 * socket down and left the room — while playback, the thing the room is about, kept running in the
 * background. Singletons here, and the screen merely observes them.
 */
val listenTogetherModule =
    module {
        single {
            ListenTogetherClient(
                clientVersion = "Echo",
                userAgent = get<AppIdentity>().userAgent,
                // Old custom/community-server preferences are deliberately not read:
                // Echo account credentials must never be sent to those endpoints.
                accessToken = { get<EchoListenTogetherAuth>().listenTogetherAccessToken() },
            )
        } bind ListenTogetherTransport::class

        single { ListenTogetherSession(client = get()) }

        // The boundary: everything above this line speaks domain types only.
        single<ListenTogetherRepository> {
            ListenTogetherRepositoryImpl(
                session = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
                account = get<EchoSessionSource>(),
            )
        }

        // createdAtStart: the bridge has to be listening before the user joins anything, otherwise
        // the first track change of a room is missed and the guest sits in silence.
        single(createdAtStart = true) {
            ListenTogetherPlaybackBridge(
                repository = get(),
                session = get(),
                handler = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            ).also { it.start() }
        }
    }
