package com.worshiphub.application.auth

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import kotlin.test.assertTrue

class AuthenticationServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val passwordValidator = mockk<PasswordValidator>()
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
            role = UserRole.TEAM_MEMBER,
            isEmailVerified = true
        )

        every { userRepository.findByEmail(email) } returns user
        every { passwordEncoder.matches(password, hashedPassword) } returns true

        val result = authService.authenticate(email, password)

        assertTrue(result is AuthenticationResult.Success)
    }

    @Test
    fun `should reject authentication with invalid credentials`() {
        val email = "user@church.org"

        every { userRepository.findByEmail(email) } returns null

        val result = authService.authenticate(email, "wrongpassword")

        assertTrue(result is AuthenticationResult.Failure)
    }
}
