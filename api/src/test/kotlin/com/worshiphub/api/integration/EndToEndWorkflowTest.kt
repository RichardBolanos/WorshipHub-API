package com.worshiphub.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@SpringBootTest
@AutoConfigureWebMvc
class EndToEndWorkflowTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `should complete church registration workflow`() {
        val churchRequest = mapOf(
            "name" to "Test Church",
            "address" to "123 Test St",
            "email" to "admin@testchurch.org"
        )
        
        mockMvc.perform(
            post("/api/v1/churches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(churchRequest))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.churchId").exists())
    }
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should complete notification workflow`() {
        val userId = UUID.randomUUID()
        
        mockMvc.perform(
            get("/api/v1/notifications")
                .header("User-Id", userId.toString())
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
    }
}