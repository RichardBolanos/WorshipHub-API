package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.infrastructure.persistence.ChurchEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaChurchRepository : JpaRepository<ChurchEntity, UUID> {
    fun findByEmail(email: String): ChurchEntity?
    fun existsByEmail(email: String): Boolean
}

@Repository
open class ChurchRepositoryImpl(
    private val jpaRepository: JpaChurchRepository
) : ChurchRepository {
    
    override fun save(church: Church): Church {
        val entity = ChurchEntity.fromDomain(church)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findById(id: UUID): Church? = 
        jpaRepository.findById(id).map { it.toDomain() }.orElse(null)
    
    override fun findByEmail(email: String): Church? = 
        jpaRepository.findByEmail(email)?.toDomain()
    
    override fun existsByEmail(email: String): Boolean = 
        jpaRepository.existsByEmail(email)
    
    override fun delete(church: Church) {
        val entity = ChurchEntity.fromDomain(church)
        jpaRepository.delete(entity)
    }
}