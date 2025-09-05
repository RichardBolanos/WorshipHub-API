package com.worshiphub.domain.auth

import java.time.LocalDateTime
import java.util.*

/**
 * Domain entity representing a password reset token.
 * 
 * @property id Unique identifier for the token
 * @property userId User ID this token belongs to
 * @property token Secure reset token
 * @property email Email address for password reset
 * @property expiresAt Token expiration timestamp (shorter than email verification)
 * @property isUsed Whether the token has been used
 * @property createdAt Token creation timestamp
 */
data class PasswordResetToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val token: String,
    val email: String,
    val expiresAt: LocalDateTime,
    val isUsed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /**
         * Password reset tokens expire in 1 hour for security.
         */
        const val EXPIRATION_HOURS = 1L
    }
    
    /**
     * Checks if the token is valid (not expired and not used).
     */
    fun isValid(): Boolean = !isUsed && LocalDateTime.now().isBefore(expiresAt)
    
    /**
     * Marks the token as used.
     */
    fun markAsUsed(): PasswordResetToken = copy(isUsed = true)
    
    /**
     * Checks if the token is expired.
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}