package com.worshiphub.api.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import com.worshiphub.api.common.BusinessException
import jakarta.validation.ConstraintViolationException
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.hibernate.StaleObjectStateException

/**
 * Global exception handler for secure error responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldName to errorMessage
        }
        
        logger.error("Validation error on {}: {}", request.getDescription(false), errors, ex)
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Invalid input data",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = errors
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Constraint Violation",
            message = "Invalid input data",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = errors
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Authentication Failed",
            message = "Invalid credentials", // Generic message for security
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.FORBIDDEN.value(),
            error = "Access Denied",
            message = "Insufficient permissions",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Invalid Parameter",
            message = "Invalid parameter type for '${ex.name}'",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        // Log the actual exception for debugging (don't expose to client)
        logger.error("Runtime exception on {}: {}", request.getDescription(false), ex.message, ex)
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred", // Generic message for security
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(StaleObjectStateException::class)
    fun handleStaleObjectState(
        ex: StaleObjectStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stale object state exception on {}: {}", request.getDescription(false), ex.message, ex)
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Concurrency Conflict",
            message = "The resource was modified by another process. Please refresh and try again.",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = ex.httpStatus.value(),
            error = ex.errorCode,
            message = ex.message ?: "Business rule violation",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        // Log the actual exception for debugging (don't expose to client)
        logger.error("Unexpected exception on {}: {}", request.getDescription(false), ex.message, ex)
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}

/**
 * Standardized error response format.
 */
data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String>? = null
)