package com.worshiphub.application.auth

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
open class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator
) {

    fun authenticate(email: String, password: String): AuthenticationResult {
        return try {
            val user = userRepository.findByEmail(email)
                ?: return AuthenticationResult.Failure("Invalid credentials")

            if (!passwordEncoder.matches(password, user.passwordHash)) {
                return AuthenticationResult.Failure("Invalid credentials")
            }
            
            if (!user.isEmailVerified) {
                return AuthenticationResult.EmailNotVerified
            }
            
            if (!user.isActive) {
                return AuthenticationResult.AccountInactive
            }

            AuthenticationResult.Success(user)
        } catch (e: Exception) {
            AuthenticationResult.Failure("Authentication failed")
        }
    }

    fun register(command: RegisterUserCommand): RegisterResult {
        return try {
            if (userRepository.existsByEmail(command.email)) {
                return RegisterResult.Failure("User with this email already exists")
            }
            
            when (val validation = passwordValidator.validate(command.password)) {
                is PasswordValidationResult.Invalid -> {
                    return RegisterResult.Failure(validation.errors.joinToString("; "))
                }
                is PasswordValidationResult.Valid -> {
                    // Continue with registration
                }
            }

            val hashedPassword = passwordEncoder.encode(command.password)
            
            val user = User(
                email = command.email,
                firstName = command.firstName,
                lastName = command.lastName,
                passwordHash = hashedPassword,
                churchId = command.churchId,
                role = command.role,
                isActive = false,
                isEmailVerified = false
            )

            val savedUser = userRepository.save(user)
            RegisterResult.Success(savedUser.id)
        } catch (e: Exception) {
            RegisterResult.Failure("Registration failed")
        }
    }
}

data class RegisterUserCommand(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val churchId: UUID,
    val role: UserRole = UserRole.TEAM_MEMBER
)

sealed class AuthenticationResult {
    data class Success(val user: User) : AuthenticationResult()
    data class Failure(val message: String) : AuthenticationResult()
    object EmailNotVerified : AuthenticationResult()
    object AccountInactive : AuthenticationResult()
}

sealed class RegisterResult {
    data class Success(val userId: UUID) : RegisterResult()
    data class Failure(val message: String) : RegisterResult()
}