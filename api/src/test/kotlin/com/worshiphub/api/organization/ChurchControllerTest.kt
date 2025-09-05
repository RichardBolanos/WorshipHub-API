package com.worshiphub.api.organization

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.organization.OrganizationApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

@WebMvcTest(ChurchController::class)
class ChurchControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var organizationApplicationService: OrganizationApplicationService
    
    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `should register church with SUPER_ADMIN role`() {
        val churchId = UUID.randomUUID()
        val request = mapOf(
            "name" to "First Baptist Church",
            "address" to "123 Main St, City, State",
            "email" to "admin@firstbaptist.org"
        )
        
        whenever(organizationApplicationService.registerChurch(any())).thenReturn(churchId)
        
        mockMvc.perform(
            post("/api/v1/churches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.churchId").value(churchId.toString()))
    }
    
    @Test
    @WithMockUser(roles = ["CHURCH_ADMIN"])
    fun `should reject church registration without SUPER_ADMIN role`() {
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
        .andExpect(status().isForbidden)
    }
}