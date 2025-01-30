package com.ekoaw.audio.server.controller

import com.ekoaw.audio.server.model.request.AudioRequestModel
import com.ekoaw.audio.server.model.response.ResponseModel
import com.ekoaw.audio.server.repository.PhraseRepository
import com.ekoaw.audio.server.repository.UserRepository
import com.ekoaw.audio.server.service.AudioService
import com.ekoaw.audio.server.util.ResponseBuilder
import com.ekoaw.audio.server.util.ServiceResult
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Handles HTTP requests related to audio files, including retrieving and uploading audio files
 * associated with specific users and phrases.
 */
@RestController
class AudioController(
  private val audioService: AudioService,
  private val userRepository: UserRepository,
  private val phraseRepository: PhraseRepository,
) {
  /**
   * Retrieves the audio file associated with the specified user and phrase.
   *
   * @param userId The ID of the user.
   * @param phraseId The ID of the phrase.
   * @return A ResponseEntity containing the audio file as a Resource,
   * ```
   *         or an error response if the file is not found.
   * ```
   */
  @GetMapping("/audio/user/{userId}/phrase/{phraseId}/{audioFormat}")
  fun getAudio(
    @PathVariable userId: Int,
    @PathVariable phraseId: Int,
    @PathVariable audioFormat: String,
  ): ResponseEntity<Any> {
    return when (
      val result = audioService.downloadAudioFile(AudioRequestModel(userId, phraseId), audioFormat)
    ) {
      is ServiceResult.Success ->
        ResponseEntity.ok()
          .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"${result.data?.filename}\"",
          )
          .body(result.data)
      is ServiceResult.Failure -> ResponseEntity(ResponseBuilder.error(result.message), result.code)
    }
  }

  /**
   * Uploads an audio file for the specified user and phrase.
   *
   * @param userId The ID of the user.
   * @param phraseId The ID of the phrase.
   * @param audioFile The audio file to be uploaded.
   * @return A ResponseEntity containing a success or failure response.
   */
  @PostMapping("/audio/user/{userId}/phrase/{phraseId}")
  fun postAudio(
    @PathVariable userId: Int,
    @PathVariable phraseId: Int,
    @RequestParam("audio_file") audioFile: MultipartFile,
  ): ResponseEntity<ResponseModel> {
    return when (
      val result = audioService.uploadAudioFile(AudioRequestModel(userId, phraseId), audioFile)
    ) {
      is ServiceResult.Success ->
        ResponseEntity(ResponseBuilder.success("File uploaded successfully"), HttpStatus.OK)
      is ServiceResult.Failure -> ResponseEntity(ResponseBuilder.error(result.message), result.code)
    }
  }
}
