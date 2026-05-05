package com.worshiphub.api.integration.security

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

/**
 * E2E integration tests for role-based access control (RBAC).
 * Covers Requirement 21: Verifies that endpoints enforce role restrictions correctly.
 */
class RoleBasedAccessE2ETest : BaseE2ETest() {

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 21: Role-Based Access Control
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 21 - Role-Based Access Control")
    inner class RoleBasedAccessControl {

        // Validates: Requirement 21.1
        @Test
        fun `should return 403 when TEAM_MEMBER attempts to create team`() {
            val registration = testData.registerChurch(adminEmail = "rbac-team-create@testchurch.com")

            val request = mapOf(
                "name" to "Unauthorized Team",
                "description" to "Should not be created",
                "leaderId" to registration.adminUserId.toString()
            )

            mockMvc.perform(
                post("/api/v1/teams")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.2
        @Test
        fun `should return 403 when TEAM_MEMBER attempts to create song`() {
            val registration = testData.registerChurch(adminEmail = "rbac-song-create@testchurch.com")

            val request = mapOf(
                "title" to "Unauthorized Song",
                "artist" to "Unknown Artist"
            )

            mockMvc.perform(
                post("/api/v1/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.3
        @Test
        fun `should return 403 when TEAM_MEMBER attempts to schedule service`() {
            val registration = testData.registerChurch(adminEmail = "rbac-svc-create@testchurch.com")

            val request = mapOf(
                "serviceName" to "Unauthorized Service",
                "scheduledDate" to LocalDateTime.now().plusDays(7).toString(),
                "teamId" to UUID.randomUUID().toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to UUID.randomUUID().toString(), "role" to "Lead Vocalist")
                )
            )

            mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.4
        @Test
        fun `should return 403 when TEAM_MEMBER attempts to change user role`() {
            val registration = testData.registerChurch(adminEmail = "rbac-role-change@testchurch.com")

            val request = mapOf("newRole" to "WORSHIP_LEADER")

            mockMvc.perform(
                put("/api/v1/roles/users/${UUID.randomUUID()}")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.5
        @Test
        fun `should return 403 when TEAM_MEMBER attempts to send invitation`() {
            val registration = testData.registerChurch(adminEmail = "rbac-invite@testchurch.com")

            val request = mapOf(
                "email" to "unauthorized-invite@testchurch.com",
                "firstName" to "Unauthorized",
                "lastName" to "Invite",
                "role" to "TEAM_MEMBER"
            )

            mockMvc.perform(
                post("/api/v1/invitations/send")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.6
        @Test
        fun `should return 403 when WORSHIP_LEADER attempts to delete team`() {
            val registration = testData.registerChurch(adminEmail = "rbac-team-delete@testchurch.com")

            // Create a team as admin first
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Try to delete as WORSHIP_LEADER
            mockMvc.perform(
                delete("/api/v1/teams/$teamId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("WORSHIP_LEADER")
                        )
                    )
            )
                .expectForbidden()
        }

        // Validates: Requirement 21.7
        @Test
        fun `should return 401 when unauthenticated request is made to protected endpoint`() {
            // Make a request to a protected endpoint without any authentication
            mockMvc.perform(
                get("/api/v1/roles/users")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
