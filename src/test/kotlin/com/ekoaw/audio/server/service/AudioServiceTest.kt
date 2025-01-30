package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.AudioConversionConfig
import com.ekoaw.audio.server.model.entity.PhraseModel
import com.ekoaw.audio.server.model.entity.UserModel
import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserPhraseFileRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.util.ServiceResult
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.test.assertEquals
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile

@ExtendWith(MockKExtension::class)
class AudioServiceTest {

  @MockK private lateinit var userRepository: UserRepository
  @MockK private lateinit var phraseRepository: PhraseRepository
  @MockK private lateinit var userPhraseFileRepository: UserPhraseFileRepository
  @MockK private lateinit var storageService: StorageService
  @MockK private lateinit var converterService: ConverterService
  @MockK private lateinit var audioConversionConfig: AudioConversionConfig
  @MockK private lateinit var executor: ExecutorService
  @InjectMockKs private lateinit var audioService: AudioService

  @BeforeEach
  fun setUp() {
    // Mock the executor to run tasks synchronously
    every { executor.submit(any()) } answers
      {
        val task = firstArg<Runnable>()
        task.run()
        mockk() // Return a mock Future
      }
  }

  @Test
  fun `uploadAudioFile should return failure for invalid file extension`() {
    // Arrange
    val multipartFile = mockk<MultipartFile>()
    val info = AudioRequestModel(userId = 1, phraseId = 1)

    every { multipartFile.originalFilename } returns "test.mp3"

    // Act
    val result = audioService.uploadAudioFile(info, multipartFile)

    // Assert
    assertEquals(
      ServiceResult.failure(
        "Invalid file extension, should be .m4a",
        HttpStatus.PRECONDITION_FAILED,
      ),
      result,
    )
  }

  @Test
  fun `uploadAudioFile should return failure if user not found`() {
    // Arrange
    val multipartFile = mockk<MultipartFile>()
    val info = AudioRequestModel(userId = 1, phraseId = 1)

    every { multipartFile.originalFilename } returns "test.m4a"
    every { userRepository.findById(info.userId) } returns Optional.empty()

    // Act
    val result = audioService.uploadAudioFile(info, multipartFile)

    // Assert
    assertEquals(ServiceResult.failure("User 1 not found.", HttpStatus.PRECONDITION_FAILED), result)
  }

  @Test
  fun `uploadAudioFile should return failure if phrase not found`() {
    // Arrange
    val multipartFile = mockk<MultipartFile>()
    val info = AudioRequestModel(userId = 1, phraseId = 1)
    val user = mockk<UserModel>()

    every { multipartFile.originalFilename } returns "test.m4a"
    every { userRepository.findById(info.userId) } returns Optional.of(user)
    every { phraseRepository.findById(info.phraseId) } returns Optional.empty()

    // Act
    val result = audioService.uploadAudioFile(info, multipartFile)

    // Assert
    assertEquals(
      ServiceResult.failure("Phrase 1 not found.", HttpStatus.PRECONDITION_FAILED),
      result,
    )
  }

  @Test
  fun `uploadAudioFile should upload file and return success`() {
    // Arrange
    val multipartFile = mockk<MultipartFile>()
    val info = AudioRequestModel(userId = 1, phraseId = 1)
    val user = mockk<UserModel>()
    val phrase = mockk<PhraseModel>()
    val inputFile = File.createTempFile("temp", ".m4a")
    val outputFile = File.createTempFile("temp", ".wav")
    val userPhraseFile = mockk<UserPhraseFileModel>()

    // Make sure temp files will be deleted
    inputFile.deleteOnExit()
    outputFile.deleteOnExit()

    // Mock static FileUtils.copyInputStreamToFile
    mockkStatic(FileUtils::class)
    every { FileUtils.copyInputStreamToFile(any(), any()) } just Runs

    // Mock multipart file behavior
    every { multipartFile.inputStream } returns mockk(relaxed = true)
    every { multipartFile.originalFilename } returns "temp.m4a"

    // Mock Model id property
    every { user.id } returns info.userId
    every { phrase.id } returns info.phraseId
    every { userPhraseFile.id } returns 1

    // Mock repositories
    every { userRepository.findById(any()) } returns Optional.of(user)
    every { phraseRepository.findById(any()) } returns Optional.of(phrase)

    // Mock services
    every { converterService.convertAudioFile(any(), any(), any()) } returns outputFile
    every { storageService.uploadFile(any(), any(), any()) } just Runs
    every { userPhraseFileRepository.save(any()) } returns userPhraseFile
    every {
      userPhraseFileRepository
        .findByUserIdAndPhraseIdAndDeletedAtIsNullAndIdLessThanOrderByCreatedAtDesc(
          any(),
          any(),
          any(),
        )
    } returns emptyList()

    // Act
    val result = audioService.uploadAudioFile(info, multipartFile)

    // Assert
    assertEquals(ServiceResult.success(userPhraseFile), result)
    verify { storageService.uploadFile(outputFile.name, any(), "audio/wav") }
  }

  @Test
  fun `downloadAudioFile should return failure for unsupported audio format`() {
    // Arrange
    val info = AudioRequestModel(userId = 1, phraseId = 1)
    val audioFormat = "xyz"

    every { audioConversionConfig.extensions.containsKey(audioFormat) } returns false

    // Act
    val result = audioService.downloadAudioFile(info, audioFormat)

    // Act & Assert
    assertEquals(
      ServiceResult.failure(
        "Error when download file: Unsupported audio format: $audioFormat",
        HttpStatus.INTERNAL_SERVER_ERROR,
      ),
      result,
    )
  }

  @Test
  fun `downloadAudioFile should return failure if file not found`() {
    // Arrange
    val info = AudioRequestModel(userId = 1, phraseId = 1)
    val audioFormat = "m4a"

    every { audioConversionConfig.extensions.containsKey(audioFormat) } returns true
    every {
      userPhraseFileRepository.findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        info.userId,
        info.phraseId,
      )
    } returns Optional.empty()

    // Act
    val result = audioService.downloadAudioFile(info, audioFormat)

    // Assert
    assertEquals(ServiceResult.failure("Audio file not found.", HttpStatus.NOT_FOUND), result)
  }

  @Test
  fun `downloadAudioFile should download and convert file successfully`() {
    // Arrange
    val info = AudioRequestModel(userId = 1, phraseId = 1)
    val audioFormat = "m4a"
    val userPhraseFile = mockk<UserPhraseFileModel>()
    val tempFile = File.createTempFile("temp", ".m4a")
    val outputFile = File.createTempFile("temp", ".m4a")
    val inputStream = mockk<InputStream>()

    // Make sure temp files will be deleted
    tempFile.deleteOnExit()
    outputFile.deleteOnExit()

    // Mock static File.createTempFile
    mockkStatic(File::class)
    every { File.createTempFile(any(), any()) } returns tempFile

    // Mock static FileUtils.copyInputStreamToFile
    mockkStatic(FileUtils::class)
    every { FileUtils.copyInputStreamToFile(any(), any()) } just Runs

    // Mock the conversion config and repository methods
    every { audioConversionConfig.extensions.containsKey(audioFormat) } returns true
    every {
      userPhraseFileRepository.findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        any(),
        any(),
      )
    } returns Optional.of(userPhraseFile)
    every { userPhraseFile.fileName } returns "test.m4a"
    every { storageService.downloadFile("test.m4a") } returns inputStream

    // Mock the conversion service
    every { converterService.convertAudioFile(any(), any(), any()) } returns outputFile

    // Act
    val result = audioService.downloadAudioFile(info, audioFormat)

    // Assert
    assertEquals(ServiceResult.success(FileSystemResource(outputFile)), result)
    verify { storageService.downloadFile("test.m4a") }
    verify { converterService.convertAudioFile(info, tempFile, "m4a") }
  }
}
