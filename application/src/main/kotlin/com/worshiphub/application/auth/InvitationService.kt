package com.worshiphub.application.auth

import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import com.worshiphub.domain.auth.service.TokenGenerationService
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for invitation operations.
 */
@Service
@Transactional
class InvitationService(
    private val invitationTokenRepository: InvitationTokenRepository,
    private val userRepository: UserRepository,
    private val churchRepository: ChurchRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator,
    private val rateLimitService: InvitationRateLimitService
) {
    
    /**
     * Sends an invitation to join a church.
     */
    fun sendInvitation(command: SendInvitationCommand): InvitationResult {
        // Validate email domain
        if (!rateLimitService.isValidEmailDomain(command.email)) {
            return InvitationResult.InvalidEmailDomain
        }
        
        // Check rate limiting
        if (!rateLimitService.canSendInvitation(command.invitedBy)) {
            return InvitationResult.RateLimitExceeded
        }
        
        // Check if user already exists
        if (userRepository.existsByEmail(command.email)) {
            return InvitationResult.UserAlreadyExists
        }
        
        // Verify church exists
        val church = churchRepository.findById(command.churchId)
            ?: return InvitationResult.ChurchNotFound
            
        // Verify inviter exists and has permission
        val inviter = userRepository.findById(command.invitedBy)
            ?: return InvitationResult.InviterNotFound
            
        if (!canInviteUsers(inviter.role)) {
            return InvitationResult.InsufficientPermissions
        }
        
        // Invalidate existing invitations for this email
        invitationTokenRepository.invalidateAllInvitationsForEmail(command.email)
        
        // Create new invitation
        val invitation = InvitationToken(
            token = TokenGenerationService.generateInvitationToken(),
            email = command.email,
            firstName = command.firstName,
            lastName = command.lastName,
            churchId = command.churchId,
            role = command.role,
            invitedBy = command.invitedBy,
            expiresAt = LocalDateTime.now().plusDays(InvitationToken.EXPIRATION_DAYS)
        )
        
        invitationTokenRepository.save(invitation)
        
        // Record invitation sent for rate limiting
        rateLimitService.recordInvitationSent(command.invitedBy)
        
        // Send invitation email
        emailService.sendInvitation(
            email = command.email,
            firstName = command.firstName,
            churchName = church.name,
            invitationToken = invitation.token
        )
        
        return InvitationResult.Success(invitation.id)
    }
    
    /**
     * Accepts an invitation and creates the user account.
     */
    fun acceptInvitation(token: String, password: String): InvitationResult {
        val invitation = invitationTokenRepository.findByToken(token)
            ?: return InvitationResult.InvalidToken
            
        if (!invitation.isValid()) {
            return if (invitation.isExpired()) {
                InvitationResult.InvitationExpired
            } else {
                InvitationResult.InvitationAlreadyUsed
            }
        }
        
        // Check if user already exists (race condition protection)
        if (userRepository.existsByEmail(invitation.email)) {
            return InvitationResult.UserAlreadyExists
        }
        
        // Validate password
        when (val validation = passwordValidator.validate(password)) {
            is PasswordValidationResult.Invalid -> {
                return InvitationResult.InvalidPassword(validation.errors)
            }
            is PasswordValidationResult.Valid -> {
                // Continue with acceptance
            }
        }
        
        // Create user account
        val hashedPassword = passwordEncoder.encode(password)
        val user = User(
            email = invitation.email,
            firstName = invitation.firstName,
            lastName = invitation.lastName,
            passwordHash = hashedPassword,
            churchId = invitation.churchId,
            role = invitation.role,
            isActive = true,
            isEmailVerified = true // Email is pre-verified through invitation
        )
        
        val savedUser = userRepository.save(user)
        
        // Mark invitation as used
        invitationTokenRepository.save(invitation.markAsUsed())
        
        // Invalidate all other invitations for this email
        invitationTokenRepository.invalidateAllInvitationsForEmail(invitation.email)
        
        return InvitationResult.Success(savedUser.id)
    }
    
    /**
     * Gets invitation details by token.
     */
    fun getInvitationDetails(token: String): InvitationDetailsResult {
        val invitation = invitationTokenRepository.findByToken(token)
            ?: return InvitationDetailsResult.NotFound
            
        if (!invitation.isValid()) {
            return if (invitation.isExpired()) {
                InvitationDetailsResult.Expired
            } else {
                InvitationDetailsResult.AlreadyUsed
            }
        }
        
        val church = churchRepository.findById(invitation.churchId)
            ?: return InvitationDetailsResult.NotFound
            
        return InvitationDetailsResult.Success(
            email = invitation.email,
            firstName = invitation.firstName,
            lastName = invitation.lastName,
            churchName = church.name,
            role = invitation.role,
            expiresAt = invitation.expiresAt
        )
    }
    
    private fun canInviteUsers(role: UserRole): Boolean {
        return role in listOf(UserRole.CHURCH_ADMIN, UserRole.WORSHIP_LEADER, UserRole.SUPER_ADMIN)
    }
}

/**
 * Command for sending invitations.
 */
data class SendInvitationCommand(
    val email: String,
    val firstName: String,
    val lastName: String,
    val churchId: UUID,
    val role: UserRole,
    val invitedBy: UUID
)

/**
 * Results for invitation operations.
 */
sealed class InvitationResult {
    data class Success(val id: UUID) : InvitationResult()
    object UserAlreadyExists : InvitationResult()
    object ChurchNotFound : InvitationResult()
    object InviterNotFound : InvitationResult()
    object InsufficientPermissions : InvitationResult()
    object InvalidToken : InvitationResult()
    object InvitationExpired : InvitationResult()
    object InvitationAlreadyUsed : InvitationResult()
    object RateLimitExceeded : InvitationResult()
    object InvalidEmailDomain : InvitationResult()
    data class InvalidPassword(val errors: List<String>) : InvitationResult()
}

/**
 * Results for invitation details.
 */
sealed class InvitationDetailsResult {
    data class Success(
        val email: String,
        val firstName: String,
        val lastName: String,
        val churchName: String,
        val role: UserRole,
        val expiresAt: LocalDateTime
    ) : InvitationDetailsResult()
    object NotFound : InvitationDetailsResult()
    object Expired : InvitationDetailsResult()
    object AlreadyUsed : InvitationDetailsResult()
}