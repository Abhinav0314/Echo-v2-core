package com.abhinav.data.echo

import DatabaseDao
import club.vibescape.echo.contracts.EchoLibraryHooks

/**
 * Always acquire the account lease before Room's write transaction. Projection follows the
 * same order, so startup cannot deadlock and a cloud snapshot cannot overwrite a newer like
 * or playlist edit. Nested DAO mutations reuse both the lease and Room transaction.
 */
internal suspend fun <T> DatabaseDao.echoLibraryMutation(block: suspend () -> T): T =
    EchoLibraryHooks.transaction {
        var result: Result<T>? = null
        echoApplyProjection { result = Result.success(block()) }
        checkNotNull(result).getOrThrow()
    }
