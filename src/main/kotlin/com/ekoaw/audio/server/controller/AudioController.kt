package com.ekoaw.audio.server.application.controller

import com.ekoaw.audio.server.model.entity.PhraseModel
import com.ekoaw.audio.server.model.entity.UserModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.model.response.ResponseModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.service.AudioFileService
import com.ekoaw.audio.server.util.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class AudioController(private val audioFileService: AudioFileService, private val userRepository: UserRepository, private val phraseRepository: PhraseRepository) {
    @GetMapping("/audio/user/{Id}")
    fun getUser(@PathVariable Id: Int): ResponseEntity<UserModel> {
        return ResponseEntity(userRepository.getReferenceById(Id), HttpStatus.OK)
    }

    @GetMapping("/audio/phrase/{Id}")
    fun getPhrase(@PathVariable Id: Int): ResponseEntity<PhraseModel> {
        return ResponseEntity(phraseRepository.getReferenceById(Id), HttpStatus.OK)
    }

    @GetMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun getAudio(
            @PathVariable userId: Int,
            @PathVariable phraseId: Int
    ): ResponseEntity<ResponseModel> {
        // TODO: file download
        return ResponseEntity(ResponseBuilder.success("Should be a file"), HttpStatus.OK)
    }

    @PostMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun postAudio(
            @PathVariable userId: Int,
            @PathVariable phraseId: Int,
            @RequestParam("audio_file") audioFile: MultipartFile
    ): ResponseEntity<ResponseModel> {
        // Validate the file extension
        if (!audioFile.originalFilename.orEmpty().endsWith(".m4a", ignoreCase = true)) {
            return ResponseEntity(
                    ResponseBuilder.error("Invalid file format. Only .m4a files are allowed."),
                    HttpStatus.BAD_REQUEST
            )
        }

        // Save the file to the temporary folder
        try {
            // TODO: move to background process to avoid congestion
            audioFileService.uploadAudioFile(AudioRequestModel(userId, phraseId), audioFile)

            return ResponseEntity(
                    ResponseBuilder.success("File uploaded successfully"),
                    HttpStatus.OK
            )
        } catch (e: Exception) {
            return ResponseEntity(
                    ResponseBuilder.error("File upload failed: ${e.message}"),
                    HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
