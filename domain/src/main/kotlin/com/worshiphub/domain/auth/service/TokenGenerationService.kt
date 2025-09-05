package com.worshiphub.domain.auth.service

import java.security.SecureRandom
import java.util.*

/**
 * Domain service for generating secure tokens.
 */
object TokenGenerationService {
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generates a cryptographically secure token for email verification.
     * Uses URL-safe Base64 encoding for easy transmission.
     */
    fun generateEmailVerificationToken(): String {
        val bytes = ByteArray(32) // 256 bits
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Generates a cryptographically secure token for password reset.
     * Uses URL-safe Base64 encoding for easy transmission.
     */
    fun generatePasswordResetToken(): String {
        val bytes = ByteArray(32) // 256 bits
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Generates a secure invitation token.
     */
    fun generateInvitationToken(): String {
        val bytes = ByteArray(24) // 192 bits
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}