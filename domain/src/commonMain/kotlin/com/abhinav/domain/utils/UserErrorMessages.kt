package com.abhinav.domain.utils

/** Exception descriptions can include backend bodies and private request data. Never display them. */
fun userErrorMessage(failure: Throwable): String {
    val names=generateSequence(failure){it.cause}.take(6).map{it::class.simpleName}.toSet()
    return when {
        names.any{it?.contains("Timeout")==true} -> "This is taking longer than expected. Please try again."
        names.any{it in setOf("SQLiteFullException","SQLiteDiskIOException")} -> "Couldn't save your changes. Check your device's available storage and try again."
        names.any{it in NETWORK_ERROR_TYPES} -> "Couldn't connect. Check your internet connection and try again."
        else -> "Something went wrong. Please try again."
    }
}

private val NETWORK_ERROR_TYPES=setOf("UnknownHostException","UnresolvedAddressException","ConnectException","IOException","SocketException","HttpRequestException")
