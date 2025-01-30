package com.ekoaw.audio.server.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "user_phrase_files", schema = "audiodemo")
data class UserPhraseFileModel(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int = 0,
  @ManyToOne @JoinColumn(name = "user_id", nullable = false) val user: UserModel,
  @ManyToOne @JoinColumn(name = "phrase_id", nullable = false) val phrase: PhraseModel,
  @Column(nullable = true) var fileName: String?,
  @Column(nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now(),
  @Column(nullable = true) var deletedAt: OffsetDateTime? = null,
) {
  constructor() :
    this(
      0,
      UserModel(0, OffsetDateTime.now()),
      PhraseModel(0, OffsetDateTime.now()),
      null,
      OffsetDateTime.now(),
      null,
    )
}
