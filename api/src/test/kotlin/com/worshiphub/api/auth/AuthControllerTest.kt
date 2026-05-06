package com.worshiphub.api.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.api.support.WebMvcSecurityTestConfig
import com.worshiphub.application.auth.AuthenticationResult
import com.worshiphub.application.auth.AuthenticationService
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.security.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // We mark these MockitoBean here because the controller depends directly on them.
    // The @Primary mocks from WebMvcSecurityTestConfig would otherwise be ambiguous
    // for `authenticationService` (which is not in that config) so we declare it
    // explicitly. For `jwtTokenProvider` we override the WebMvcSecurityTestConfig
    // mock so we can stub `generateToken`.
    @MockitoBean
    private lateinit var authenticationService: AuthenticationService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private fun sampleUser() = User(
        id = UUID.randomUUID(),
        email = "admin@church.com",
        firstName = "Admin",
        lastName = "User",
        passwordHash = "hashed",
        churchId = UUID.randomUUID(),
        role = UserRole.CHURCH_ADMIN,
        isActive = true,
        isEmailVerified = true
    )

    @Test
    fun `should login with valid credentials`() {
        val user = sampleUser()
        whenever(authenticationService.authenticate(eq("admin@church.com"), eq("password123")))
            .thenReturn(AuthenticationResult.Success(user))
        whenever(jwtTokenProvider.generateToken(any(), any(), any()))
            .thenReturn("fake-jwt-token")

        val request = mapOf(
            "email" to "admin@church.com",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("fake-jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("admin@church.com"))
            .andExpect(jsonPath("$.user.role").value("CHURCH_ADMIN"))
    }

    @Test
    fun `should reject invalid credentials`() {
        whenever(authenticationService.authenticate(eq("admin@church.com"), eq("wrongpassword")))
            .thenReturn(AuthenticationResult.Failure("Invalid credentials"))

        val request = mapOf(
            "email" to "admin@church.com",
            "password" to "wrongpassword"
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
    }
}