package com.ekoaw.audio.server.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "users", schema = "audiodemo")
data class UserModel(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int = 0,
  @Column(nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
  constructor() : this(0, OffsetDateTime.now())
}
