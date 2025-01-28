package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.AudioConversionConfig
import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserPhraseFileRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.util.ServiceResult
import com.github.ksuid.Ksuid
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.core.io.Resource
import org.springframework.core.io.FileSystemResource

@Service
class AudioFileService(
        private val userRepository: UserRepository,
        private val phraseRepository: PhraseRepository,
        private val userPhraseFileRepository: UserPhraseFileRepository,
        private val audioConversionConfig: AudioConversionConfig
) {

    // Configure the folder where files will be saved
    private val tempFolder: Path = Paths.get("temp")
    private val storageFolder: Path = Paths.get("storage")

    init {
        // Ensure the folder exists
        if (!Files.exists(tempFolder)) {
            Files.createDirectories(tempFolder)
        }
        if (!Files.exists(storageFolder)) {
            Files.createDirectories(storageFolder)
        }
    }

    fun uploadAudioFile(
            info: AudioRequestModel,
            multipartFile: MultipartFile
    ): ServiceResult<UserPhraseFileModel?> {
        // Validate the file extension
        if (!multipartFile.originalFilename.orEmpty().endsWith(".m4a", ignoreCase = true)) {
            return ServiceResult.failure(
                    "Invalid file extension, should be .m4a",
                    HttpStatus.PRECONDITION_FAILED
            )
        }
        // Check User and Phrase
        val user = userRepository.findById(info.userId)
        if (user.isEmpty) {
            return ServiceResult.failure(
                    "User ${info.userId} not found.",
                    HttpStatus.PRECONDITION_FAILED
            )
        }

        val phrase = phraseRepository.findById(info.phraseId)
        if (phrase.isEmpty) {
            return ServiceResult.failure(
                    "Phrase ${info.phraseId} not found.",
                    HttpStatus.PRECONDITION_FAILED
            )
        }

        try {
            // Determine the destination path
            val filePath =
                    tempFolder.resolve("${user.get().id}_${phrase.get().id}_${Ksuid.newKsuid()}.m4a")
            val file = filePath.toFile()

            // Save the file to the disk
            multipartFile.inputStream.use { input -> Files.copy(input, filePath) }

            // Convert to wav
            val outputFile = convertAudioFile(file, "wav")

            // Delete temp file from disk
            file.delete()

            // Delete previous file if exists
            val filesToSoftDelete =
                    userPhraseFileRepository
                            .findByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    user.get().id,
                                    phrase.get().id
                            )
            val now = OffsetDateTime.now()
            filesToSoftDelete.forEach { it.deletedAt = now }
            userPhraseFileRepository.saveAll(filesToSoftDelete)

            userPhraseFileRepository.save(
                    UserPhraseFileModel(user = user.get(), phrase = phrase.get(), fileName = outputFile.name)
            )
        } catch(ex: Exception) {
            return ServiceResult.failure("Error when uploading file: ${ex.message}", HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ServiceResult.success(null)
    }

    fun downloadAudioFile(info: AudioRequestModel): ServiceResult<Resource?> {
        try {
            // Check latest user phrase file
            val uploadedFile =
                    userPhraseFileRepository
                            .findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    info.userId,
                                    info.phraseId
                            )

            if (uploadedFile.isEmpty()) {
                return ServiceResult.failure("Audio file not found.", HttpStatus.NOT_FOUND)
            }

            val sourcePath = Paths.get("storage").resolve(uploadedFile.get().fileName)
            if (!Files.exists(sourcePath)) {
                throw RuntimeException("Source file not found. ${sourcePath}")
            }

            val output = convertAudioFile(sourcePath.toFile(), "m4a")
            return ServiceResult.success(FileSystemResource(output))

        } catch(ex: Exception) {
            return ServiceResult.failure("Error when download file: ${ex.message}", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun convertAudioFile(inputFile: File, ext: String): File {
        val outputFile = storageFolder.resolve("${inputFile.nameWithoutExtension}.$ext").toFile()

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val outputArguments = audioConversionConfig.extensions[ext.lowercase()]
        if (outputArguments == null) {
            throw IllegalArgumentException("Unsupported extension: $ext")
        }

        val arguments = listOf("ffmpeg", "-i", inputFile.absolutePath) + outputArguments + listOf(outputFile.absolutePath)
        println(arguments)

        val process = ProcessBuilder(arguments).start()
        process.waitFor()

        if (process.exitValue() != 0) {
            throw Exception("Audio conversion failed")
        }

        return outputFile
    }
}
