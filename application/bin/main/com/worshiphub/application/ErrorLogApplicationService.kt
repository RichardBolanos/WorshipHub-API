package com.worshiphub.application

import com.worshiphub.domain.common.ErrorLog
import com.worshiphub.domain.common.ErrorLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service for intelligent error logging
 */
@Service
@Transactional
class ErrorLogApplicationService(
    private val errorLogRepository: ErrorLogRepository
) {
    
    fun logError(message: String, fileName: String, lineNumber: Int, stackTrace: String? = null) {
        val errorHash = ErrorLog.generateHash(message, fileName, lineNumber)
        
        val existingError = errorLogRepository.findByErrorHash(errorHash)
        
        if (existingError != null) {
            existingError.incrementOccurrence()
            errorLogRepository.save(existingError)
        } else {
            val newError = ErrorLog(
                errorHash = errorHash,
                errorMessage = message,
                fileName = fileName,
                lineNumber = lineNumber,
                stackTrace = stackTrace
            )
            errorLogRepository.save(newError)
        }
    }
}