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

@WebMvcTest(controllers = [UserController::class, UserProfileController::class])
class UserControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var organizationApplicationService: OrganizationApplicationService
    
    @Test
    @WithMockUser(roles = ["CHURCH_ADMIN"])
    fun `should invite user with CHURCH_ADMIN role`() {
        val userId = UUID.randomUUID()
        val churchId = UUID.randomUUID()
        val request = mapOf(
            "email" to "newuser@church.org",
            "firstName" to "John",
            "lastName" to "Doe",
            "role" to "TEAM_MEMBER"
        )
        
        whenever(organizationApplicationService.inviteUser(any())).thenReturn(userId)
        
        mockMvc.perform(
            post("/api/v1/users/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", churchId.toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.userId").value(userId.toString()))
    }
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should get user profile`() {
        val userId = UUID.randomUUID()
        val mockUser = mapOf(
            "id" to userId.toString(),
            "email" to "user@church.org",
            "firstName" to "John",
            "lastName" to "Doe",
            "role" to "TEAM_MEMBER"
        )
        
        whenever(organizationApplicationService.getUserProfile(any())).thenReturn(mockUser)
        
        mockMvc.perform(
            get("/api/v1/users/profile")
                .header("User-Id", userId.toString())
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.email").value("user@church.org"))
    }
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should update user profile`() {
        val userId = UUID.randomUUID()
        val request = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith"
        )
        
        mockMvc.perform(
            patch("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Id", userId.toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk)
    }
}