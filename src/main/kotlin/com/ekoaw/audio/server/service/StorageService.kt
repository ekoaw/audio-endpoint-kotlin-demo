package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.MinioConfig
import io.minio.*
import io.minio.errors.MinioException
import java.io.InputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * This service class provides an abstraction for interacting with a cloud storage service.
 *
 * It encapsulates the logic for uploading, downloading, and deleting files from the storage
 * service, providing a cleaner interface for other parts of the application to interact with the
 * underlying storage infrastructure.
 */
@Service
class StorageService(private val minioClient: MinioClient, private val minioConfig: MinioConfig) {
    private val logger: Logger = LoggerFactory.getLogger(AudioService::class.java)

    /**
     * Uploads a file to the cloud storage service.
     *
     * @param objectName The name of the object to be stored in the storage.
     * @param inputStream The input stream of the file to be uploaded.
     * @param contentType The content type of the file.
     */
    fun uploadFile(objectName: String, inputStream: InputStream, contentType: String) {
        try {
            logger.info("Uploading file to storage: {}", objectName)
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .`object`(objectName)
                            .stream(inputStream, -1, 10485760) // 10MB buffer
                            .contentType(contentType)
                            .build()
            )
        } catch (e: MinioException) {
            throw RuntimeException("Failed to upload file to MinIO", e)
        } catch (e: Exception) {
            throw RuntimeException("Unexpected error while uploading file", e)
        }
    }

    /**
     * Downloads a file from the cloud storage service.
     *
     * @param objectName The name of the object to be downloaded.
     * @return An InputStream of the downloaded file.
     */
    fun downloadFile(objectName: String): InputStream {
        return try {
            logger.info("Downloading file from storage: {}", objectName)
            minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .`object`(objectName)
                            .build()
            )
        } catch (e: MinioException) {
            throw RuntimeException("Failed to download file from MinIO", e)
        } catch (e: Exception) {
            throw RuntimeException("Unexpected error while downloading file", e)
        }
    }

    /**
     * Deletes a file from the cloud storage service.
     *
     * @param objectName The name of the object to be deleted.
     */
    fun deleteFile(objectName: String) {
        try {
            logger.info("Deleting file from storage: {}", objectName)
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .`object`(objectName)
                            .build()
            )
        } catch (e: MinioException) {
            throw RuntimeException("Failed to delete file from MinIO", e)
        } catch (e: Exception) {
            throw RuntimeException("Unexpected error while deleting file", e)
        }
    }
}
