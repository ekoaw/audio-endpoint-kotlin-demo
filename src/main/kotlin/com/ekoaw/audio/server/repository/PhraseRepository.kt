package com.ekoaw.audio.server.repository

import com.ekoaw.audio.server.model.entity.PhraseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface PhraseRepository : JpaRepository<PhraseModel, Int> {}
