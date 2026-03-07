package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Attachment
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaAttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findBySongId(songId: UUID): List<Attachment>
}

@Repository
open class AttachmentRepositoryImpl(
    private val jpaRepository: JpaAttachmentRepository
) : AttachmentRepository {
    
    override fun save(attachment: Attachment): Attachment = jpaRepository.save(attachment)
    override fun findById(id: UUID): Attachment? = jpaRepository.findById(id).orElse(null)
    override fun findBySongId(songId: UUID): List<Attachment> = jpaRepository.findBySongId(songId)
    override fun delete(attachment: Attachment) = jpaRepository.delete(attachment)
}