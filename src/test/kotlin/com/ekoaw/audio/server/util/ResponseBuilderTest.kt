package com.ekoaw.audio.server.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ResponseBuilderTest {
    @Test
    fun `success should return response with success status`() {
        val response = ResponseBuilder.success()
        assertEquals("success", response.status)
        assertEquals("success", response.message)
    }

    @Test
    fun `success should return response with custom message`() {
        val customMessage = "Operation completed"
        val response = ResponseBuilder.success(customMessage)
        assertEquals("success", response.status)
        assertEquals(customMessage, response.message)
    }

    @Test
    fun `error should return response with error status`() {
        val response = ResponseBuilder.error()
        assertEquals("error", response.status)
        assertEquals("error", response.message)
    }

    @Test
    fun `error should return response with custom message`() {
        val customMessage = "Something went wrong"
        val response = ResponseBuilder.error(customMessage)
        assertEquals("error", response.status)
        assertEquals(customMessage, response.message)
    }
}
