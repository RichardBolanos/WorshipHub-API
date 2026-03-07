package com.worshiphub.application.auth

import com.worshiphub.domain.auth.EmailVerificationToken
import com.worshiphub.domain.auth.repository.EmailVerificationTokenRepository
import com.worshiphub.domain.auth.service.TokenGenerationService
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for email verification operations.
 */
@Service
@Transactional
class EmailVerificationService(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    
    /**
     * Sends email verification to a user.
     */
    fun sendEmailVerification(userId: UUID): EmailVerificationResult {
        val user = userRepository.findById(userId)
            ?: return EmailVerificationResult.UserNotFound
            
        return sendEmailVerificationForUser(user)
    }

    /**
     * Resends email verification using the user's email address.
     */
    fun resendEmailVerificationByEmail(email: String): EmailVerificationResult {
        val user = userRepository.findByEmail(email)
            ?: return EmailVerificationResult.UserNotFound
            
        return sendEmailVerificationForUser(user)
    }

    private fun sendEmailVerificationForUser(user: User): EmailVerificationResult {
        if (user.isEmailVerified) {
            return EmailVerificationResult.AlreadyVerified
        }
        
        // Invalidate existing tokens first
        emailVerificationTokenRepository.invalidateAllTokensForUser(user.id)
        
        // Generate new token - let database assign ID
        val token = EmailVerificationToken(
            userId = user.id,
            token = TokenGenerationService.generateEmailVerificationToken(),
            email = user.email,
            expiresAt = LocalDateTime.now().plusHours(24) // 24 hours expiration
        )
        
        val savedToken = emailVerificationTokenRepository.save(token)
        
        // Send email
        emailService.sendEmailVerification(user.email, user.firstName, savedToken.token)
        
        return EmailVerificationResult.Success
    }
    
    /**
     * Verifies an email using the provided token.
     */
    fun verifyEmail(tokenString: String): EmailVerificationResult {
        val token = emailVerificationTokenRepository.findByToken(tokenString)
            ?: return EmailVerificationResult.InvalidToken
            
        if (!token.isValid()) {
            return if (token.isExpired()) {
                EmailVerificationResult.TokenExpired
            } else {
                EmailVerificationResult.TokenAlreadyUsed
            }
        }
        
        val user = userRepository.findById(token.userId)
            ?: return EmailVerificationResult.UserNotFound
            
        // Mark token as used
        emailVerificationTokenRepository.save(token.markAsUsed())
        
        // Update user as verified
        val verifiedUser = user.copy(isEmailVerified = true, isActive = true)
        userRepository.save(verifiedUser)
        
        // Invalidate all other tokens for this user
        emailVerificationTokenRepository.invalidateAllTokensForUser(token.userId)
        
        return EmailVerificationResult.Success
    }
}

/**
 * Results for email verification operations.
 */
sealed class EmailVerificationResult {
    object Success : EmailVerificationResult()
    object UserNotFound : EmailVerificationResult()
    object AlreadyVerified : EmailVerificationResult()
    object InvalidToken : EmailVerificationResult()
    object TokenExpired : EmailVerificationResult()
    object TokenAlreadyUsed : EmailVerificationResult()
}