package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.MinioConfig
import io.minio.*
import io.minio.errors.MinioException
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.io.InputStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class StorageServiceTest {

  @MockK private lateinit var minioClient: MinioClient

  @MockK private lateinit var minioConfig: MinioConfig

  @InjectMockKs private lateinit var storageService: StorageService

  @BeforeEach
  fun setUp() {
    // Mock the bucket name
    every { minioConfig.getBucketName() } returns "test-bucket"
  }

  @Test
  fun `uploadFile should call minioClient putObject with correct parameters`() {
    // Arrange
    val objectName = "test-object"
    val inputStream = mockk<InputStream>()
    val contentType = "application/octet-stream"

    // Mock the response for putObject
    every { minioClient.putObject(any()) } returns mockk<ObjectWriteResponse>()

    // Act
    storageService.uploadFile(objectName, inputStream, contentType)

    // Assert
    verify {
      minioClient.putObject(
        withArg {
          assert(it.bucket() == "test-bucket")
          assert(it.`object`() == objectName)
          assert(it.stream() is InputStream)
          assert(it.contentType() == contentType)
        }
      )
    }
  }

  @Test
  fun `uploadFile should throw RuntimeException when MinioException occurs`() {
    // Arrange
    val objectName = "test-object"
    val inputStream = mockk<InputStream>()
    val contentType = "application/octet-stream"

    every { minioClient.putObject(any()) } throws MinioException("MinIO error")

    // Act & Assert
    assertThrows<RuntimeException> {
      storageService.uploadFile(objectName, inputStream, contentType)
    }
  }

  @Test
  fun `downloadFile should call minioClient getObject with correct parameters`() {
    // Arrange
    val objectName = "test-object"

    // Mock the response for getObject
    val objectResponse = mockk<GetObjectResponse>()
    every { minioClient.getObject(any()) } returns objectResponse

    // Act
    val result = storageService.downloadFile(objectName)

    // Assert
    verify {
      minioClient.getObject(
        withArg {
          assert(it.bucket() == "test-bucket")
          assert(it.`object`() == objectName)
        }
      )
    }
    assert(result == objectResponse)
  }

  @Test
  fun `downloadFile should throw RuntimeException when MinioException occurs`() {
    // Arrange
    val objectName = "test-object"

    every { minioClient.getObject(any()) } throws MinioException("MinIO error")

    // Act & Assert
    assertThrows<RuntimeException> { storageService.downloadFile(objectName) }
  }

  @Test
  fun `deleteFile should call minioClient removeObject with correct parameters`() {
    // Arrange
    val objectName = "test-object"

    // Mock the response for removeObject
    every { minioClient.removeObject(any()) } just Runs

    // Act
    storageService.deleteFile(objectName)

    // Assert
    verify {
      minioClient.removeObject(
        withArg {
          assert(it.bucket() == "test-bucket")
          assert(it.`object`() == objectName)
        }
      )
    }
  }

  @Test
  fun `deleteFile should throw RuntimeException when MinioException occurs`() {
    // Arrange
    val objectName = "test-object"

    every { minioClient.removeObject(any()) } throws MinioException("MinIO error")

    // Act & Assert
    assertThrows<RuntimeException> { storageService.deleteFile(objectName) }
  }
}
