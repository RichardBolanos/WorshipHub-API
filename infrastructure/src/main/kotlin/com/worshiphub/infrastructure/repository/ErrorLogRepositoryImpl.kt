package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.common.ErrorLog
import com.worshiphub.domain.common.ErrorLogRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaErrorLogRepository : JpaRepository<ErrorLog, UUID> {
    fun findByErrorHash(errorHash: String): ErrorLog?
}

@Repository
class ErrorLogRepositoryImpl(
    private val jpaRepository: JpaErrorLogRepository
) : ErrorLogRepository {
    
    override fun findByErrorHash(errorHash: String): ErrorLog? {
        return jpaRepository.findByErrorHash(errorHash)
    }
    
    override fun save(errorLog: ErrorLog): ErrorLog {
        return jpaRepository.save(errorLog)
    }
}