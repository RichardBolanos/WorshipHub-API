package com.worshiphub.domain.auth

import java.time.LocalDateTime
import java.util.*

/**
 * Domain entity representing an email verification token.
 * 
 * @property id Unique identifier for the token
 * @property userId User ID this token belongs to
 * @property token Secure verification token
 * @property email Email address to be verified
 * @property expiresAt Token expiration timestamp
 * @property isUsed Whether the token has been used
 * @property createdAt Token creation timestamp
 */
data class EmailVerificationToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val token: String,
    val email: String,
    val expiresAt: LocalDateTime,
    val isUsed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Checks if the token is valid (not expired and not used).
     */
    fun isValid(): Boolean = !isUsed && LocalDateTime.now().isBefore(expiresAt)
    
    /**
     * Marks the token as used.
     */
    fun markAsUsed(): EmailVerificationToken = copy(isUsed = true)
    
    /**
     * Checks if the token is expired.
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}