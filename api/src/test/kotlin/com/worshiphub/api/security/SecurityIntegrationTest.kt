package com.worshiphub.api.security

import com.worshiphub.api.integration.BaseE2ETest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

/**
 * Cross-cutting security checks against the live Spring context.
 *
 * Extends [BaseE2ETest] so we run with profile=h2, the H2 in-memory DB,
 * Flyway disabled, and a no-op email service. That gives us a real security
 * filter chain to assert against.
 */
class SecurityIntegrationTest : BaseE2ETest() {

    @Test
    fun `should reject requests without JWT token`() {
        val request = mapOf(
            "name" to "Test Church",
            "address" to "123 Test St",
            "email" to "test@church.org"
        )

        mockMvc.perform(
            post("/api/v1/churches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should enforce password policies`() {
        val request = mapOf(
            "email" to "newuser@church.org",
            "password" to "weak",
            "firstName" to "John",
            "lastName" to "Doe",
            "churchId" to UUID.randomUUID().toString(),
            "role" to "TEAM_MEMBER"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}