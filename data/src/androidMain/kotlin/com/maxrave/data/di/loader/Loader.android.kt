package com.maxrave.data.di.loader

import com.maxrave.media3.di.loadMediaService

actual fun loadMediaService() {
    loadMediaService()
}

actual fun loadLoginSyncModule() {
    // The common loader retains its upstream platform seam. Echo Android authenticates
    // natively and never constructs a desktop credential store or transfer transport.
}
