package com.abhinav.data.di.loader

import com.abhinav.echo.media_jvm.di.loadDesktopPlayerModule
import com.abhinav.data.loginsync.LoginSyncHostRepositoryImpl
import com.abhinav.data.loginsync.LoginSyncStore
import com.abhinav.domain.repository.LoginSyncHostRepository
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncHostRepository> { LoginSyncHostRepositoryImpl(get()) }
        },
    )
}
