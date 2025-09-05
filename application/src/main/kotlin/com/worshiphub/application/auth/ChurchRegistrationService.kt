package com.worshiphub.application.auth

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for complete church registration with admin user.
 */
@Service
@Transactional
class ChurchRegistrationService(
    private val churchRepository: ChurchRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator,
    private val emailVerificationService: EmailVerificationService,
    private val emailService: EmailService
) {
    
    /**
     * Registers a new church with its first admin user.
     */
    fun registerChurchWithAdmin(command: RegisterChurchWithAdminCommand): ChurchRegistrationResult {
        // Check if admin email already exists
        if (userRepository.existsByEmail(command.adminEmail)) {
            return ChurchRegistrationResult.EmailAlreadyExists
        }
        
        // Validate password
        when (val validation = passwordValidator.validate(command.adminPassword)) {
            is PasswordValidationResult.Invalid -> {
                return ChurchRegistrationResult.InvalidPassword(validation.errors)
            }
            is PasswordValidationResult.Valid -> {
                // Continue with registration
            }
        }
        
        // Create church
        val church = Church(
            name = command.churchName,
            address = command.churchAddress,
            email = command.churchEmail
        )
        val savedChurch = churchRepository.save(church)
        
        // Create admin user
        val hashedPassword = passwordEncoder.encode(command.adminPassword)
        val adminUser = User(
            email = command.adminEmail,
            firstName = command.adminFirstName,
            lastName = command.adminLastName,
            passwordHash = hashedPassword,
            churchId = savedChurch.id,
            role = UserRole.CHURCH_ADMIN,
            isActive = false, // Will be activated after email verification
            isEmailVerified = false
        )
        val savedUser = userRepository.save(adminUser)
        
        // Send email verification
        emailVerificationService.sendEmailVerification(savedUser.id)
        
        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.email, savedUser.firstName, savedChurch.name)
        
        return ChurchRegistrationResult.Success(savedChurch.id, savedUser.id)
    }
}

/**
 * Command for registering church with admin.
 */
data class RegisterChurchWithAdminCommand(
    val churchName: String,
    val churchAddress: String,
    val churchEmail: String,
    val adminEmail: String,
    val adminFirstName: String,
    val adminLastName: String,
    val adminPassword: String
)

/**
 * Results for church registration operations.
 */
sealed class ChurchRegistrationResult {
    data class Success(val churchId: UUID, val adminUserId: UUID) : ChurchRegistrationResult()
    object EmailAlreadyExists : ChurchRegistrationResult()
    data class InvalidPassword(val errors: List<String>) : ChurchRegistrationResult()
}