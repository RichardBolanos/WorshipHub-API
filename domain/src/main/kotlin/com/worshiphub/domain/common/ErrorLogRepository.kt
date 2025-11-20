package com.worshiphub.domain.common

/**
 * Repository interface for ErrorLog aggregate
 */
interface ErrorLogRepository {
    fun findByErrorHash(errorHash: String): ErrorLog?
    fun save(errorLog: ErrorLog): ErrorLog
}