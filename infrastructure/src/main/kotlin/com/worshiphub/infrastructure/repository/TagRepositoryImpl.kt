package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.repository.TagRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaTagRepository : JpaRepository<Tag, UUID> {
    fun findByChurchId(churchId: UUID): List<Tag>
}

@Repository
open class TagRepositoryImpl(
    private val jpaRepository: JpaTagRepository,
    private val entityManager: EntityManager
) : TagRepository {
    
    override fun save(tag: Tag): Tag {
        return if (jpaRepository.existsById(tag.id)) {
            jpaRepository.save(tag)
        } else {
            entityManager.persist(tag)
            tag
        }
    }
    override fun findById(id: UUID): Tag? = jpaRepository.findById(id).orElse(null)
    override fun findByChurchId(churchId: UUID): List<Tag> = jpaRepository.findByChurchId(churchId)
    override fun delete(tag: Tag) = jpaRepository.delete(tag)
}