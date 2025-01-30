package com.ekoaw.audio.server.repository

import com.ekoaw.audio.server.model.entity.PhraseModel
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@DataJpaTest
@Sql(scripts = ["/migrations/sql/001_schemas.sql", "/migrations/sql/003_phrases_data.sql"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PhraseRepositoryTests(@Autowired private val phraseRepository: PhraseRepository) {

  companion object {
    @Container private val db = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))

    @BeforeAll
    fun startDBContainer() {
      db.start()
    }

    @AfterAll
    fun stopDBContainer() {
      db.stop()
    }

    @DynamicPropertySource
    @JvmStatic
    fun registerDBContainer(registry: DynamicPropertyRegistry) {
      println(db)
      registry.add("spring.datasource.url", db::getJdbcUrl)
      registry.add("spring.datasource.username", db::getUsername)
      registry.add("spring.datasource.password", db::getPassword)
    }
  }

  @Test
  fun `id is generated when a phrase is persisted`() {
    val phrase = PhraseModel()

    assertEquals(0, phrase.id)
    phraseRepository.save(phrase)
    assertEquals(11, phrase.id)
  }
}
