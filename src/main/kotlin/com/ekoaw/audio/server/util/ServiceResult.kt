package com.ekoaw.audio.server.util

import org.springframework.http.HttpStatusCode
import org.springframework.http.HttpStatus

sealed class ServiceResult<out T> {
    data class Success<out T>(val data: T) : ServiceResult<T>()
    data class Failure(val message: String, val code: HttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR, val error: Throwable? = null) : ServiceResult<Nothing>()

    companion object {
        fun <T> success(data: T): ServiceResult<T> = Success(data)
        fun failure(message: String, code: HttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR, error: Throwable? = null): ServiceResult<Nothing> = Failure(message, code, error)
    }
}
