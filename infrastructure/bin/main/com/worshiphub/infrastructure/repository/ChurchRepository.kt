package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.repository.ChurchRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*
import org.slf4j.LoggerFactory

interface JpaChurchRepository : JpaRepository<Church, UUID> {
    fun findByEmail(email: String): Church?
    fun existsByEmail(email: String): Boolean
}

@Repository
open class ChurchRepositoryImpl(
    private val jpaRepository: JpaChurchRepository,
    private val entityManager: EntityManager
) : ChurchRepository {
    
    private val logger = LoggerFactory.getLogger(ChurchRepositoryImpl::class.java)
    
    override fun save(church: Church): Church {
        logger.info("Saving church: name=${church.name}, email=${church.email}")
        // Use persist for new entities to avoid StaleObjectStateException.
        // Since Church generates its own UUID before save, JpaRepository.save()
        // calls merge() (thinks it's detached). persist() is correct for new entities.
        return if (jpaRepository.existsById(church.id)) {
            jpaRepository.save(church)
        } else {
            entityManager.persist(church)
            church
        }
    }
    
    override fun findById(id: UUID): Church? = 
        jpaRepository.findById(id).orElse(null)
    
    override fun findByEmail(email: String): Church? = 
        jpaRepository.findByEmail(email)
    
    override fun existsByEmail(email: String): Boolean = 
        jpaRepository.existsByEmail(email)
    
    override fun delete(church: Church) {
        jpaRepository.deleteById(church.id)
    }
}