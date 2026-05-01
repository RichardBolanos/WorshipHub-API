package com.worshiphub.application.organization

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationRepository
import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.TeamMember
import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.TeamMemberRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.AssignedMember
import com.worshiphub.domain.scheduling.ConfirmationStatus
import com.worshiphub.domain.scheduling.ServiceEvent
import com.worshiphub.domain.scheduling.ServiceEventStatus
import com.worshiphub.domain.scheduling.UserAvailability
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for OrganizationApplicationService using Kotest FreeSpec + MockK.
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4 (notifications) and general method behavior.
 */
class OrganizationApplicationServiceTest : FreeSpec({

    // ── Shared mock setup ──

    fun createMocks(): TestMocks {
        val userRepository = mockk<UserRepository>()
        val churchRepository = mockk<ChurchRepository>()
        val teamRepository = mockk<TeamRepository>()
        val teamMemberRepository = mockk<TeamMemberRepository>()
        val notificationRepository = mockk<NotificationRepository>()
        val serviceEventRepository = mockk<ServiceEventRepository>()
        val userAvailabilityRepository = mockk<UserAvailabilityRepository>()
        val service = OrganizationApplicationService(
            userRepository, churchRepository, teamRepository, teamMemberRepository,
            notificationRepository, serviceEventRepository, userAvailabilityRepository
        )
        return TestMocks(
            service, teamRepository, teamMemberRepository,
            notificationRepository, serviceEventRepository, userAvailabilityRepository
        )
    }

    // ── Shared test data ──

    val teamId = UUID.randomUUID()
    val churchId = UUID.randomUUID()
    val leaderId = UUID.randomUUID()
    val team = Team(id = teamId, name = "Worship Team", description = "Main team", churchId = churchId, leaderId = leaderId)


    // ══════════════════════════════════════════════════════════════════════
    // getTeamsByChurchId
    // ══════════════════════════════════════════════════════════════════════
    "getTeamsByChurchId" - {
        "should return teams for a given church" {
            val mocks = createMocks()
            val teams = listOf(
                Team(name = "Team A", churchId = churchId, leaderId = leaderId),
                Team(name = "Team B", churchId = churchId, leaderId = leaderId)
            )
            every { mocks.teamRepository.findByChurchId(churchId) } returns teams

            val result = mocks.service.getTeamsByChurchId(churchId)

            result.isSuccess shouldBe true
            result.getOrNull()!! shouldHaveSize 2
        }

        "should return empty list when no teams exist" {
            val mocks = createMocks()
            every { mocks.teamRepository.findByChurchId(churchId) } returns emptyList()

            val result = mocks.service.getTeamsByChurchId(churchId)

            result.isSuccess shouldBe true
            result.getOrNull()!!.shouldBeEmpty()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // getTeamById
    // ══════════════════════════════════════════════════════════════════════
    "getTeamById" - {
        "should return team when found" {
            val mocks = createMocks()
            every { mocks.teamRepository.findById(teamId) } returns team

            val result = mocks.service.getTeamById(teamId)

            result.isSuccess shouldBe true
            result.getOrNull()!!.name shouldBe "Worship Team"
        }

        "should return failure when team not found" {
            val mocks = createMocks()
            every { mocks.teamRepository.findById(teamId) } returns null

            val result = mocks.service.getTeamById(teamId)

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Team not found: $teamId"
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // updateTeam
    // ══════════════════════════════════════════════════════════════════════
    "updateTeam" - {
        "should return updated team" {
            val mocks = createMocks()
            val updatedTeam = team.copy(name = "New Name")
            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.teamRepository.save(any()) } returns updatedTeam
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()

            val result = mocks.service.updateTeam(UpdateTeamCommand(teamId, "New Name", "desc", leaderId))

            result.isSuccess shouldBe true
            result.getOrNull()!!.name shouldBe "New Name"
        }

        "should return failure when team not found" {
            val mocks = createMocks()
            every { mocks.teamRepository.findById(teamId) } returns null

            val result = mocks.service.updateTeam(UpdateTeamCommand(teamId, "X", null, leaderId))

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Team not found: $teamId"
        }

        // ── Validates: Requirement 12.4 ──
        "should create TEAM_LEADER_CHANGED notifications for all members when leader changes" {
            val mocks = createMocks()
            val newLeaderId = UUID.randomUUID()
            val members = listOf(
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.DRUMS),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.KEYBOARD),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.LEAD_VOCALIST)
            )
            val updatedTeam = team.copy(leaderId = newLeaderId)

            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.teamRepository.save(any()) } returns updatedTeam
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns members

            val savedNotifications = mutableListOf<Notification>()
            every { mocks.notificationRepository.save(any()) } answers {
                val n = firstArg<Notification>()
                savedNotifications.add(n)
                n
            }

            val result = mocks.service.updateTeam(UpdateTeamCommand(teamId, "Worship Team", "Main team", newLeaderId))

            result.isSuccess shouldBe true
            savedNotifications shouldHaveSize 3
            savedNotifications.all { it.type == NotificationType.TEAM_LEADER_CHANGED } shouldBe true
            savedNotifications.map { it.userId }.toSet() shouldBe members.map { it.userId }.toSet()
        }

        "should NOT create notifications when leader does not change" {
            val mocks = createMocks()
            val updatedTeam = team.copy(name = "Renamed")
            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.teamRepository.save(any()) } returns updatedTeam

            val result = mocks.service.updateTeam(UpdateTeamCommand(teamId, "Renamed", null, leaderId))

            result.isSuccess shouldBe true
            verify(exactly = 0) { mocks.notificationRepository.save(any()) }
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // deleteTeam
    // ══════════════════════════════════════════════════════════════════════
    "deleteTeam" - {
        "should delete members first then team" {
            val mocks = createMocks()
            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.teamMemberRepository.deleteByTeamId(teamId) } just Runs
            every { mocks.teamRepository.delete(team) } just Runs

            val result = mocks.service.deleteTeam(teamId)

            result.isSuccess shouldBe true
            verifyOrder {
                mocks.teamMemberRepository.deleteByTeamId(teamId)
                mocks.teamRepository.delete(team)
            }
        }

        "should return failure when team not found" {
            val mocks = createMocks()
            every { mocks.teamRepository.findById(teamId) } returns null

            val result = mocks.service.deleteTeam(teamId)

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Team not found: $teamId"
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // assignTeamMember — Validates: Requirement 12.1
    // ══════════════════════════════════════════════════════════════════════
    "assignTeamMember" - {
        "should create TEAM_MEMBER_ADDED notifications for each existing member" {
            val mocks = createMocks()
            val newUserId = UUID.randomUUID()
            val existingMembers = listOf(
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.DRUMS),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.KEYBOARD)
            )

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, newUserId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns existingMembers
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }
            every { mocks.teamRepository.findById(teamId) } returns team

            val savedNotifications = mutableListOf<Notification>()
            every { mocks.notificationRepository.save(any()) } answers {
                val n = firstArg<Notification>()
                savedNotifications.add(n)
                n
            }

            val result = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, newUserId, TeamRole.BASS_GUITAR)
            )

            result.isSuccess shouldBe true
            savedNotifications shouldHaveSize 2
            savedNotifications.all { it.type == NotificationType.TEAM_MEMBER_ADDED } shouldBe true
            savedNotifications.map { it.userId }.toSet() shouldBe existingMembers.map { it.userId }.toSet()
        }

        "should create zero notifications when team has no existing members" {
            val mocks = createMocks()
            val newUserId = UUID.randomUUID()

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, newUserId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }
            every { mocks.teamRepository.findById(teamId) } returns team

            val result = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, newUserId, TeamRole.ACOUSTIC_GUITAR)
            )

            result.isSuccess shouldBe true
            verify(exactly = 0) { mocks.notificationRepository.save(any()) }
        }

        "should return failure for duplicate member" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            val existing = TeamMember(teamId = teamId, userId = userId, teamRole = TeamRole.DRUMS)

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns existing

            val result = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, userId, TeamRole.DRUMS)
            )

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "User is already a member of this team"
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // removeTeamMember — Validates: Requirement 12.2
    // ══════════════════════════════════════════════════════════════════════
    "removeTeamMember" - {
        "should create TEAM_MEMBER_REMOVED notifications for remaining members" {
            val mocks = createMocks()
            val userToRemove = UUID.randomUUID()
            val memberToRemove = TeamMember(teamId = teamId, userId = userToRemove, teamRole = TeamRole.DRUMS)
            val remainingMembers = listOf(
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.KEYBOARD),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.LEAD_VOCALIST)
            )

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userToRemove) } returns memberToRemove
            every { mocks.teamMemberRepository.deleteByTeamIdAndUserId(teamId, userToRemove) } just Runs
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns remainingMembers
            every { mocks.teamRepository.findById(teamId) } returns team

            val savedNotifications = mutableListOf<Notification>()
            every { mocks.notificationRepository.save(any()) } answers {
                val n = firstArg<Notification>()
                savedNotifications.add(n)
                n
            }

            val result = mocks.service.removeTeamMember(teamId, userToRemove)

            result.isSuccess shouldBe true
            savedNotifications shouldHaveSize 2
            savedNotifications.all { it.type == NotificationType.TEAM_MEMBER_REMOVED } shouldBe true
            savedNotifications.map { it.userId }.toSet() shouldBe remainingMembers.map { it.userId }.toSet()
        }

        "should create zero notifications when no remaining members" {
            val mocks = createMocks()
            val userToRemove = UUID.randomUUID()
            val memberToRemove = TeamMember(teamId = teamId, userId = userToRemove, teamRole = TeamRole.DRUMS)

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userToRemove) } returns memberToRemove
            every { mocks.teamMemberRepository.deleteByTeamIdAndUserId(teamId, userToRemove) } just Runs
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.teamRepository.findById(teamId) } returns team

            val result = mocks.service.removeTeamMember(teamId, userToRemove)

            result.isSuccess shouldBe true
            verify(exactly = 0) { mocks.notificationRepository.save(any()) }
        }

        "should return failure when member not found" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null

            val result = mocks.service.removeTeamMember(teamId, userId)

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Team member not found"
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // updateTeamMemberRole — Validates: Requirement 12.3
    // ══════════════════════════════════════════════════════════════════════
    "updateTeamMemberRole" - {
        "should create exactly 1 TEAM_ROLE_CHANGED notification for the affected member" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            val member = TeamMember(teamId = teamId, userId = userId, teamRole = TeamRole.DRUMS)

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns member
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }
            every { mocks.teamRepository.findById(teamId) } returns team

            val savedNotifications = mutableListOf<Notification>()
            every { mocks.notificationRepository.save(any()) } answers {
                val n = firstArg<Notification>()
                savedNotifications.add(n)
                n
            }

            val result = mocks.service.updateTeamMemberRole(teamId, userId, TeamRole.KEYBOARD)

            result.isSuccess shouldBe true
            savedNotifications shouldHaveSize 1
            savedNotifications.first().type shouldBe NotificationType.TEAM_ROLE_CHANGED
            savedNotifications.first().userId shouldBe userId
        }

        "should return failure when member not found" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null

            val result = mocks.service.updateTeamMemberRole(teamId, userId, TeamRole.KEYBOARD)

            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Team member not found"
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // getUpcomingServices
    // ══════════════════════════════════════════════════════════════════════
    "getUpcomingServices" - {
        "should return services sorted by scheduledDate ascending" {
            val mocks = createMocks()
            val now = LocalDateTime.now()
            val service1 = ServiceEvent(
                name = "Late Service", scheduledDate = now.plusDays(5),
                teamId = teamId, churchId = churchId,
                assignedMembers = listOf(
                    AssignedMember(serviceEventId = UUID.randomUUID(), userId = UUID.randomUUID(), role = "V", confirmationStatus = ConfirmationStatus.ACCEPTED)
                )
            )
            val service2 = ServiceEvent(
                name = "Early Service", scheduledDate = now.plusDays(1),
                teamId = teamId, churchId = churchId,
                assignedMembers = listOf(
                    AssignedMember(serviceEventId = UUID.randomUUID(), userId = UUID.randomUUID(), role = "G", confirmationStatus = ConfirmationStatus.PENDING)
                )
            )
            // Return in wrong order to verify sorting
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns listOf(service1, service2)

            val result = mocks.service.getUpcomingServices(teamId)

            result.isSuccess shouldBe true
            val dtos = result.getOrNull()!!
            dtos shouldHaveSize 2
            dtos[0].name shouldBe "Early Service"
            dtos[1].name shouldBe "Late Service"
            dtos[0].confirmedCount shouldBe 0
            dtos[0].assignedCount shouldBe 1
            dtos[1].confirmedCount shouldBe 1
            dtos[1].assignedCount shouldBe 1
        }

        "should return empty list when no upcoming services" {
            val mocks = createMocks()
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns emptyList()

            val result = mocks.service.getUpcomingServices(teamId)

            result.isSuccess shouldBe true
            result.getOrNull()!!.shouldBeEmpty()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // getTeamAvailability
    // ══════════════════════════════════════════════════════════════════════
    "getTeamAvailability" - {
        "should return filtered availability within date range" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            val member = TeamMember(teamId = teamId, userId = userId, teamRole = TeamRole.DRUMS)
            val startDate = LocalDate.of(2025, 1, 10)
            val endDate = LocalDate.of(2025, 1, 20)

            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns listOf(member)

            val insideDate = UserAvailability(userId = userId, unavailableDate = LocalDate.of(2025, 1, 15), reason = "Vacation")
            val outsideDate = UserAvailability(userId = userId, unavailableDate = LocalDate.of(2025, 1, 5))
            every {
                mocks.userAvailabilityRepository.findByUserIdAndDateRange(userId, startDate, endDate)
            } returns listOf(insideDate, outsideDate)

            val result = mocks.service.getTeamAvailability(teamId, startDate, endDate)

            result.isSuccess shouldBe true
            val availabilities = result.getOrNull()!!
            availabilities shouldHaveSize 1
            availabilities.first().userId shouldBe userId
            availabilities.first().unavailableDates shouldHaveSize 1
            availabilities.first().unavailableDates.first().date shouldBe LocalDate.of(2025, 1, 15)
            availabilities.first().unavailableDates.first().reason shouldBe "Vacation"
        }

        "should return empty unavailable dates when member has no conflicts" {
            val mocks = createMocks()
            val userId = UUID.randomUUID()
            val member = TeamMember(teamId = teamId, userId = userId, teamRole = TeamRole.KEYBOARD)
            val startDate = LocalDate.of(2025, 2, 1)
            val endDate = LocalDate.of(2025, 2, 28)

            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every {
                mocks.userAvailabilityRepository.findByUserIdAndDateRange(userId, startDate, endDate)
            } returns emptyList()

            val result = mocks.service.getTeamAvailability(teamId, startDate, endDate)

            result.isSuccess shouldBe true
            result.getOrNull()!!.first().unavailableDates.shouldBeEmpty()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // getTeamSummary
    // ══════════════════════════════════════════════════════════════════════
    "getTeamSummary" - {
        "should return correct summary data" {
            val mocks = createMocks()
            val members = listOf(
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.DRUMS),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.DRUMS),
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.KEYBOARD)
            )

            every { mocks.teamMemberRepository.countByTeamId(teamId) } returns 3
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns members
            every { mocks.serviceEventRepository.findByTeamIdAndDateRange(eq(teamId), any(), any()) } returns listOf(
                ServiceEvent(name = "Recent", scheduledDate = LocalDateTime.now().minusDays(5), teamId = teamId, churchId = churchId)
            )
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns listOf(
                ServiceEvent(name = "Upcoming 1", scheduledDate = LocalDateTime.now().plusDays(3), teamId = teamId, churchId = churchId),
                ServiceEvent(name = "Upcoming 2", scheduledDate = LocalDateTime.now().plusDays(7), teamId = teamId, churchId = churchId)
            )

            val result = mocks.service.getTeamSummary(teamId)

            result.isSuccess shouldBe true
            val summary = result.getOrNull()!!
            summary.totalMembers shouldBe 3
            summary.recentServicesCount shouldBe 1
            summary.upcomingServicesCount shouldBe 2
            summary.roleDistribution[TeamRole.DRUMS] shouldBe 2
            summary.roleDistribution[TeamRole.KEYBOARD] shouldBe 1
        }

        "should return zeros when team has no members or services" {
            val mocks = createMocks()
            every { mocks.teamMemberRepository.countByTeamId(teamId) } returns 0
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.serviceEventRepository.findByTeamIdAndDateRange(eq(teamId), any(), any()) } returns emptyList()
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns emptyList()

            val result = mocks.service.getTeamSummary(teamId)

            result.isSuccess shouldBe true
            val summary = result.getOrNull()!!
            summary.totalMembers shouldBe 0
            summary.recentServicesCount shouldBe 0
            summary.upcomingServicesCount shouldBe 0
            summary.roleDistribution shouldBe emptyMap()
        }
    }
})

/**
 * Helper class to hold all mocks and the service instance for unit tests.
 */
private data class TestMocks(
    val service: OrganizationApplicationService,
    val teamRepository: TeamRepository,
    val teamMemberRepository: TeamMemberRepository,
    val notificationRepository: NotificationRepository,
    val serviceEventRepository: ServiceEventRepository,
    val userAvailabilityRepository: UserAvailabilityRepository
)
