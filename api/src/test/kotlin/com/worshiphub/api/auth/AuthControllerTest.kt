package com.worshiphub.api.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.auth.AuthenticationService
import com.worshiphub.security.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@WebMvcTest(AuthController::class)
class AuthControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var authenticationService: AuthenticationService
    
    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    @Test
    fun `should login with valid credentials`() {
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
    }
    
    @Test
    fun `should reject invalid credentials`() {
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
    }
}