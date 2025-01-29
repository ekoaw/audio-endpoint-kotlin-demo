package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.AudioConversionConfig
import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserPhraseFileRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.util.ServiceResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import org.apache.commons.io.FileCleaningTracker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * This service class provides business logic for managing audio files, including uploading,
 * downloading, and conversion.
 *
 * It interacts with various repositories (UserRepository, PhraseRepository,
 * UserPhraseFileRepository) and services (StorageService) to perform these operations.
 */
@Service
class AudioService(
        private val userRepository: UserRepository,
        private val phraseRepository: PhraseRepository,
        private val userPhraseFileRepository: UserPhraseFileRepository,
        private val audioConversionConfig: AudioConversionConfig,
        private val storageService: StorageService
) {
    private val logger: Logger = LoggerFactory.getLogger(AudioService::class.java)
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Uploads an audio file associated with a specific user and phrase.
     *
     * @param info An object containing user ID and phrase ID.
     * @param multipartFile The audio file to be uploaded.
     * @return A ServiceResult indicating success or failure with an optional UserPhraseFileModel
     * object on success.
     */
    fun uploadAudioFile(
            info: AudioRequestModel,
            multipartFile: MultipartFile
    ): ServiceResult<UserPhraseFileModel?> {
        // Validate file extension
        if (!multipartFile.originalFilename.orEmpty().endsWith(".m4a", ignoreCase = true)) {
            logger.error(
                    "Invalid file extension, should be .m4a: {}",
                    multipartFile.originalFilename
            )
            return ServiceResult.failure(
                    "Invalid file extension, should be .m4a",
                    HttpStatus.PRECONDITION_FAILED
            )
        }

        // Check user existence
        val user = userRepository.findById(info.userId)
        if (user.isEmpty) {
            logger.error("User with ID ${info.userId} not found")
            return ServiceResult.failure(
                    "User ${info.userId} not found.",
                    HttpStatus.PRECONDITION_FAILED
            )
        }

        // Check phrase existence
        val phrase = phraseRepository.findById(info.phraseId)
        if (phrase.isEmpty) {
            logger.error("Phrase with ID ${info.phraseId} not found")
            return ServiceResult.failure(
                    "Phrase ${info.phraseId} not found.",
                    HttpStatus.PRECONDITION_FAILED
            )
        }

        try {
            val userId = user.get().id
            val phraseId = phrase.get().id

            // Generate unique file name and path
            // val inputPath = tempFolder.resolve("${userId}_${phraseId}_${Ksuid.newKsuid()}.m4a")
            // val inputFile = inputPath.toFile()
            val inputFile = File.createTempFile("${userId}_${phraseId}_", ".m4a")

            // Save uploaded file to temporary location
            logger.info("Saving uploaded file to: {}", inputFile)
            // multipartFile.inputStream.use { input -> Files.copy(input, inputFile.toPath()) }

            multipartFile.inputStream.use { inputStream ->
                FileOutputStream(inputFile).use { outputStream -> inputStream.copyTo(outputStream) }
            }

            // Convert audio to WAV format
            val outputFile = convertAudioFile(info, inputFile, "wav")

            // Upload converted file to storage
            FileInputStream(inputFile).use { inputStream ->
                storageService.uploadFile(outputFile.name, inputStream, "audio/wav")
            }

            // Insert user-phrase file info into database
            val fileId =
                    userPhraseFileRepository.save(
                                    UserPhraseFileModel(
                                            user = user.get(),
                                            phrase = phrase.get(),
                                            fileName = outputFile.name
                                    )
                            )
                            .id

            // Schedule background task for cleanup
            executor.submit {
                try {
                    // Find any previous files for this user-phrase combination
                    val filesToSoftDelete =
                            userPhraseFileRepository
                                    .findByUserIdAndPhraseIdAndDeletedAtIsNullAndIdLessThanOrderByCreatedAtDesc(
                                            userId,
                                            phraseId,
                                            fileId
                                    )

                    // Soft-delete any previous files for this user-phrase combination
                    val now = OffsetDateTime.now()
                    filesToSoftDelete.forEach {
                        it.deletedAt = now
                        it.fileName?.let { fileName ->
                            try {
                                storageService.deleteFile(fileName)
                                logger.info("Deleted file from storage: {}", fileName)
                            } catch (e: Exception) {
                                logger.error("Failed to delete file from storage: {}", fileName, e)
                            }
                        }
                    }
                    userPhraseFileRepository.saveAll(filesToSoftDelete)

                    // Delete temporary files
                    logger.info("Deleting temporary files: $inputFile, $outputFile")
                    inputFile.delete()
                    outputFile.delete()
                } catch (e: Exception) {
                    logger.error("Error during cleanup task:", e)
                }
            }
        } catch (ex: Exception) {
            logger.error("Error when uploading file:", ex)
            return ServiceResult.failure(
                    "Error when uploading file: ${ex.message}",
                    HttpStatus.INTERNAL_SERVER_ERROR
            )
        }

        return ServiceResult.success(null)
    }

    /**
     * Downloads the latest audio file associated with a specific user and phrase.
     *
     * @param info An object containing user ID and phrase ID.
     * @return A ServiceResult indicating success or failure with an optional Resource object on
     * success.
     */
    fun downloadAudioFile(info: AudioRequestModel): ServiceResult<Resource?> {
        var tempFile: File? = null
        var outputFile: File? = null

        try {
            // Find the latest uploaded file
            val uploadedFile =
                    userPhraseFileRepository
                            .findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    info.userId,
                                    info.phraseId
                            )

            if (uploadedFile.isEmpty()) {
                logger.warn(
                        "Audio file not found for user ${info.userId} and phrase ${info.phraseId}"
                )
                return ServiceResult.failure("Audio file not found.", HttpStatus.NOT_FOUND)
            }

            val objectName =
                    uploadedFile.get().fileName
                            ?: throw IllegalStateException("File name should not be null")

            val inputStream = storageService.downloadFile(objectName)

            // Create temporary file
            tempFile =
                    File.createTempFile(
                            objectName.substringBeforeLast("."),
                            objectName.substringAfterLast(".")
                    )

            // Save downloaded file to temporary file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }

            // Convert audio to m4a format
            outputFile = convertAudioFile(info, tempFile!!, "m4a")

            // Return the converted file as a Resource
            return ServiceResult.success(FileSystemResource(outputFile))
        } catch (ex: Exception) {
            logger.error("Error when downloading file:", ex)
            return ServiceResult.failure(
                    "Error when download file: ${ex.message}",
                    HttpStatus.INTERNAL_SERVER_ERROR
            )
        } finally {
            // Schedule background task for cleanup
            executor.submit {
                try {
                    tempFile?.delete() // Delete temporary file
                    outputFile?.let { outputFile ->
                        val tracker = FileCleaningTracker()
                        tracker.track(outputFile, outputFile)
                    }
                    logger.debug("Temporary files deleted successfully.")
                } catch (e: Exception) {
                    logger.warn("Failed to delete temporary files: {}", e.message)
                }
            }
        }
    }

    /**
     * Converts an audio file to the specified format using FFmpeg.
     *
     * @param info An object containing user ID and phrase ID.
     * @param inputFile The input audio file.
     * @param ext The desired output file extension (e.g., "mp3", "wav").
     * @return The converted audio file.
     *
     * @throws IllegalArgumentException If the specified extension is not supported.
     * @throws IOException If an I/O error occurs during the conversion process.
     * @throws Exception If the FFmpeg command execution fails.
     */
    private fun convertAudioFile(info: AudioRequestModel, inputFile: File, ext: String): File {
        logger.info("Converting audio file to {}", ext)
        val outputFile = File.createTempFile("${info.userId}_${info.phraseId}_", ".$ext")

        try {
            if (outputFile.exists()) {
                logger.debug("Deleting existing output file: {}", outputFile.absolutePath)
                outputFile.delete()
            }

            val outputArguments = audioConversionConfig.extensions[ext.lowercase()]
            if (outputArguments == null) {
                logger.error("Unsupported extension: {}", ext)
                throw IllegalArgumentException("Unsupported extension: $ext")
            }

            val arguments =
                    listOf("ffmpeg", "-i", inputFile.absolutePath) +
                            outputArguments +
                            listOf(outputFile.absolutePath)

            logger.info("Starting audio conversion: {}", arguments.joinToString(" "))

            val process = ProcessBuilder(arguments).start()
            process.waitFor()

            if (process.exitValue() != 0) {
                logger.error("Audio conversion failed with exit code: {}", process.exitValue())
                throw Exception("Audio conversion failed")
            }

            logger.info("Audio conversion successful: {}", outputFile.absolutePath)
            return outputFile
        } catch (e: IOException) {
            logger.error("I/O error during audio conversion:", e)
            throw e
        }
    }
}
