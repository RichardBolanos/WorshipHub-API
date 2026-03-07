package com.worshiphub.api.organization

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.organization.OrganizationApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

/**
 * Integration tests for TeamController.
 */
@WebMvcTest(TeamController::class)
class TeamControllerIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var organizationApplicationService: OrganizationApplicationService
    
    @Test
    fun `should create team successfully`() {
        val teamId = UUID.randomUUID()
        val request = CreateTeamRequest(
            name = "Worship Team",
            description = "Main worship team",
            leaderId = UUID.randomUUID()
        )
        
        whenever(organizationApplicationService.createTeam(any())).thenReturn(teamId)
        
        mockMvc.perform(
            post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.teamId").value(teamId.toString()))
    }
}