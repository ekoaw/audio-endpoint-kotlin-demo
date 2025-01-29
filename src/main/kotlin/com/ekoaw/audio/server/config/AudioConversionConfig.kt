package com.ekoaw.audio.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "audio.conversion")
data class AudioConversionConfig(val extensions: Map<String, List<String>>)
