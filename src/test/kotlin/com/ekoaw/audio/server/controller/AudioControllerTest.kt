package com.ekoaw.audio.server.controller

import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.model.response.ResponseModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.service.AudioService
import com.ekoaw.audio.server.util.ServiceResult
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

class AudioControllerUnitTest {

  private val audioService: AudioService = mockk()
  private val userRepository: UserRepository = mockk()
  private val phraseRepository: PhraseRepository = mockk()

  private val controller = AudioController(audioService, userRepository, phraseRepository)

  @Test
  fun `getAudio should return file when service succeeds`() {
    val userId = 1
    val phraseId = 1
    val audioFormat = "m4a"
    val audioFile = File.createTempFile("temp", ".m4a")
    val requestModel = AudioRequestModel(userId, phraseId)

    audioFile.deleteOnExit()

    every { audioService.downloadAudioFile(requestModel, audioFormat) } returns
      ServiceResult.Success(FileSystemResource(audioFile))

    val response = controller.getAudio(userId, phraseId, audioFormat)

    assertEquals(HttpStatus.OK, response.statusCode)
    assertEquals(
      "attachment; filename=\"${audioFile.name}\"",
      response.headers["Content-Disposition"]?.first(),
    )
    assertNotNull(response.body)
    assertTrue(response.body is Resource)
  }

  @Test
  fun `getAudio should return error response when service fails`() {
    val userId = 1
    val phraseId = 1
    val audioFormat = "m4a"
    val requestModel = AudioRequestModel(userId, phraseId)

    every { audioService.downloadAudioFile(requestModel, audioFormat) } returns
      ServiceResult.Failure("File not found", HttpStatus.NOT_FOUND)

    val response = controller.getAudio(userId, phraseId, audioFormat)

    assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    assertNotNull(response.body)
    assertTrue(response.body is ResponseModel)
    assertEquals("File not found", (response.body as ResponseModel).message)
  }

  @Test
  fun `postAudio should return success response when upload succeeds`() {
    val userId = 1
    val phraseId = 1
    val file = MockMultipartFile("audio_file", "test.wav", "audio/wav", byteArrayOf(1, 2, 3))
    val requestModel = AudioRequestModel(userId, phraseId)
    val userPhraseFile = UserPhraseFileModel()

    every { audioService.uploadAudioFile(requestModel, file) } returns
      ServiceResult.Success(userPhraseFile)

    val response = controller.postAudio(userId, phraseId, file)

    assertEquals(HttpStatus.OK, response.statusCode)
    assertNotNull(response.body)
    assertEquals("File uploaded successfully", response.body?.message)
  }

  @Test
  fun `postAudio should return error response when upload fails`() {
    val userId = 1
    val phraseId = 1
    val file = MockMultipartFile("audio_file", "test.wav", "audio/wav", byteArrayOf(1, 2, 3))
    val requestModel = AudioRequestModel(userId, phraseId)

    every { audioService.uploadAudioFile(requestModel, file) } returns
      ServiceResult.Failure("Upload failed", HttpStatus.BAD_REQUEST)

    val response = controller.postAudio(userId, phraseId, file)

    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    assertNotNull(response.body)
    assertEquals("Upload failed", response.body?.message)
  }
}
