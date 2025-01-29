package com.ekoaw.audio.server.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig {

    @Value("\${minio.endpoint}") private lateinit var endpoint: String

    @Value("\${minio.access-key}") private lateinit var accessKey: String

    @Value("\${minio.secret-key}") private lateinit var secretKey: String

    @Value("\${minio.bucket}") private lateinit var bucketName: String

    @Bean
    fun minioClient(): MinioClient {
        println("MinIO config: ${endpoint}, ${accessKey}, ${secretKey}, ${bucketName}")
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build()
    }

    fun getBucketName(): String {
        return bucketName
    }
}
