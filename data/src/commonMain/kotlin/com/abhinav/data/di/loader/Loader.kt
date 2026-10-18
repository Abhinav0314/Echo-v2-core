package com.abhinav.data.di.loader

import com.abhinav.common.AppIdentity
import com.abhinav.data.di.databaseModule
import com.abhinav.data.di.listenTogetherModule
import com.abhinav.data.di.mediaHandlerModule
import com.abhinav.data.di.repositoryModule
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

fun loadAllModules(appIdentity: AppIdentity) {
    loadKoinModules(
        listOf(
            module { single { appIdentity } },
            databaseModule,
            repositoryModule,
        ),
    )
    loadKoinModules(mediaHandlerModule)
    // NOTE: `createdAtStart` is NOT enough for a module loaded this way — nothing constructs it
    // unless something injects it, and the bridge exists purely to listen, so nobody would.
    // ListenTogetherViewModel injects and starts it; start() is idempotent.
    loadKoinModules(listenTogetherModule)
    loadMediaService()
    loadLoginSyncModule()
}

expect fun loadMediaService()

/** Upstream desktop login transfer seam; Echo Android and iOS take no part. */
expect fun loadLoginSyncModule()
