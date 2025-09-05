package com.worshiphub.api.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@SpringBootTest
@AutoConfigureWebMvc
class SecurityIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
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