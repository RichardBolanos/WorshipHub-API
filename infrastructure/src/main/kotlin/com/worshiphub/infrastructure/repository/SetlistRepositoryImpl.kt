package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.scheduling.Setlist
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaSetlistRepository : JpaRepository<Setlist, UUID> {
    fun findByChurchId(churchId: UUID): List<Setlist>
}

@Repository
open class SetlistRepositoryImpl(
    private val jpaRepository: JpaSetlistRepository,
    private val entityManager: EntityManager
) : SetlistRepository {
    
    override fun save(setlist: Setlist): Setlist {
        return if (jpaRepository.existsById(setlist.id)) {
            jpaRepository.save(setlist)
        } else {
            entityManager.persist(setlist)
            setlist
        }
    }
    
    override fun findById(id: UUID): Setlist? = jpaRepository.findById(id).orElse(null)
    
    override fun findByChurchId(churchId: UUID): List<Setlist> = jpaRepository.findByChurchId(churchId)
    
    override fun delete(setlist: Setlist) = jpaRepository.delete(setlist)
    
    override fun delete(id: UUID) {
        jpaRepository.deleteById(id)
    }
}