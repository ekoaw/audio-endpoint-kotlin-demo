package com.ekoaw.audio.server.application.controller

import com.ekoaw.audio.server.model.entity.PhraseModel
import com.ekoaw.audio.server.model.entity.UserModel
import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.model.response.ResponseModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.service.AudioFileService
import com.ekoaw.audio.server.util.ResponseBuilder
import com.ekoaw.audio.server.util.ServiceResult
import java.util.Optional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.core.io.Resource

@RestController
class AudioController(
        private val audioFileService: AudioFileService,
        private val userRepository: UserRepository,
        private val phraseRepository: PhraseRepository
) {
    @GetMapping("/audio/user/{Id}")
    fun getUser(@PathVariable Id: Int): ResponseEntity<Optional<UserModel>> {
        return ResponseEntity(userRepository.findById(Id), HttpStatus.OK)
    }

    @GetMapping("/audio/phrase/{Id}")
    fun getPhrase(@PathVariable Id: Int): ResponseEntity<Optional<PhraseModel>> {
        return ResponseEntity(phraseRepository.findById(Id), HttpStatus.OK)
    }

    @GetMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun getAudio(
            @PathVariable userId: Int,
            @PathVariable phraseId: Int
    ): ResponseEntity<Any> {
        return when (val result = audioFileService.downloadAudioFile(AudioRequestModel(userId, phraseId))) {
        is ServiceResult.Success ->
            ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.data?.filename}\"")
            .body(result.data)
        is ServiceResult.Failure ->
            ResponseEntity(ResponseBuilder.error(result.message), result.code)
        }
    }

    @PostMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun postAudio(
            @PathVariable userId: Int,
            @PathVariable phraseId: Int,
            @RequestParam("audio_file") audioFile: MultipartFile
    ): ResponseEntity<ResponseModel> {
        return when (val result =
                        audioFileService.uploadAudioFile(
                                AudioRequestModel(userId, phraseId),
                                audioFile
                        )
        ) {
            is ServiceResult.Success ->
                    ResponseEntity(
                            ResponseBuilder.success("File uploaded successfully"),
                            HttpStatus.OK
                    )
            is ServiceResult.Failure ->
                    ResponseEntity(ResponseBuilder.error(result.message), result.code)
        }
    }
}
