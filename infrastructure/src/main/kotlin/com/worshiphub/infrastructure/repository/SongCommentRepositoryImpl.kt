package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.SongComment
import com.worshiphub.domain.collaboration.repository.SongCommentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaSongCommentRepository : JpaRepository<SongComment, UUID> {
    fun findBySongId(songId: UUID): List<SongComment>
    fun findByUserId(userId: UUID): List<SongComment>
}

@Repository
open class SongCommentRepositoryImpl(
    private val jpaRepository: JpaSongCommentRepository
) : SongCommentRepository {
    
    override fun save(comment: SongComment): SongComment = jpaRepository.save(comment)
    override fun findById(id: UUID): SongComment? = jpaRepository.findById(id).orElse(null)
    override fun findBySongId(songId: UUID): List<SongComment> = jpaRepository.findBySongId(songId)
    override fun findByUserId(userId: UUID): List<SongComment> = jpaRepository.findByUserId(userId)
    override fun delete(comment: SongComment) = jpaRepository.delete(comment)
}