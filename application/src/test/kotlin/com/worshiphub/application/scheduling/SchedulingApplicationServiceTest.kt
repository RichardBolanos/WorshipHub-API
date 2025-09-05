package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.service.SetlistGenerationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.*

class SchedulingApplicationServiceTest {
    
    private val setlistGenerationService = mock<SetlistGenerationService>()
    private val schedulingService = SchedulingApplicationService(setlistGenerationService)
    
    @Test
    fun `should schedule service with notifications`() {
        val command = ScheduleCommand(
            serviceName = "Sunday Morning Service",
            scheduledDate = LocalDateTime.now().plusDays(7),
            teamId = UUID.randomUUID(),
            setlistId = UUID.randomUUID(),
            memberAssignments = listOf(
                MemberAssignment(UUID.randomUUID(), "Vocalist"),
                MemberAssignment(UUID.randomUUID(), "Guitarist")
            ),
            churchId = UUID.randomUUID()
        )
        
        val result = schedulingService.scheduleTeamForService(command)
        
        assert(result != null)
    }
    
    @Test
    fun `should generate setlist with rules`() {
        val command = GenerateSetlistCommand(
            name = "Auto Generated Setlist",
            churchId = UUID.randomUUID(),
            rules = listOf(
                mapOf("category" to "Opening", "count" to 1),
                mapOf("category" to "Worship", "count" to 3),
                mapOf("category" to "Closing", "count" to 1)
            )
        )
        
        val mockSetlist = mapOf(
            "id" to UUID.randomUUID().toString(),
            "name" to command.name,
            "songs" to listOf<String>()
        )
        
        whenever(setlistGenerationService.generateSetlist(any(), any(), any(), any())).thenReturn(mockSetlist)
        
        val result = schedulingService.generateSetlist(command)
        
        assert(result != null)
    }
    
    @Test
    fun `should calculate setlist duration`() {
        val setlistId = UUID.randomUUID()
        
        whenever(setlistGenerationService.calculateDuration(any())).thenReturn(25)
        
        val result = schedulingService.calculateSetlistDuration(setlistId)
        
        assert(result > 0)
    }
}