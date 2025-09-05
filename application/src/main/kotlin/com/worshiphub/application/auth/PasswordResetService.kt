package com.worshiphub.application.auth

import com.worshiphub.domain.auth.PasswordResetToken
import com.worshiphub.domain.auth.repository.PasswordResetTokenRepository
import com.worshiphub.domain.auth.service.TokenGenerationService
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for password reset operations.
 */
@Service
@Transactional
class PasswordResetService(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator
) {
    
    /**
     * Initiates password reset process by sending reset email.
     */
    fun initiatePasswordReset(email: String): PasswordResetResult {
        val user = userRepository.findByEmailAndIsActiveTrue(email)
            ?: return PasswordResetResult.Success // Don't reveal if email exists
            
        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateAllTokensForUser(user.id)
        
        // Generate new token
        val token = PasswordResetToken(
            userId = user.id,
            token = TokenGenerationService.generatePasswordResetToken(),
            email = user.email,
            expiresAt = LocalDateTime.now().plusHours(PasswordResetToken.EXPIRATION_HOURS)
        )
        
        passwordResetTokenRepository.save(token)
        
        // Send email
        emailService.sendPasswordReset(user.email, user.firstName, token.token)
        
        return PasswordResetResult.Success
    }
    
    /**
     * Validates a password reset token.
     */
    fun validateResetToken(tokenString: String): PasswordResetResult {
        val token = passwordResetTokenRepository.findByToken(tokenString)
            ?: return PasswordResetResult.InvalidToken
            
        return if (token.isValid()) {
            PasswordResetResult.Success
        } else if (token.isExpired()) {
            PasswordResetResult.TokenExpired
        } else {
            PasswordResetResult.TokenAlreadyUsed
        }
    }
    
    /**
     * Resets password using the provided token and new password.
     */
    fun resetPassword(tokenString: String, newPassword: String): PasswordResetResult {
        val token = passwordResetTokenRepository.findByToken(tokenString)
            ?: return PasswordResetResult.InvalidToken
            
        if (!token.isValid()) {
            return if (token.isExpired()) {
                PasswordResetResult.TokenExpired
            } else {
                PasswordResetResult.TokenAlreadyUsed
            }
        }
        
        // Validate new password
        when (val validation = passwordValidator.validate(newPassword)) {
            is PasswordValidationResult.Invalid -> {
                return PasswordResetResult.InvalidPassword(validation.errors)
            }
            is PasswordValidationResult.Valid -> {
                // Continue with reset
            }
        }
        
        val user = userRepository.findById(token.userId)
            ?: return PasswordResetResult.UserNotFound
            
        // Update password
        val hashedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(passwordHash = hashedPassword)
        userRepository.save(updatedUser)
        
        // Mark token as used
        passwordResetTokenRepository.save(token.markAsUsed())
        
        // Invalidate all other tokens for this user
        passwordResetTokenRepository.invalidateAllTokensForUser(token.userId)
        
        return PasswordResetResult.Success
    }
}

/**
 * Results for password reset operations.
 */
sealed class PasswordResetResult {
    object Success : PasswordResetResult()
    object UserNotFound : PasswordResetResult()
    object InvalidToken : PasswordResetResult()
    object TokenExpired : PasswordResetResult()
    object TokenAlreadyUsed : PasswordResetResult()
    data class InvalidPassword(val errors: List<String>) : PasswordResetResult()
}