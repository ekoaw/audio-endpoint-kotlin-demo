package com.ekoaw.audio.server.repository

import com.ekoaw.audio.server.model.entity.UserModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface UserRepository : JpaRepository<UserModel, Int> {}
