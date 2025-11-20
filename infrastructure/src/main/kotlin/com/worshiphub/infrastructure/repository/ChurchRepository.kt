package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.repository.ChurchRepository
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
    private val jpaRepository: JpaChurchRepository
) : ChurchRepository {
    
    private val logger = LoggerFactory.getLogger(ChurchRepositoryImpl::class.java)
    
    override fun save(church: Church): Church {
        logger.info("Saving church: name=${church.name}, email=${church.email}")
        return jpaRepository.save(church)
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