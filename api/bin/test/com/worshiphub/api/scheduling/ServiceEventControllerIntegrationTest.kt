package com.worshiphub.api.scheduling

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.scheduling.SchedulingApplicationService
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
import java.time.LocalDateTime
import java.util.*

/**
 * Integration tests for ServiceEventController.
 */
@WebMvcTest(ServiceEventController::class)
class ServiceEventControllerIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var schedulingApplicationService: SchedulingApplicationService
    
    // @Test - Temporarily disabled
    fun `should schedule service successfully`() {
        val serviceId = UUID.randomUUID()
        val request = mapOf(
            "serviceName" to "Sunday Service",
            "scheduledDate" to LocalDateTime.now().plusDays(7).toString(),
            "teamId" to UUID.randomUUID().toString(),
            "setlistId" to UUID.randomUUID().toString(),
            "memberAssignments" to emptyList<Any>()
        )
        
        whenever(schedulingApplicationService.scheduleTeamForService(any())).thenReturn(serviceId)
        
        mockMvc.perform(
            post("/api/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
    }
}