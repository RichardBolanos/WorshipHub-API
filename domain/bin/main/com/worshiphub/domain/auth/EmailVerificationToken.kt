package com.worshiphub.domain.auth

import jakarta.persistence.*
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
@Entity
@Table(name = "email_verification_tokens")
data class EmailVerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, unique = true)
    val token: String,
    
    @Column(nullable = false)
    val email: String,
    
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    
    @Column(nullable = false)
    val isUsed: Boolean = false,
    
    @Column(nullable = false)
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