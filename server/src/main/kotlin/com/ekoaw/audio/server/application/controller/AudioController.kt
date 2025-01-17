package com.ekoaw.audio.server.application.controller

import com.ekoaw.audio.server.application.util.ResponseBuilder
import com.ekoaw.audio.server.domain.model.ResponseModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class AudioController() {
    @GetMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun getAudio(
            @PathVariable userId: String,
            @PathVariable phraseId: String
    ): ResponseEntity<ResponseModel> {
        // TODO: file download
        return ResponseEntity(ResponseBuilder.success("Should be a file"), HttpStatus.OK)
    }

    @PostMapping("/audio/user/{userId}/phrase/{phraseId}")
    fun postAudio(
            @PathVariable userId: String,
            @PathVariable phraseId: String,
            @RequestParam("audio_file") audioFile: MultipartFile
    ): ResponseEntity<ResponseModel> {
        // Validate the file extension
        if (!audioFile.originalFilename.orEmpty().endsWith(".m4a", ignoreCase = true)) {
            return ResponseEntity(
                    ResponseBuilder.error(
                            "Invalid file format. Only .m4a files are allowed.",
                            "ERR_INVALID_FILE_FORMAT"
                    ),
                    HttpStatus.BAD_REQUEST
            )
        }

        // Save the file to the temporary folder
        try {
            // TODO: file storage
            return ResponseEntity(
                    ResponseBuilder.success("File uploaded successfully"),
                    HttpStatus.OK
            )
        } catch (e: Exception) {
            return ResponseEntity(
                    ResponseBuilder.error(
                            "File upload failed: ${e.message}",
                            "ERR_FILE_UPLOAD_ERROR"
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
