package com.worshiphub.api.integration.collaboration

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationRepository
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

/**
 * E2E integration tests for the Collaboration bounded context.
 * Covers Requirements 19-20: Notifications and Team Chat.
 */
class CollaborationE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 19: Notifications
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 19 - Notifications")
    inner class Notifications {

        // Validates: Requirement 19.1
        @Test
        fun `should get user notifications and return 200 with list containing expected fields`() {
            val registration = testData.registerChurch(adminEmail = "notif-get@testchurch.com")

            // Create a notification directly via repository since there's no "create notification" API
            val notification = notificationRepository.save(
                Notification(
                    userId = registration.adminUserId,
                    title = "New Service Scheduled",
                    message = "A new worship service has been scheduled for Sunday",
                    type = NotificationType.SERVICE_SCHEDULED
                )
            )

            mockMvc.perform(
                get("/api/v1/notifications")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("User-Id", registration.adminUserId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].isRead").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
        }

        // Validates: Requirement 19.2
        @Test
        fun `should mark notification as read and return 204`() {
            val registration = testData.registerChurch(adminEmail = "notif-read@testchurch.com")

            // Create a notification directly via repository
            val notification = notificationRepository.save(
                Notification(
                    userId = registration.adminUserId,
                    title = "Team Assignment",
                    message = "You have been assigned to a new team",
                    type = NotificationType.TEAM_ASSIGNMENT
                )
            )

            mockMvc.perform(
                patch("/api/v1/notifications/${notification.id}/read")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectNoContent()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 20: Team Chat
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 20 - Team Chat")
    inner class TeamChat {

        // Validates: Requirement 20.1
        @Test
        fun `should send chat message via REST and return 201 with expected fields`() {
            val registration = testData.registerChurch(adminEmail = "chat-send@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val request = mapOf("content" to "Hello team! Ready for Sunday?")

            mockMvc.perform(
                post("/api/v1/teams/$teamId/messages")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.teamId").value(teamId.toString()))
                .andExpect(jsonPath("$.userId").value(registration.adminUserId.toString()))
                .andExpect(jsonPath("$.content").value("Hello team! Ready for Sunday?"))
                .andExpect(jsonPath("$.createdAt").exists())
        }

        // Validates: Requirement 20.2
        @Test
        fun `should get chat history and return 200 with list of messages`() {
            val registration = testData.registerChurch(adminEmail = "chat-history@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Send a message first via REST so there's history to retrieve
            val request = mapOf("content" to "First message for history test")

            mockMvc.perform(
                post("/api/v1/teams/$teamId/messages")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()

            // Now retrieve chat history
            mockMvc.perform(
                get("/api/v1/teams/$teamId/chat/history")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("limit", "50")
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].teamId").exists())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
        }
    }
}
