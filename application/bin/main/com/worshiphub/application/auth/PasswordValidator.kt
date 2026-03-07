package com.worshiphub.application.auth

import org.springframework.stereotype.Component

@Component
class PasswordValidator {

    fun validate(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()
        
        if (password.length < 8) {
            errors.add("Password must be at least 8 characters long")
        }
        
        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }
        
        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }
        
        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one digit")
        }
        
        if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) {
            errors.add("Password must contain at least one special character")
        }
        
        return if (errors.isEmpty()) {
            PasswordValidationResult.Valid
        } else {
            PasswordValidationResult.Invalid(errors)
        }
    }
}

sealed class PasswordValidationResult {
    object Valid : PasswordValidationResult()
    data class Invalid(val errors: List<String>) : PasswordValidationResult()
}