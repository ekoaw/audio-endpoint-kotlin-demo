﻿package com.ekoaw.audio.server.util

import com.ekoaw.audio.server.config.AudioConversionConfig
import com.ekoaw.audio.server.model.request.AudioRequestModel
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AudioConverterTest {
    private lateinit var audioConverter: AudioConverter
    private lateinit var audioConversionConfig: AudioConversionConfig
    private lateinit var processMock: Process
    private lateinit var inputFile: File
    private lateinit var audioRequestModel: AudioRequestModel
    private lateinit var tempFileMock: File

    @BeforeEach
    fun setUp() {
        // Mocking the AudioConversionConfig
        audioConversionConfig = mockk()

        // Mock the ProcessBuilder constructor
        mockkConstructor(ProcessBuilder::class)

        // Creating AudioConverter with mocked AudioConversionConfig
        audioConverter = AudioConverter(audioConversionConfig)

        // Mocking Process
        processMock = mockk()

        // Mocking File.createTempFile() to return a mock file
        tempFileMock = mockk(relaxed = true)
        mockkStatic(File::class)
        every { File.createTempFile(any(), any()) } returns tempFileMock

        // Spying on ProcessBuilder and mocking the start() method
        mockkStatic(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } returns processMock

        inputFile = File("input.wav")
        audioRequestModel = AudioRequestModel(userId = 1, phraseId = 1001)

        // Mocking the extension configuration
        every { audioConversionConfig.extensions["m4a"] } returns listOf("-c:a", "aac", "-b:a", "128k")
    }

    @Test
    fun `test audio conversion success`() {
        // Mocking the process to simulate success
        every { processMock.waitFor() } returns 0 // Simulating success
        every { processMock.exitValue() } returns 0

        // Call the method to test and check that no exception is thrown
        audioConverter.convertAudioFile(audioRequestModel, inputFile, "m4a")

        // Verifying that the process was executed
        verify { processMock.waitFor() }
    }

    @Test
    fun `test audio conversion failure`() {
        // Mocking the process to simulate failure
        every { processMock.waitFor() } returns 1 // Simulating failure
        every { processMock.exitValue() } returns 1

        // Expecting an exception
        assertFailsWith<Exception>("Audio conversion failed") {
            audioConverter.convertAudioFile(audioRequestModel, inputFile, "m4a")
        }

        verify { processMock.waitFor() }
    }

    @Test
    fun `test unsupported extension`() {
        // Testing invalid extension handling
        val invalidExtension = "xyz"

        val exception = assertFailsWith<IllegalArgumentException> {
            audioConverter.convertAudioFile(audioRequestModel, inputFile, invalidExtension)
        }

        assertTrue(exception.message?.contains("Unsupported extension") == true)
    }
}
