package com.ekoaw.audio.server.repository

import com.ekoaw.audio.server.model.entity.UserPhraseFileModel
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPhraseFileRepository : JpaRepository<UserPhraseFileModel, Int> {
        fun findFirstByUserIdAndPhraseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId: Int,
                phraseId: Int
        ): Optional<UserPhraseFileModel>

        fun findByUserIdAndPhraseIdAndDeletedAtIsNullAndIdLessThanOrderByCreatedAtDesc(
                userId: Int,
                phraseId: Int,
                id: Int
        ): List<UserPhraseFileModel>
}
