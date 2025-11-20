package com.worshiphub.application.auth

import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.application.auth.PasswordValidator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class PasswordManagementService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordValidator: PasswordValidator
) {

    fun setPassword(userId: UUID, password: String): SetPasswordResult {
        val user = userRepository.findById(userId)
            ?: return SetPasswordResult.UserNotFound

        // Check if user already has a password
        if (user.passwordHash != null) {
            return SetPasswordResult.PasswordAlreadyExists
        }

        // Validate password requirements
        val validationResult = passwordValidator.validate(password)
        if (validationResult is PasswordValidationResult.Invalid) {
            return SetPasswordResult.InvalidPassword(validationResult.errors)
        }

        // Set password
        val hashedPassword = passwordEncoder.encode(password)
        val updatedUser = user.copy(passwordHash = hashedPassword)
        userRepository.save(updatedUser)

        return SetPasswordResult.Success
    }

    fun changePassword(userId: UUID, currentPassword: String, newPassword: String): ChangePasswordResult {
        val user = userRepository.findById(userId)
            ?: return ChangePasswordResult.UserNotFound

        // Check if user has a password set
        if (user.passwordHash == null) {
            return ChangePasswordResult.NoPasswordSet
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            return ChangePasswordResult.InvalidCurrentPassword
        }

        // Validate new password requirements
        val validationResult = passwordValidator.validate(newPassword)
        if (validationResult is PasswordValidationResult.Invalid) {
            return ChangePasswordResult.InvalidNewPassword(validationResult.errors)
        }

        // Update password
        val hashedNewPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(passwordHash = hashedNewPassword)
        userRepository.save(updatedUser)

        return ChangePasswordResult.Success
    }

    fun hasPassword(userId: UUID): Boolean {
        val user = userRepository.findById(userId) ?: return false
        return user.passwordHash != null
    }
}

sealed class SetPasswordResult {
    object Success : SetPasswordResult()
    object UserNotFound : SetPasswordResult()
    object PasswordAlreadyExists : SetPasswordResult()
    data class InvalidPassword(val errors: List<String>) : SetPasswordResult()
}

sealed class ChangePasswordResult {
    object Success : ChangePasswordResult()
    object UserNotFound : ChangePasswordResult()
    object NoPasswordSet : ChangePasswordResult()
    object InvalidCurrentPassword : ChangePasswordResult()
    data class InvalidNewPassword(val errors: List<String>) : ChangePasswordResult()
}