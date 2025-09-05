package com.worshiphub.domain.auth.repository

import com.worshiphub.domain.auth.PasswordResetToken
import java.util.*

/**
 * Repository interface for password reset token operations.
 */
interface PasswordResetTokenRepository {
    
    /**
     * Saves a password reset token.
     */
    fun save(token: PasswordResetToken): PasswordResetToken
    
    /**
     * Finds a token by its token string.
     */
    fun findByToken(token: String): PasswordResetToken?
    
    /**
     * Finds all valid tokens for a user.
     */
    fun findValidTokensByUserId(userId: UUID): List<PasswordResetToken>
    
    /**
     * Invalidates all tokens for a user (marks as used).
     */
    fun invalidateAllTokensForUser(userId: UUID)
    
    /**
     * Deletes expired tokens (cleanup job).
     */
    fun deleteExpiredTokens()
}