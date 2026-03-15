package com.worshiphub.api.organization

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.worshiphub.application.organization.MemberAvailabilityDTO
import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.TeamSummaryDTO
import com.worshiphub.application.organization.UnavailableDateDTO
import com.worshiphub.application.organization.UpcomingServiceDTO
import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.TeamMember
import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.scheduling.ServiceEventStatus
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for TeamController endpoints using MockMvc + SpringMockK.
 * Security filters are disabled to focus on controller logic.
 *
 * Validates: Requirements 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1, 11.1
 */
@WebMvcTest(TeamController::class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var organizationApplicationService: OrganizationApplicationService

    private val teamId: UUID = UUID.randomUUID()
    private val churchId: UUID = UUID.randomUUID()
    private val leaderId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()
    private val now: LocalDateTime = LocalDateTime.of(2025, 1, 15, 10, 0, 0)

    private fun sampleTeam() = Team(
        id = teamId,
        name = "Worship Team",
        description = "Main worship team",
        churchId = churchId,
        leaderId = leaderId,
        createdAt = now
    )

    // ── GET /api/v1/teams — Validates: Requirement 1.1 ──

    @Test
    fun `GET teams - should return list of teams for church`() {
        val teams = listOf(sampleTeam())
        every { organizationApplicationService.getTeamsByChurchId(churchId) } returns Result.success(teams)

        mockMvc.perform(
            get("/api/v1/teams")
                .header("Church-Id", churchId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(teamId.toString()))
            .andExpect(jsonPath("$[0].name").value("Worship Team"))
            .andExpect(jsonPath("$[0].churchId").value(churchId.toString()))
    }

    @Test
    fun `GET teams - should return empty list when no teams exist`() {
        every { organizationApplicationService.getTeamsByChurchId(churchId) } returns Result.success(emptyList())

        mockMvc.perform(
            get("/api/v1/teams")
                .header("Church-Id", churchId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    // ── GET /api/v1/teams/{teamId} — Validates: Requirement 2.1 ──

    @Test
    fun `GET team by id - should return team details`() {
        every { organizationApplicationService.getTeamById(teamId) } returns Result.success(sampleTeam())

        mockMvc.perform(get("/api/v1/teams/$teamId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(teamId.toString()))
            .andExpect(jsonPath("$.name").value("Worship Team"))
            .andExpect(jsonPath("$.description").value("Main worship team"))
            .andExpect(jsonPath("$.leaderId").value(leaderId.toString()))
    }

    @Test
    fun `GET team by id - should return 404 when team not found`() {
        every { organizationApplicationService.getTeamById(teamId) } returns
            Result.failure(RuntimeException("Team not found: $teamId"))

        mockMvc.perform(get("/api/v1/teams/$teamId"))
            .andExpect(status().isNotFound)
    }

    // ── PUT /api/v1/teams/{teamId} — Validates: Requirement 3.1 ──

    @Test
    fun `PUT team - should update team successfully`() {
        val updatedTeam = sampleTeam().copy(name = "Updated Team", description = "Updated desc")
        every { organizationApplicationService.updateTeam(any()) } returns Result.success(updatedTeam)

        val request = UpdateTeamRequest(
            name = "Updated Team",
            description = "Updated desc",
            leaderId = leaderId
        )

        mockMvc.perform(
            put("/api/v1/teams/$teamId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Team"))
            .andExpect(jsonPath("$.description").value("Updated desc"))
    }

    @Test
    fun `PUT team - should return 404 when team not found`() {
        every { organizationApplicationService.updateTeam(any()) } returns
            Result.failure(RuntimeException("Team not found: $teamId"))

        val request = UpdateTeamRequest(name = "Updated", description = null, leaderId = leaderId)

        mockMvc.perform(
            put("/api/v1/teams/$teamId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT team - should return 400 when name is blank`() {
        val request = mapOf(
            "name" to "",
            "description" to "desc",
            "leaderId" to leaderId.toString()
        )

        mockMvc.perform(
            put("/api/v1/teams/$teamId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // ── DELETE /api/v1/teams/{teamId} — Validates: Requirement 4.1 ──

    @Test
    fun `DELETE team - should delete team and return 204`() {
        every { organizationApplicationService.deleteTeam(teamId) } returns Result.success(Unit)

        mockMvc.perform(delete("/api/v1/teams/$teamId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE team - should return 404 when team not found`() {
        every { organizationApplicationService.deleteTeam(teamId) } returns
            Result.failure(RuntimeException("Team not found: $teamId"))

        mockMvc.perform(delete("/api/v1/teams/$teamId"))
            .andExpect(status().isNotFound)
    }

    // ── POST /api/v1/teams/{teamId}/members — Validates: Requirement 5.1 ──

    @Test
    fun `POST assign member - should assign member and return 201`() {
        val memberId = UUID.randomUUID()
        every { organizationApplicationService.assignTeamMember(any()) } returns Result.success(memberId)

        val request = AssignTeamMemberRequest(userId = userId, teamRole = TeamRole.LEAD_VOCALIST)

        mockMvc.perform(
            post("/api/v1/teams/$teamId/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.memberId").value(memberId.toString()))
    }

    @Test
    fun `POST assign member - should return 409 when member already exists`() {
        every { organizationApplicationService.assignTeamMember(any()) } returns
            Result.failure(RuntimeException("User is already a member of this team"))

        val request = AssignTeamMemberRequest(userId = userId, teamRole = TeamRole.DRUMS)

        mockMvc.perform(
            post("/api/v1/teams/$teamId/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST assign member - should return 404 when team not found`() {
        every { organizationApplicationService.assignTeamMember(any()) } returns
            Result.failure(RuntimeException("Team not found: $teamId"))

        val request = AssignTeamMemberRequest(userId = userId, teamRole = TeamRole.KEYBOARD)

        mockMvc.perform(
            post("/api/v1/teams/$teamId/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // ── GET /api/v1/teams/{teamId}/members — Validates: Requirement 6.1 ──

    @Test
    fun `GET members - should return list of team members`() {
        val members = listOf(
            TeamMember(teamId = teamId, userId = userId, teamRole = TeamRole.LEAD_VOCALIST, joinedAt = now)
        )
        every { organizationApplicationService.getTeamMembers(teamId) } returns Result.success(members)

        mockMvc.perform(get("/api/v1/teams/$teamId/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$[0].teamRole").value("LEAD_VOCALIST"))
    }

    @Test
    fun `GET members - should return empty list when no members`() {
        every { organizationApplicationService.getTeamMembers(teamId) } returns Result.success(emptyList())

        mockMvc.perform(get("/api/v1/teams/$teamId/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    // ── PUT /api/v1/teams/{teamId}/members/{userId}/role — Validates: Requirement 7.1 ──

    @Test
    fun `PUT member role - should update role successfully`() {
        every { organizationApplicationService.updateTeamMemberRole(teamId, userId, TeamRole.DRUMS) } returns
            Result.success(Unit)

        val request = UpdateMemberRoleRequest(teamRole = TeamRole.DRUMS)

        mockMvc.perform(
            put("/api/v1/teams/$teamId/members/$userId/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT member role - should return 404 when member not found`() {
        every { organizationApplicationService.updateTeamMemberRole(teamId, userId, TeamRole.DRUMS) } returns
            Result.failure(RuntimeException("Team member not found"))

        val request = UpdateMemberRoleRequest(teamRole = TeamRole.DRUMS)

        mockMvc.perform(
            put("/api/v1/teams/$teamId/members/$userId/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // ── DELETE /api/v1/teams/{teamId}/members/{userId} — Validates: Requirement 8.1 ──

    @Test
    fun `DELETE member - should remove member and return 204`() {
        every { organizationApplicationService.removeTeamMember(teamId, userId) } returns Result.success(Unit)

        mockMvc.perform(delete("/api/v1/teams/$teamId/members/$userId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE member - should return 404 when member not found`() {
        every { organizationApplicationService.removeTeamMember(teamId, userId) } returns
            Result.failure(RuntimeException("Team member not found"))

        mockMvc.perform(delete("/api/v1/teams/$teamId/members/$userId"))
            .andExpect(status().isNotFound)
    }

    // ── GET /api/v1/teams/{teamId}/upcoming-services — Validates: Requirement 9.1 ──

    @Test
    fun `GET upcoming services - should return list of services`() {
        val services = listOf(
            UpcomingServiceDTO(
                id = UUID.randomUUID(),
                name = "Sunday Service",
                scheduledDate = now.plusDays(7),
                status = ServiceEventStatus.PUBLISHED,
                confirmedCount = 5,
                assignedCount = 8
            )
        )
        every { organizationApplicationService.getUpcomingServices(teamId) } returns Result.success(services)

        mockMvc.perform(get("/api/v1/teams/$teamId/upcoming-services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Sunday Service"))
            .andExpect(jsonPath("$[0].confirmedCount").value(5))
            .andExpect(jsonPath("$[0].assignedCount").value(8))
    }

    @Test
    fun `GET upcoming services - should return empty list when none`() {
        every { organizationApplicationService.getUpcomingServices(teamId) } returns Result.success(emptyList())

        mockMvc.perform(get("/api/v1/teams/$teamId/upcoming-services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    // ── GET /api/v1/teams/{teamId}/availability — Validates: Requirement 10.1 ──

    @Test
    fun `GET availability - should return member availability`() {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val availability = listOf(
            MemberAvailabilityDTO(
                userId = userId,
                teamRole = TeamRole.LEAD_VOCALIST,
                unavailableDates = listOf(
                    UnavailableDateDTO(date = LocalDate.of(2025, 1, 10), reason = "Vacation")
                )
            )
        )
        every {
            organizationApplicationService.getTeamAvailability(teamId, startDate, endDate)
        } returns Result.success(availability)

        mockMvc.perform(
            get("/api/v1/teams/$teamId/availability")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-01-31")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$[0].teamRole").value("LEAD_VOCALIST"))
            .andExpect(jsonPath("$[0].unavailableDates[0].date").value("2025-01-10"))
            .andExpect(jsonPath("$[0].unavailableDates[0].reason").value("Vacation"))
    }

    // ── GET /api/v1/teams/{teamId}/summary — Validates: Requirement 11.1 ──

    @Test
    fun `GET summary - should return team summary`() {
        val summary = TeamSummaryDTO(
            totalMembers = 8,
            recentServicesCount = 4,
            upcomingServicesCount = 3,
            roleDistribution = mapOf(
                TeamRole.LEAD_VOCALIST to 1,
                TeamRole.DRUMS to 2,
                TeamRole.KEYBOARD to 1
            )
        )
        every { organizationApplicationService.getTeamSummary(teamId) } returns Result.success(summary)

        mockMvc.perform(get("/api/v1/teams/$teamId/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMembers").value(8))
            .andExpect(jsonPath("$.recentServicesCount").value(4))
            .andExpect(jsonPath("$.upcomingServicesCount").value(3))
            .andExpect(jsonPath("$.roleDistribution.LEAD_VOCALIST").value(1))
            .andExpect(jsonPath("$.roleDistribution.DRUMS").value(2))
    }
}
