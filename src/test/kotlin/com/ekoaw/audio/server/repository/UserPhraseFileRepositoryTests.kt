package com.ekoaw.audio.server.repository

import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
@Sql(
  scripts =
    [
      "/migrations/sql/001_schemas.sql",
      "/migrations/sql/002_users_data.sql",
      "/migrations/sql/003_phrases_data.sql",
      "/migrations/test_sql/001_user_phrase_files_data.sql",
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserPhraseFileRepositoryTests {

  @Autowired private lateinit var userRepository: UserRepository
  @Autowired private lateinit var phraseRepository: PhraseRepository
  @Autowired private lateinit var userPhraseFileRepository: UserPhraseFileRepository

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
  fun `id is generated when a user-phrase-file is persisted`() {
    val user = userRepository.findById(1)
    val phrase = phraseRepository.findById(1)

    val upf =
      UserPhraseFileModel(user = user.get(), phrase = phrase.get(), fileName = "output-file")

    assertEquals(0, upf.id)
    userPhraseFileRepository.save(upf)
    assertEquals(16, upf.id)
  }

  @Test
  fun `find last user-phrase-file data`() {
    val upf =
      userPhraseFileRepository.findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        10,
        10,
      )
    assertTrue(upf.isPresent())
    assertEquals(15, upf.get().id)
    assertEquals("audio-file-10-z", upf.get().fileName)
  }

  @Test
  fun `find all user-phrase-file data with id less than specified`() {
    val list =
      userPhraseFileRepository
        .findByUserIdAndPhraseIdAndDeletedAtIsNullAndIdLessThanOrderByCreatedAtDesc(10, 10, 15)
    assertEquals(1, list.size)
    assertEquals(12, list[0].id)
    assertEquals("audio-file-10", list[0].fileName)
  }
}
