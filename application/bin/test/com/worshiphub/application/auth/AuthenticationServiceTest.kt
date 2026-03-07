package com.worshiphub.application.auth

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import kotlin.test.assertTrue

class AuthenticationServiceTest {
    
    private val userRepository = mock<UserRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val passwordValidator = mock<PasswordValidator>()
    private val authService = AuthenticationService(userRepository, passwordEncoder, passwordValidator)
    
    @Test
    fun `should authenticate user with valid credentials`() {
        val email = "user@church.org"
        val password = "password123"
        val hashedPassword = "hashed-password"
        
        val user = User(
            email = email,
            firstName = "John",
            lastName = "Doe",
            passwordHash = hashedPassword,
            churchId = UUID.randomUUID(),
            role = UserRole.TEAM_MEMBER
        )
        
        whenever(userRepository.findByEmailAndIsActiveTrue(email)).thenReturn(user)
        whenever(passwordEncoder.matches(password, hashedPassword)).thenReturn(true)
        
        val result = authService.authenticate(email, password)
        
        assert(result != null)
    }
    
    @Test
    fun `should reject authentication with invalid credentials`() {
        val email = "user@church.org"
        val password = "wrongpassword"
        
        whenever(userRepository.findByEmailAndIsActiveTrue(email)).thenReturn(null)
        
        val result = authService.authenticate(email, password)
        
        assert(result == null)
    }
}