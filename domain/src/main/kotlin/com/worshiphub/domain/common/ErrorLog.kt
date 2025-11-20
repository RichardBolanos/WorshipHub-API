package com.worshiphub.domain.common

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Error log aggregate for intelligent error tracking
 */
@Entity
@Table(name = "error_logs")
data class ErrorLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "error_hash", unique = true, nullable = false)
    val errorHash: String,
    
    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    val errorMessage: String,
    
    @Column(name = "file_name", nullable = false)
    val fileName: String,
    
    @Column(name = "line_number", nullable = false)
    val lineNumber: Int,
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    val stackTrace: String?,
    
    @Column(name = "occurrence_count", nullable = false)
    var occurrenceCount: Int = 1,
    
    @Column(name = "first_occurrence", nullable = false)
    val firstOccurrence: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "last_occurrence", nullable = false)
    var lastOccurrence: LocalDateTime = LocalDateTime.now()
) {
    fun incrementOccurrence() {
        occurrenceCount++
        lastOccurrence = LocalDateTime.now()
    }
    
    companion object {
        fun generateHash(message: String, fileName: String, lineNumber: Int): String {
            return "${message.hashCode()}_${fileName}_$lineNumber".hashCode().toString()
        }
    }
}