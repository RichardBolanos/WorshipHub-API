package com.worshiphub.domain.auth.repository

import com.worshiphub.domain.auth.EmailVerificationToken
import java.util.*

/**
 * Repository interface for email verification token operations.
 */
interface EmailVerificationTokenRepository {
    
    /**
     * Saves an email verification token.
     */
    fun save(token: EmailVerificationToken): EmailVerificationToken
    
    /**
     * Finds a token by its token string.
     */
    fun findByToken(token: String): EmailVerificationToken?
    
    /**
     * Finds all valid tokens for a user.
     */
    fun findValidTokensByUserId(userId: UUID): List<EmailVerificationToken>
    
    /**
     * Invalidates all tokens for a user (marks as used).
     */
    fun invalidateAllTokensForUser(userId: UUID)
    
    /**
     * Deletes expired tokens (cleanup job).
     */
    fun deleteExpiredTokens()
}