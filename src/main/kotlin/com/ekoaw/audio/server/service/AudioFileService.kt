package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.util.ServiceResult
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class AudioFileService(private val userRepository: UserRepository, private val phraseRepository: PhraseRepository) {
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

    fun uploadAudioFile(info: AudioRequestModel, multipartFile: MultipartFile): ServiceResult<UserPhraseFileModel?> {
        // Validate the file extension
        if (!multipartFile.originalFilename.orEmpty().endsWith(".m4a", ignoreCase = true)) {
            return ServiceResult.failure("Invalid file extension, should be .m4a")
        }
        // Check User and Phrase
        if (userRepository.findById(info.userId).isEmpty) {
            return ServiceResult.failure("User ${info.userId} not found.")
        }

        if (phraseRepository.findById(info.phraseId).isEmpty) {
            return ServiceResult.failure("Phrase ${info.phraseId} not found.")
        }

        // Determine the destination path
        val filePath = tempFolder.resolve("${info.userId}_${info.phraseId}.m4a")
        val file = filePath.toFile()

        if (file.exists()) {
            file.delete()
        }

        // Save the file to the disk
        multipartFile.inputStream.use { input -> Files.copy(input, filePath) }

        // Convert to wav
        convertAudioFile(file, "wav")

        // Delete temp file from disk
        file.delete()

        return ServiceResult.success(null)
    }

    private fun convertAudioFile(inputFile: File, ext: String): Boolean {
        val outputFile = storageFolder.resolve("${inputFile.nameWithoutExtension}.$ext").toFile()

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val process =
                ProcessBuilder("ffmpeg", "-i", inputFile.absolutePath, outputFile.absolutePath)
                        .start()

        process.waitFor()
        if (process.exitValue() == 0) {
            println("Conversion successful: ${outputFile.absolutePath}")
            return true
        } else {
            println("Conversion failed for: ${inputFile.absolutePath}")
            return false
        }
    }
}
