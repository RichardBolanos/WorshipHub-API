package com.worshiphub.domain.scheduling

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for ServiceEvent domain entity.
 */
class ServiceEventTest {
    
    @Test
    fun `should create service event with valid data`() {
        val teamId = UUID.randomUUID()
        val setlistId = UUID.randomUUID()
        val eventDate = LocalDateTime.now().plusDays(7)
        
        val serviceEvent = ServiceEvent(
            id = UUID.randomUUID(),
            name = "Sunday Morning Service",
            scheduledDate = eventDate,
            teamId = teamId,
            setlistId = setlistId,
            churchId = UUID.randomUUID()
        )
        
        assertEquals("Sunday Morning Service", serviceEvent.name)
        assertEquals(eventDate, serviceEvent.scheduledDate)
        assertEquals(teamId, serviceEvent.teamId)
        assertEquals(setlistId, serviceEvent.setlistId)
    }
}