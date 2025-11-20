package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.scheduling.Setlist
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaSetlistRepository : JpaRepository<Setlist, UUID> {
    fun findByChurchId(churchId: UUID): List<Setlist>
}

@Repository
open class SetlistRepositoryImpl(
    private val jpaRepository: JpaSetlistRepository
) : SetlistRepository {
    
    override fun save(setlist: Setlist): Setlist = jpaRepository.save(setlist)
    
    override fun findById(id: UUID): Setlist? = jpaRepository.findById(id).orElse(null)
    
    override fun findByChurchId(churchId: UUID): List<Setlist> = jpaRepository.findByChurchId(churchId)
    
    override fun delete(setlist: Setlist) = jpaRepository.delete(setlist)
}