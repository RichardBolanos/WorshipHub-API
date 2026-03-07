package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.repository.TagRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaTagRepository : JpaRepository<Tag, UUID> {
    fun findByChurchId(churchId: UUID): List<Tag>
    fun findBySongId(songId: UUID): List<Tag>
}

@Repository
open class TagRepositoryImpl(
    private val jpaRepository: JpaTagRepository
) : TagRepository {
    
    override fun save(tag: Tag): Tag = jpaRepository.save(tag)
    override fun findById(id: UUID): Tag? = jpaRepository.findById(id).orElse(null)
    override fun findByChurchId(churchId: UUID): List<Tag> = jpaRepository.findByChurchId(churchId)
    override fun findBySongId(songId: UUID): List<Tag> = jpaRepository.findBySongId(songId)
    override fun delete(tag: Tag) = jpaRepository.delete(tag)
}