package com.ekoaw.audio.server.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus

class ServiceResultTest {
    @Test
    fun `success result should contain correct data`() {
        val result = ServiceResult.success("Test Data")
        assertEquals(ServiceResult.Success("Test Data"), result)
    }

    @Test
    fun `failure result should contain correct message and default values`() {
        val result = ServiceResult.failure("An error occurred")
        assertEquals(ServiceResult.Failure(
            message = "An error occurred",
            code = HttpStatus.INTERNAL_SERVER_ERROR,
            error = null
        ), result)
    }

    @Test
    fun `failure result should contain custom status code and error`() {
        val exception = RuntimeException("Test Exception")
        val result = ServiceResult.failure("Custom error", HttpStatus.BAD_REQUEST, exception)
        assertEquals(ServiceResult.Failure(
            message = "Custom error",
            code = HttpStatus.BAD_REQUEST,
            error = exception
        ), result)
    }
}
