package com.worshiphub.application.auth

import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.core.env.Environment
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import org.slf4j.LoggerFactory

/**
 * Application service for complete church registration with admin user.
 */
@Service
class ChurchRegistrationService(
    private val churchRepository: ChurchRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator,
    private val emailVerificationService: EmailVerificationService,
    private val emailService: EmailService,
    private val environment: Environment
) {
    
    private val logger = LoggerFactory.getLogger(ChurchRegistrationService::class.java)

    /**
     * Whether the H2 (development/testing) profile is active.
     * When true, newly registered users are auto-verified and activated
     * so that E2E tests can login immediately without email verification.
     */
    private val isH2Profile: Boolean
        get() = environment.activeProfiles.contains("h2")
    
    /**
     * Registers a new church with its first admin user.
     */
    @Transactional
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
        logger.info("Created church domain object: id=${church.id}, name=${church.name}")
        
        val savedChurch = churchRepository.save(church)
        logger.info("Church saved successfully: id=${savedChurch.id}")

        // In H2 profile (dev/testing), auto-verify and activate the user
        // so that E2E tests can login immediately without email verification.
        val autoVerify = isH2Profile
        if (autoVerify) {
            logger.info("H2 profile active — auto-verifying and activating admin user")
        }
        
        // Create admin user
        val hashedPassword = passwordEncoder.encode(command.adminPassword)
        val adminUser = User(
            email = command.adminEmail,
            firstName = command.adminFirstName,
            lastName = command.adminLastName,
            passwordHash = hashedPassword,
            churchId = savedChurch.id,
            role = UserRole.CHURCH_ADMIN,
            isActive = autoVerify,
            isEmailVerified = autoVerify
        )
        val savedUser = userRepository.save(adminUser)

        if (!autoVerify) {
            // Send email verification (production flow)
            emailVerificationService.sendEmailVerification(savedUser.id)
        }
        
        // Send welcome email (no-op in H2 profile thanks to NoOpEmailService)
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