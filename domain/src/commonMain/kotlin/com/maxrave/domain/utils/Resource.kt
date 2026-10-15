package com.maxrave.domain.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(
        data: T,
    ) : Resource<T>(data)

    class Error<T>(
        message: String,
        data: T? = null,
    ) : Resource<T>(data, message)
}

/**
 * New resource class that is used to wrap the data that is returned from the manager (only local data sources)
 * 3 state
 * Success, Error, Loading
 */
sealed class LocalResource<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(
        data: T,
    ) : LocalResource<T>(data)

    class Error<T>(
        message: String,
        data: T? = null,
    ) : LocalResource<T>(data, message)

    class Loading<T> : LocalResource<T>()
}

sealed class NoResponseResource(
    val message: String? = null,
) {
    class Success : NoResponseResource()

    class Error(
        message: String,
    ) : NoResponseResource(message)

    class Loading : NoResponseResource()
}

suspend fun <T> Flow<LocalResource<T>>.collectResource(
    onSuccess: (T?) -> Unit,
    onError: ((String) -> Unit) = {},
    onLoading: (() -> Unit) = {},
) = this.collect { resource ->
    when (resource) {
        is LocalResource.Success -> onSuccess(resource.data)
        is LocalResource.Error -> onError(resource.message ?: "Something went wrong. Please try again.")
        is LocalResource.Loading -> onLoading()
    }
}

suspend fun Flow<NoResponseResource>.collectNoResponseResource(
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.collect { resource ->
    when (resource) {
        is NoResponseResource.Success -> onSuccess()
        is NoResponseResource.Error -> onError(resource.message ?: "Something went wrong. Please try again.")
        is NoResponseResource.Loading -> onLoading()
    }
}

suspend fun <T> Flow<LocalResource<T>>.collectLatestResource(
    onSuccess: (T?) -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.collectLatest { resource ->
    when (resource) {
        is LocalResource.Success -> onSuccess(resource.data)
        is LocalResource.Error -> onError(resource.message ?: "Something went wrong. Please try again.")
        is LocalResource.Loading -> onLoading()
    }
}

suspend fun Flow<NoResponseResource>.collectLatestNoResponseResource(
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.collectLatest { resource ->
    when (resource) {
        is NoResponseResource.Success -> onSuccess()
        is NoResponseResource.Error -> onError(resource.message ?: "Something went wrong. Please try again.")
        is NoResponseResource.Loading -> onLoading()
    }
}

fun wrapMessageResource(
    successMessage: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> Unit,
): Flow<LocalResource<String>> =
    flow {
        emit(LocalResource.Loading())
        runCatching { block.invoke() }
            .onSuccess {
                emit(LocalResource.Success(successMessage))
            }.onFailure {
                if (it is CancellationException && it !is TimeoutCancellationException) throw it
                emit(LocalResource.Error<String>(userErrorMessage(it)))
            }
    }.flowOn(dispatcher)

fun <T> wrapDataResource(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> T,
): Flow<LocalResource<T>> =
    flow {
        emit(LocalResource.Loading())
        runCatching { block.invoke() }
            .onSuccess {
                emit(LocalResource.Success(it))
            }.onFailure {
                if (it is CancellationException && it !is TimeoutCancellationException) throw it
                emit(LocalResource.Error<T>(userErrorMessage(it)))
            }
    }.flowOn(dispatcher)

// For one time emit
fun <T> wrapResultResource(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> Result<T>,
): Flow<LocalResource<T>> =
    flow {
        emit(LocalResource.Loading())
        block
            .invoke()
            .onSuccess {
                emit(LocalResource.Success(it))
            }.onFailure {
                if (it is CancellationException && it !is TimeoutCancellationException) throw it
                emit(LocalResource.Error<T>(userErrorMessage(it)))
            }
    }.flowOn(dispatcher)

suspend fun forceNoReturn(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> Unit,
) = withContext(dispatcher) {
    block.invoke()
}
