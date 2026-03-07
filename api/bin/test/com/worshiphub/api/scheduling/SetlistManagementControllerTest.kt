package com.worshiphub.api.scheduling

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.scheduling.SchedulingApplicationService
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

@WebMvcTest(SetlistManagementController::class)
class SetlistManagementControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var schedulingApplicationService: SchedulingApplicationService
    
    @Test
    @WithMockUser(roles = ["WORSHIP_LEADER"])
    fun `should add song to setlist`() {
        val setlistId = UUID.randomUUID()
        val songId = UUID.randomUUID()
        val request = mapOf(
            "songId" to songId.toString(),
            "position" to 1
        )
        
        mockMvc.perform(
            post("/api/v1/services/setlists/{setlistId}/songs", setlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
    }
    
    @Test
    @WithMockUser(roles = ["WORSHIP_LEADER"])
    fun `should remove song from setlist`() {
        val setlistId = UUID.randomUUID()
        val songId = UUID.randomUUID()
        
        mockMvc.perform(
            delete("/api/v1/services/setlists/{setlistId}/songs/{songId}", setlistId, songId)
        )
        .andExpect(status().isNoContent)
    }
    
    @Test
    @WithMockUser(roles = ["WORSHIP_LEADER"])
    fun `should reorder setlist songs`() {
        val setlistId = UUID.randomUUID()
        val request = mapOf(
            "songOrder" to listOf(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            )
        )
        
        mockMvc.perform(
            patch("/api/v1/services/setlists/{setlistId}/songs/reorder", setlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk)
    }
    
    @Test
    fun `should get setlist details`() {
        val setlistId = UUID.randomUUID()
        val mockSetlist = mapOf(
            "id" to setlistId.toString(),
            "name" to "Sunday Morning",
            "songs" to listOf<String>(),
            "totalDuration" to 25
        )
        
        whenever(schedulingApplicationService.getSetlistDetails(setlistId)).thenReturn(mockSetlist)
        
        mockMvc.perform(
            get("/api/v1/services/setlists/{setlistId}", setlistId)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.name").value("Sunday Morning"))
    }
}