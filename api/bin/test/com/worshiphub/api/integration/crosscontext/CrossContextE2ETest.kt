package com.worshiphub.api.integration.crosscontext

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
import com.worshiphub.domain.auth.InvitationToken
import com.worshiphub.domain.auth.repository.InvitationTokenRepository
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.UserRole
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

/**
 * E2E integration tests for cross-context flows.
 * Covers Requirement 22: Multi-context flows that span multiple bounded contexts.
 */
class CrossContextE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var serviceEventRepository: ServiceEventRepository

    @Autowired
    lateinit var invitationTokenRepository: InvitationTokenRepository

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 22: Cross-Context Flows
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 22 - Cross-Context Flows")
    inner class CrossContextFlows {

        // Validates: Requirement 22.1
        @Test
        fun `should complete full worship service preparation flow`() {
            // Step 1: Register church
            val registration = testData.registerChurch(
                adminEmail = "cross-svc-admin@testchurch.com",
                churchName = "Cross Context Church"
            )
            val adminUserId = registration.adminUserId
            val churchId = registration.churchId
            val adminAuth = TestSecurityHelper.withAuth(adminUserId, churchId, listOf("CHURCH_ADMIN"))

            // Step 2: Create team
            val teamId = testData.createTeam(
                churchId = churchId,
                leaderId = adminUserId,
                name = "Sunday Worship Team"
            )

            // Step 3: Create members and assign to team
            val member1 = userRepository.save(
                User(
                    email = "cross-member1@testchurch.com",
                    firstName = "Vocalist",
                    lastName = "One",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val member2 = userRepository.save(
                User(
                    email = "cross-member2@testchurch.com",
                    firstName = "Guitarist",
                    lastName = "Two",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            // Assign members to team
            mockMvc.perform(
                post("/api/v1/teams/$teamId/members")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf(
                        "userId" to member1.id.toString(),
                        "teamRole" to "LEAD_VOCALIST"
                    )))
            ).expectCreated()

            mockMvc.perform(
                post("/api/v1/teams/$teamId/members")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf(
                        "userId" to member2.id.toString(),
                        "teamRole" to "ELECTRIC_GUITAR"
                    )))
            ).expectCreated()

            // Step 4: Create songs
            val songId = testData.createSong(
                userId = adminUserId,
                churchId = churchId,
                title = "Cross Context Song",
                artist = "Test Artist"
            )

            // Step 5: Create setlist with the song
            val setlistId = testData.createSetlist(
                userId = adminUserId,
                churchId = churchId,
                name = "Sunday Morning Setlist",
                songIds = listOf(songId)
            )

            // Step 6: Schedule service with member assignments
            val futureDate = LocalDateTime.now().plusDays(7)
            val scheduleRequest = mapOf(
                "serviceName" to "Sunday Morning Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "setlistId" to setlistId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member1.id.toString(), "role" to "Lead Vocalist"),
                    mapOf("userId" to member2.id.toString(), "role" to "Guitarist")
                )
            )

            val scheduleResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(adminAuth)
                    .header("Church-Id", churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(scheduleRequest))
            )
                .expectCreated()
                .andExpect(jsonPath("$.serviceId").exists())

            val serviceId = scheduleResult.extractUUID("$.serviceId")

            // Step 7: Members respond to invitations
            val serviceEvent = serviceEventRepository.findById(serviceId)!!
            val assignment1 = serviceEvent.assignedMembers.first { it.userId == member1.id }
            val assignment2 = serviceEvent.assignedMembers.first { it.userId == member2.id }

            // Member 1 accepts
            mockMvc.perform(
                patch("/api/v1/services/$serviceId/assignments/${assignment1.id}")
                    .with(TestSecurityHelper.withAuth(member1.id, churchId, listOf("TEAM_MEMBER")))
                    .header("User-Id", member1.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("response" to "ACCEPTED")))
            ).expectOk()

            // Member 2 accepts
            mockMvc.perform(
                patch("/api/v1/services/$serviceId/assignments/${assignment2.id}")
                    .with(TestSecurityHelper.withAuth(member2.id, churchId, listOf("TEAM_MEMBER")))
                    .header("User-Id", member2.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("response" to "ACCEPTED")))
            ).expectOk()

            // Step 8: Verify confirmation status
            mockMvc.perform(
                get("/api/v1/services/$serviceId/confirmations")
                    .with(adminAuth)
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].status").exists())
        }

        // Validates: Requirement 22.2
        @Test
        fun `should complete full member onboarding flow`() {
            // Step 1: Register church
            val registration = testData.registerChurch(
                adminEmail = "cross-onboard-admin@testchurch.com",
                churchName = "Onboarding Church"
            )
            val adminUserId = registration.adminUserId
            val churchId = registration.churchId
            val adminAuth = TestSecurityHelper.withAuth(adminUserId, churchId, listOf("CHURCH_ADMIN"))

            // Step 2: Create a team for the new member to access
            val teamId = testData.createTeam(
                churchId = churchId,
                leaderId = adminUserId,
                name = "Onboarding Team"
            )

            // Step 3: Send invitation (create token directly for reliable test)
            val invitationToken = "onboard-token-${UUID.randomUUID()}"
            val invitation = invitationTokenRepository.save(
                InvitationToken(
                    token = invitationToken,
                    email = "new-member-onboard@testchurch.com",
                    firstName = "New",
                    lastName = "Member",
                    churchId = churchId,
                    role = UserRole.TEAM_MEMBER,
                    invitedBy = adminUserId,
                    expiresAt = LocalDateTime.now().plusDays(7)
                )
            )

            // Step 4: Accept invitation
            val acceptResult = mockMvc.perform(
                post("/api/v1/invitations/$invitationToken/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf(
                        "password" to TestConstants.VALID_PASSWORD
                    )))
            )
                .expectCreated()
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.message").exists())

            val newUserId = acceptResult.extractUUID("$.userId")

            // Step 5: Verify user can access team endpoints
            // The new user should be able to list teams (TEAM_MEMBER has access to GET /api/v1/teams)
            mockMvc.perform(
                get("/api/v1/teams")
                    .with(TestSecurityHelper.withAuth(newUserId, churchId, listOf("TEAM_MEMBER")))
                    .header("Church-Id", churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
        }

        // Validates: Requirement 22.3
        @Test
        fun `should complete full catalog management flow`() {
            // Step 1: Register church
            val registration = testData.registerChurch(
                adminEmail = "cross-catalog-admin@testchurch.com",
                churchName = "Catalog Church"
            )
            val adminUserId = registration.adminUserId
            val churchId = registration.churchId
            val adminAuth = TestSecurityHelper.withAuth(adminUserId, churchId, listOf("CHURCH_ADMIN"))

            // Step 2: Create categories
            val categoryId = testData.createCategory(
                userId = adminUserId,
                churchId = churchId,
                name = "Worship",
                description = "Songs for worship time"
            )

            // Step 3: Create tags
            val tagId = testData.createTag(
                userId = adminUserId,
                churchId = churchId,
                name = "Contemporary",
                color = "#FF5733"
            )

            // Step 4: Create song
            val songId = testData.createSong(
                userId = adminUserId,
                churchId = churchId,
                title = "Catalog Flow Song",
                artist = "Flow Artist",
                key = "G",
                bpm = 120,
                lyrics = "Test lyrics for catalog flow",
                chords = "[G]Test [C]chords"
            )

            // Step 5: Assign categories to song
            mockMvc.perform(
                post("/api/v1/songs/$songId/categories")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(listOf(categoryId)))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Categories assigned successfully"))

            // Step 6: Assign tags to song
            mockMvc.perform(
                post("/api/v1/songs/$songId/tags")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(listOf(tagId)))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Tags assigned successfully"))

            // Step 7: Add attachment to song
            val attachmentRequest = mapOf(
                "name" to "Lead Sheet",
                "url" to "https://example.com/leadsheet.pdf",
                "type" to "PDF_SHEET"
            )

            mockMvc.perform(
                post("/api/v1/songs/$songId/attachments")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(attachmentRequest))
            )
                .expectCreated()
                .andExpect(jsonPath("$.attachmentId").exists())
                .andExpect(jsonPath("$.name").value("Lead Sheet"))

            // Step 8: Add comment to song
            val commentRequest = mapOf("content" to "Great arrangement for Sunday!")

            mockMvc.perform(
                post("/api/v1/songs/$songId/comments")
                    .with(adminAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest))
            )
                .expectCreated()
                .andExpect(jsonPath("$.commentId").exists())

            // Step 9: Search for the song
            mockMvc.perform(
                get("/api/v1/songs/search")
                    .with(adminAuth)
                    .param("query", "Catalog Flow")
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)

            // Step 10: Filter by category
            mockMvc.perform(
                get("/api/v1/songs/filter")
                    .with(adminAuth)
                    .param("categoryId", categoryId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].title").value("Catalog Flow Song"))

            // Step 11: Filter by tag
            mockMvc.perform(
                get("/api/v1/songs/filter")
                    .with(adminAuth)
                    .param("tagIds", tagId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].title").value("Catalog Flow Song"))
        }
    }
}
