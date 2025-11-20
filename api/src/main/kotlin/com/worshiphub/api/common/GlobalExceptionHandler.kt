package com.worshiphub.api.common

import com.worshiphub.application.ErrorLogApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global exception handler with intelligent error logging
 */
@ControllerAdvice
class GlobalExceptionHandler(
    private val errorLogService: ErrorLogApplicationService
) {
    
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        val stackTrace = ex.stackTrace.firstOrNull()
        val fileName = stackTrace?.fileName ?: "Unknown"
        val lineNumber = stackTrace?.lineNumber ?: 0
        val fullStackTrace = getFullStackTrace(ex)
        
        errorLogService.logError(
            message = ex.message ?: "Unknown error",
            fileName = fileName,
            lineNumber = lineNumber,
            stackTrace = fullStackTrace
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Internal server error occurred"))
    }
    
    private fun getFullStackTrace(ex: Exception): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        ex.printStackTrace(pw)
        return sw.toString()
    }
}

data class ErrorResponse(
    val message: String
)