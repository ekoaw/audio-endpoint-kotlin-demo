package com.ekoaw.audio.server.service

import com.ekoaw.audio.server.config.AudioConversionConfig
import com.ekoaw.audio.server.model.request.AudioRequestModel
import java.io.File
import java.io.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ConverterService(private val audioConversionConfig: AudioConversionConfig) {
  private val logger: Logger = LoggerFactory.getLogger(ConverterService::class.java)

  /**
   * Converts an audio file to the specified format using FFmpeg.
   *
   * @param info An object containing user ID and phrase ID.
   * @param inputFile The input audio file.
   * @param ext The desired output file extension (e.g., "mp3", "wav").
   * @return The converted audio file.
   * @throws IllegalArgumentException If the specified extension is not supported.
   * @throws IOException If an I/O error occurs during the conversion process.
   * @throws Exception If the FFmpeg command execution fails.
   */
  fun convertAudioFile(info: AudioRequestModel, inputFile: File, ext: String): File {
    logger.info("Converting audio file to {}", ext)
    val outputFile = File.createTempFile("${info.userId}_${info.phraseId}_", ".$ext")

    try {
      if (outputFile.exists()) {
        logger.debug("Deleting existing output file: {}", outputFile.absolutePath)
        outputFile.delete()
      }

      if (!audioConversionConfig.extensions.containsKey(ext)) {
        logger.error("Unsupported extension: {}", ext)
        throw IllegalArgumentException("Unsupported extension: $ext")
      }

      val outputArguments = audioConversionConfig.extensions[ext]!!

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
