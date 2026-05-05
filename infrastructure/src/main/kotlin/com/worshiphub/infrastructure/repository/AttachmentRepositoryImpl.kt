package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Attachment
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaAttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findBySongId(songId: UUID): List<Attachment>
}

@Repository
open class AttachmentRepositoryImpl(
    private val jpaRepository: JpaAttachmentRepository,
    private val entityManager: EntityManager
) : AttachmentRepository {
    
    override fun save(attachment: Attachment): Attachment {
        return if (jpaRepository.existsById(attachment.id)) {
            jpaRepository.save(attachment)
        } else {
            entityManager.persist(attachment)
            attachment
        }
    }
    override fun findById(id: UUID): Attachment? = jpaRepository.findById(id).orElse(null)
    override fun findBySongId(songId: UUID): List<Attachment> = jpaRepository.findBySongId(songId)
    override fun delete(attachment: Attachment) = jpaRepository.delete(attachment)
}