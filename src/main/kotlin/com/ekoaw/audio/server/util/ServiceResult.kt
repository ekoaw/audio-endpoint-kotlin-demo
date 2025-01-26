package com.ekoaw.audio.server.util

sealed class ServiceResult<out T> {
    data class Success<out T>(val data: T) : ServiceResult<T>()
    data class Failure(val message: String, val error: Throwable? = null) : ServiceResult<Nothing>()

    companion object {
        fun <T> success(data: T): ServiceResult<T> = Success(data)
        fun failure(message: String, error: Throwable? = null): ServiceResult<Nothing> = Failure(message, error)
    }
}
