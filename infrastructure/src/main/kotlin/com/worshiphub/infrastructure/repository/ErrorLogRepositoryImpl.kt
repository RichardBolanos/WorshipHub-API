package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.common.ErrorLog
import com.worshiphub.domain.common.ErrorLogRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaErrorLogRepository : JpaRepository<ErrorLog, UUID> {
    fun findByErrorHash(errorHash: String): ErrorLog?
}

@Repository
open class ErrorLogRepositoryImpl(
    private val jpaRepository: JpaErrorLogRepository,
    private val entityManager: EntityManager
) : ErrorLogRepository {
    
    override fun findByErrorHash(errorHash: String): ErrorLog? {
        return jpaRepository.findByErrorHash(errorHash)
    }
    
    override fun save(errorLog: ErrorLog): ErrorLog {
        return if (jpaRepository.existsById(errorLog.id)) {
            jpaRepository.save(errorLog)
        } else {
            entityManager.persist(errorLog)
            errorLog
        }
    }
}