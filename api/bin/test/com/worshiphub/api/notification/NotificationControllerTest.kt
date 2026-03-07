package com.worshiphub.api.notification

import com.worshiphub.application.notification.NotificationApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(NotificationController::class)
class NotificationControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var notificationApplicationService: NotificationApplicationService
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should get user notifications`() {
        val userId = UUID.randomUUID()
        val notifications = listOf(
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "title" to "New Service Scheduled",
                "message" to "You have been invited to Sunday service",
                "type" to "SERVICE_INVITATION",
                "isRead" to false,
                "createdAt" to LocalDateTime.now().toString()
            )
        )
        
        whenever(notificationApplicationService.getUserNotifications(userId)).thenReturn(notifications)
        
        mockMvc.perform(
            get("/api/v1/notifications")
                .header("User-Id", userId.toString())
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$[0].title").value("New Service Scheduled"))
    }
    
    @Test
    @WithMockUser(roles = ["TEAM_MEMBER"])
    fun `should mark notification as read`() {
        val notificationId = UUID.randomUUID()
        
        mockMvc.perform(
            patch("/api/v1/notifications/{notificationId}/read", notificationId)
        )
        .andExpect(status().isNoContent)
    }
}