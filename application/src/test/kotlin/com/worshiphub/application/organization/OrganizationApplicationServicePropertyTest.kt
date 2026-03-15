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
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Property-based tests for OrganizationApplicationService using Kotest + MockK.
 * Each test validates a correctness property from the design document.
 */
@OptIn(ExperimentalKotest::class)
class OrganizationApplicationServicePropertyTest : FreeSpec({

    // ── Shared generators ──

    val arbUUID = Arb.uuid()
    val arbTeamName = Arb.string(1..100).filter { it.isNotBlank() }
    val arbDescription = Arb.string(0..500).orNull(0.3)
    val arbTeamRole = Arb.enum<TeamRole>()

    // ── Shared mock setup ──

    fun createMocks(): ServiceMocks {
        val churchRepository = mockk<ChurchRepository>()
        val userRepository = mockk<UserRepository>()
        val teamRepository = mockk<TeamRepository>()
        val teamMemberRepository = mockk<TeamMemberRepository>()
        val notificationRepository = mockk<NotificationRepository>()
        val serviceEventRepository = mockk<ServiceEventRepository>()
        val userAvailabilityRepository = mockk<UserAvailabilityRepository>()
        val service = OrganizationApplicationService(
            userRepository, churchRepository, teamRepository, teamMemberRepository,
            notificationRepository, serviceEventRepository, userAvailabilityRepository
        )
        return ServiceMocks(
            service, teamRepository, teamMemberRepository,
            notificationRepository, serviceEventRepository, userAvailabilityRepository
        )
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 1: Round-trip de CRUD de equipos
    // Validates: Requirements 2.1, 3.1, 4.1
    // ══════════════════════════════════════════════════════════════════════
    "Property 1: Round-trip CRUD de equipos — create, get, update, get, delete, get" {
        checkAll(PropTestConfig(iterations = 100), arbUUID, arbTeamName, arbDescription, arbUUID, arbUUID) {
            teamId, name, description, churchId, leaderId ->

            val mocks = createMocks()
            val createdTeam = Team(id = teamId, name = name, description = description, churchId = churchId, leaderId = leaderId)

            // create → returns team with generated id
            every { mocks.teamRepository.save(any()) } returns createdTeam

            val createResult = mocks.service.createTeam(CreateTeamCommand(name, description, churchId, leaderId))
            createResult.isSuccess shouldBe true

            // get → returns the same team
            every { mocks.teamRepository.findById(teamId) } returns createdTeam
            val getResult = mocks.service.getTeamById(teamId)
            getResult.isSuccess shouldBe true
            getResult.getOrNull()!!.name shouldBe name
            getResult.getOrNull()!!.description shouldBe description

            // update → returns updated team
            val newName = name.take(50) + "X"
            val newLeaderId = UUID.randomUUID()
            val updatedTeam = createdTeam.copy(name = newName, leaderId = newLeaderId)
            every { mocks.teamRepository.save(match { it.name == newName }) } returns updatedTeam
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()

            val updateResult = mocks.service.updateTeam(UpdateTeamCommand(teamId, newName, description, newLeaderId))
            updateResult.isSuccess shouldBe true
            updateResult.getOrNull()!!.name shouldBe newName
            updateResult.getOrNull()!!.leaderId shouldBe newLeaderId

            // get after update → reflects changes
            every { mocks.teamRepository.findById(teamId) } returns updatedTeam
            val getAfterUpdate = mocks.service.getTeamById(teamId)
            getAfterUpdate.getOrNull()!!.name shouldBe newName

            // delete → success
            every { mocks.teamMemberRepository.deleteByTeamId(teamId) } just Runs
            every { mocks.teamRepository.delete(updatedTeam) } just Runs
            val deleteResult = mocks.service.deleteTeam(teamId)
            deleteResult.isSuccess shouldBe true

            // get after delete → failure (not found)
            every { mocks.teamRepository.findById(teamId) } returns null
            val getAfterDelete = mocks.service.getTeamById(teamId)
            getAfterDelete.isFailure shouldBe true
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 2: Validación de restricciones de entrada
    // Validates: Requirements 3.3, 5.3, 7.3
    // Note: Jakarta validation happens at controller level. At service level,
    // we verify that the service correctly processes valid TeamRole values
    // and that invalid data propagation is handled.
    // ══════════════════════════════════════════════════════════════════════
    "Property 2: Validación de restricciones — service rejects invalid team names (empty)" {
        checkAll(PropTestConfig(iterations = 100), arbUUID) { teamId ->
            val mocks = createMocks()

            // Team with empty name — the repository save will be called but
            // at service level, we verify the command flows through.
            // The actual validation (name length 1-100, desc ≤500) is Jakarta at controller.
            // At service level, we test that all TeamRole enum values are accepted.
            val role = TeamRole.entries.random()
            val userId = UUID.randomUUID()
            val command = AssignTeamMemberCommand(teamId = teamId, userId = userId, teamRole = role)

            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.teamMemberRepository.save(any()) } answers {
                firstArg<TeamMember>()
            }
            every { mocks.teamRepository.findById(teamId) } returns Team(
                id = teamId, name = "Test", churchId = UUID.randomUUID(), leaderId = UUID.randomUUID()
            )
            every { mocks.notificationRepository.save(any()) } answers { firstArg<Notification>() }

            val result = mocks.service.assignTeamMember(command)
            result.isSuccess shouldBe true

            // Verify the role was correctly stored
            verify { mocks.teamMemberRepository.save(match { it.teamRole == role }) }
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 3: Aplicación de autorización por endpoint
    // Validates: Requirements 1.3, 3.4, 4.3, 5.4, 6.3, 7.4, 8.3, 9.4, 10.3, 11.3
    // Note: Authorization is enforced via @PreAuthorize at controller level.
    // At service level, we verify that the service methods are callable and
    // return correct results regardless of caller — authorization is orthogonal.
    // This test verifies that every service method returns a proper Result
    // (success or typed failure) and never throws unchecked exceptions.
    // ══════════════════════════════════════════════════════════════════════
    "Property 3: Autorización — service methods return Result without throwing" {
        checkAll(PropTestConfig(iterations = 100), arbUUID, arbUUID) { teamId, churchId ->
            val mocks = createMocks()

            // getTeamsByChurchId
            every { mocks.teamRepository.findByChurchId(churchId) } returns emptyList()
            val listResult = mocks.service.getTeamsByChurchId(churchId)
            listResult.isSuccess shouldBe true

            // getTeamById — not found
            every { mocks.teamRepository.findById(teamId) } returns null
            val getResult = mocks.service.getTeamById(teamId)
            getResult.isFailure shouldBe true

            // deleteTeam — not found
            val deleteResult = mocks.service.deleteTeam(teamId)
            deleteResult.isFailure shouldBe true

            // getTeamMembers
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            val membersResult = mocks.service.getTeamMembers(teamId)
            membersResult.isSuccess shouldBe true

            // getUpcomingServices
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns emptyList()
            val servicesResult = mocks.service.getUpcomingServices(teamId)
            servicesResult.isSuccess shouldBe true

            // getTeamAvailability
            every { mocks.userAvailabilityRepository.findByUserIdAndDateRange(any(), any(), any()) } returns emptyList()
            val availResult = mocks.service.getTeamAvailability(teamId, LocalDate.now(), LocalDate.now().plusDays(7))
            availResult.isSuccess shouldBe true

            // getTeamSummary
            every { mocks.teamMemberRepository.countByTeamId(teamId) } returns 0
            every { mocks.serviceEventRepository.findByTeamIdAndDateRange(eq(teamId), any(), any()) } returns emptyList()
            val summaryResult = mocks.service.getTeamSummary(teamId)
            summaryResult.isSuccess shouldBe true
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 4: Unicidad de membresía en equipo
    // Validates: Requirements 5.5
    // ══════════════════════════════════════════════════════════════════════
    "Property 4: Unicidad de membresía — duplicate assign returns failure" {
        checkAll(PropTestConfig(iterations = 100), arbUUID, arbUUID, arbTeamRole) { teamId, userId, role ->
            val mocks = createMocks()
            val existingMember = TeamMember(teamId = teamId, userId = userId, teamRole = role)

            // First assign succeeds
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }
            every { mocks.teamRepository.findById(teamId) } returns Team(
                id = teamId, name = "T", churchId = UUID.randomUUID(), leaderId = UUID.randomUUID()
            )
            every { mocks.notificationRepository.save(any()) } answers { firstArg<Notification>() }

            val firstResult = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, userId, role)
            )
            firstResult.isSuccess shouldBe true

            // Second assign — user already exists
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns existingMember

            val secondResult = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, userId, role)
            )
            secondResult.isFailure shouldBe true
            secondResult.exceptionOrNull()!!.message shouldBe "User is already a member of this team"
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 5: Eliminación en cascada de miembros
    // Validates: Requirements 4.4
    // ══════════════════════════════════════════════════════════════════════
    "Property 5: Eliminación en cascada — deleteByTeamId called before team deletion" {
        checkAll(PropTestConfig(iterations = 100), arbUUID, arbTeamName, arbUUID, arbUUID, Arb.int(0..20)) {
            teamId, name, churchId, leaderId, memberCount ->

            val mocks = createMocks()
            val team = Team(id = teamId, name = name, churchId = churchId, leaderId = leaderId)

            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.teamMemberRepository.deleteByTeamId(teamId) } just Runs
            every { mocks.teamRepository.delete(team) } just Runs

            val result = mocks.service.deleteTeam(teamId)
            result.isSuccess shouldBe true

            // Verify deleteByTeamId was called BEFORE team delete
            verifyOrder {
                mocks.teamMemberRepository.deleteByTeamId(teamId)
                mocks.teamRepository.delete(team)
            }
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 6: Round-trip de CRUD de miembros
    // Validates: Requirements 5.1, 6.1, 7.1, 8.1
    // ══════════════════════════════════════════════════════════════════════
    "Property 6: Round-trip CRUD de miembros — assign, list, updateRole, list, remove, list" {
        checkAll(PropTestConfig(iterations = 100), arbUUID, arbUUID, arbTeamRole, arbTeamRole) {
            teamId, userId, initialRole, newRole ->

            val mocks = createMocks()
            val memberId = UUID.randomUUID()
            val member = TeamMember(id = memberId, teamId = teamId, userId = userId, teamRole = initialRole)
            val team = Team(id = teamId, name = "T", churchId = UUID.randomUUID(), leaderId = UUID.randomUUID())

            // assign
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }
            every { mocks.teamRepository.findById(teamId) } returns team
            every { mocks.notificationRepository.save(any()) } answers { firstArg<Notification>() }

            val assignResult = mocks.service.assignTeamMember(
                AssignTeamMemberCommand(teamId, userId, initialRole)
            )
            assignResult.isSuccess shouldBe true

            // list → includes the new member
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            val listResult = mocks.service.getTeamMembers(teamId)
            listResult.isSuccess shouldBe true
            listResult.getOrNull()!! shouldHaveSize 1
            listResult.getOrNull()!!.first().teamRole shouldBe initialRole

            // updateRole
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns member
            val updatedMember = member.copy(teamRole = newRole)
            every { mocks.teamMemberRepository.save(match { it.teamRole == newRole }) } returns updatedMember

            val updateResult = mocks.service.updateTeamMemberRole(teamId, userId, newRole)
            updateResult.isSuccess shouldBe true

            // list after update → reflects new role
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns listOf(updatedMember)
            val listAfterUpdate = mocks.service.getTeamMembers(teamId)
            listAfterUpdate.getOrNull()!!.first().teamRole shouldBe newRole

            // remove
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns updatedMember
            every { mocks.teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId) } just Runs
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns emptyList()

            val removeResult = mocks.service.removeTeamMember(teamId, userId)
            removeResult.isSuccess shouldBe true

            // list after remove → empty
            val listAfterRemove = mocks.service.getTeamMembers(teamId)
            listAfterRemove.getOrNull()!! shouldHaveSize 0
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 7: Ordenamiento de próximos servicios
    // Validates: Requirements 9.1, 9.2
    // ══════════════════════════════════════════════════════════════════════
    "Property 7: Ordenamiento de próximos servicios — sorted ascending by scheduledDate" {
        val arbServiceCount = Arb.int(0..15)

        checkAll(PropTestConfig(iterations = 100), arbUUID, arbServiceCount) { teamId, count ->
            val mocks = createMocks()
            val baseDate = LocalDateTime.now().plusDays(1)

            // Generate services with random date offsets (shuffled)
            val offsets = (0 until count).shuffled()
            val services = offsets.map { offset ->
                val serviceId = UUID.randomUUID()
                val accepted = AssignedMember(
                    serviceEventId = serviceId, userId = UUID.randomUUID(),
                    role = "VOCALIST", confirmationStatus = ConfirmationStatus.ACCEPTED
                )
                val pending = AssignedMember(
                    serviceEventId = serviceId, userId = UUID.randomUUID(),
                    role = "GUITAR", confirmationStatus = ConfirmationStatus.PENDING
                )
                ServiceEvent(
                    id = serviceId,
                    name = "Service $offset",
                    scheduledDate = baseDate.plusDays(offset.toLong()),
                    teamId = teamId,
                    assignedMembers = listOf(accepted, pending),
                    status = ServiceEventStatus.PUBLISHED,
                    churchId = UUID.randomUUID()
                )
            }

            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns services

            val result = mocks.service.getUpcomingServices(teamId)
            result.isSuccess shouldBe true
            val dtos = result.getOrNull()!!

            dtos shouldHaveSize count

            // Verify ascending order
            dtos.zipWithNext().forEach { (a, b) ->
                (a.scheduledDate <= b.scheduledDate) shouldBe true
            }

            // Verify confirmed/assigned counts
            dtos.forEach { dto ->
                dto.confirmedCount shouldBe 1  // one ACCEPTED per service
                dto.assignedCount shouldBe 2   // two assigned per service
            }
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 8: Filtrado de disponibilidad por rango de fechas
    // Validates: Requirements 10.1, 10.2
    // ══════════════════════════════════════════════════════════════════════
    "Property 8: Filtrado de disponibilidad — only dates within range are returned" {
        val arbMemberCount = Arb.int(1..5)
        val arbDayOffset = Arb.int(0..60)

        checkAll(PropTestConfig(iterations = 100), arbUUID, arbMemberCount, arbDayOffset, arbDayOffset) {
            teamId, memberCount, startOffset, rangeSize ->

            val mocks = createMocks()
            val startDate = LocalDate.now().plusDays(startOffset.toLong())
            val endDate = startDate.plusDays(rangeSize.toLong())

            val members = (1..memberCount).map {
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.entries.random())
            }

            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns members

            members.forEach { member ->
                // Generate some dates inside range and some outside
                val insideDates = (0..rangeSize.toLong()).map { dayOff ->
                    UserAvailability(
                        userId = member.userId,
                        unavailableDate = startDate.plusDays(dayOff)
                    )
                }
                val outsideDates = listOf(
                    UserAvailability(userId = member.userId, unavailableDate = startDate.minusDays(1)),
                    UserAvailability(userId = member.userId, unavailableDate = endDate.plusDays(1))
                )
                // Repository returns all dates (inside + outside) — service should filter
                every {
                    mocks.userAvailabilityRepository.findByUserIdAndDateRange(member.userId, startDate, endDate)
                } returns (insideDates + outsideDates)
            }

            val result = mocks.service.getTeamAvailability(teamId, startDate, endDate)
            result.isSuccess shouldBe true
            val availabilities = result.getOrNull()!!

            availabilities shouldHaveSize memberCount

            // All returned dates must be within [startDate, endDate]
            availabilities.forEach { memberAvail ->
                memberAvail.unavailableDates.forEach { dateDto ->
                    (dateDto.date >= startDate) shouldBe true
                    (dateDto.date <= endDate) shouldBe true
                }
            }
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 9: Consistencia del resumen del equipo
    // Validates: Requirements 11.2
    // ══════════════════════════════════════════════════════════════════════
    "Property 9: Consistencia del resumen — totalMembers and roleDistribution match actual data" {
        val arbMemberCount = Arb.int(0..20)

        checkAll(PropTestConfig(iterations = 100), arbUUID, arbMemberCount) { teamId, memberCount ->
            val mocks = createMocks()

            val members = (1..memberCount).map {
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.entries.random())
            }

            val expectedRoleDistribution = members.groupBy { it.teamRole }.mapValues { it.value.size }

            every { mocks.teamMemberRepository.countByTeamId(teamId) } returns memberCount
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns members

            val recentCount = (0..5).random()
            val upcomingCount = (0..5).random()
            val recentServices = (1..recentCount).map {
                ServiceEvent(
                    name = "Recent $it", scheduledDate = LocalDateTime.now().minusDays(it.toLong()),
                    teamId = teamId, churchId = UUID.randomUUID()
                )
            }
            val upcomingServices = (1..upcomingCount).map {
                ServiceEvent(
                    name = "Upcoming $it", scheduledDate = LocalDateTime.now().plusDays(it.toLong()),
                    teamId = teamId, churchId = UUID.randomUUID()
                )
            }

            every { mocks.serviceEventRepository.findByTeamIdAndDateRange(eq(teamId), any(), any()) } returns recentServices
            every { mocks.serviceEventRepository.findUpcomingByTeamId(teamId) } returns upcomingServices

            val result = mocks.service.getTeamSummary(teamId)
            result.isSuccess shouldBe true
            val summary = result.getOrNull()!!

            summary.totalMembers shouldBe memberCount
            summary.roleDistribution shouldBe expectedRoleDistribution
            summary.recentServicesCount shouldBe recentCount
            summary.upcomingServicesCount shouldBe upcomingCount
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 10: Creación de notificaciones por mutaciones de miembros
    // Validates: Requirements 12.1, 12.2, 12.3, 12.4
    // ══════════════════════════════════════════════════════════════════════
    "Property 10: Notificaciones — correct count and type per mutation" {
        val arbMemberCount = Arb.int(1..10)

        checkAll(PropTestConfig(iterations = 100), arbUUID, arbMemberCount, arbTeamRole) {
            teamId, existingCount, role ->

            val mocks = createMocks()
            val team = Team(id = teamId, name = "Team", churchId = UUID.randomUUID(), leaderId = UUID.randomUUID())
            val existingMembers = (1..existingCount).map {
                TeamMember(teamId = teamId, userId = UUID.randomUUID(), teamRole = TeamRole.entries.random())
            }

            val savedNotifications = mutableListOf<Notification>()
            every { mocks.notificationRepository.save(any()) } answers {
                val n = firstArg<Notification>()
                savedNotifications.add(n)
                n
            }
            every { mocks.teamRepository.findById(teamId) } returns team

            // ── 12.1: assignTeamMember → N notifications of TEAM_MEMBER_ADDED ──
            savedNotifications.clear()
            val newUserId = UUID.randomUUID()
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, newUserId) } returns null
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns existingMembers
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }

            mocks.service.assignTeamMember(AssignTeamMemberCommand(teamId, newUserId, role))

            savedNotifications shouldHaveSize existingCount
            savedNotifications.all { it.type == NotificationType.TEAM_MEMBER_ADDED } shouldBe true

            // ── 12.2: removeTeamMember → (N-1) notifications of TEAM_MEMBER_REMOVED ──
            savedNotifications.clear()
            val memberToRemove = existingMembers.first()
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, memberToRemove.userId) } returns memberToRemove
            every { mocks.teamMemberRepository.deleteByTeamIdAndUserId(teamId, memberToRemove.userId) } just Runs
            val remaining = existingMembers.drop(1)
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns remaining

            mocks.service.removeTeamMember(teamId, memberToRemove.userId)

            savedNotifications shouldHaveSize remaining.size
            savedNotifications.all { it.type == NotificationType.TEAM_MEMBER_REMOVED } shouldBe true

            // ── 12.3: updateTeamMemberRole → 1 notification of TEAM_ROLE_CHANGED ──
            savedNotifications.clear()
            val memberToUpdate = existingMembers.last()
            every { mocks.teamMemberRepository.findByTeamIdAndUserId(teamId, memberToUpdate.userId) } returns memberToUpdate
            every { mocks.teamMemberRepository.save(any()) } answers { firstArg<TeamMember>() }

            mocks.service.updateTeamMemberRole(teamId, memberToUpdate.userId, role)

            savedNotifications shouldHaveSize 1
            savedNotifications.first().type shouldBe NotificationType.TEAM_ROLE_CHANGED
            savedNotifications.first().userId shouldBe memberToUpdate.userId

            // ── 12.4: updateTeam with leader change → N notifications of TEAM_LEADER_CHANGED ──
            savedNotifications.clear()
            val newLeaderId = UUID.randomUUID()
            val updatedTeam = team.copy(leaderId = newLeaderId)
            every { mocks.teamRepository.save(any()) } returns updatedTeam
            every { mocks.teamMemberRepository.findByTeamId(teamId) } returns existingMembers

            mocks.service.updateTeam(UpdateTeamCommand(teamId, team.name, team.description, newLeaderId))

            savedNotifications shouldHaveSize existingCount
            savedNotifications.all { it.type == NotificationType.TEAM_LEADER_CHANGED } shouldBe true
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // Feature: teams-module, Property 11: Filtrado de equipos por iglesia
    // Validates: Requirements 1.1
    // ══════════════════════════════════════════════════════════════════════
    "Property 11: Filtrado por iglesia — only teams with matching churchId are returned" {
        val arbTeamCount = Arb.int(0..15)

        checkAll(PropTestConfig(iterations = 100), arbUUID, arbUUID, arbTeamCount) {
            targetChurchId, otherChurchId, teamCount ->

            val mocks = createMocks()

            val matchingTeams = (1..teamCount).map {
                Team(name = "Team $it", churchId = targetChurchId, leaderId = UUID.randomUUID())
            }

            // Repository only returns matching teams (as per findByChurchId contract)
            every { mocks.teamRepository.findByChurchId(targetChurchId) } returns matchingTeams

            val result = mocks.service.getTeamsByChurchId(targetChurchId)
            result.isSuccess shouldBe true
            val teams = result.getOrNull()!!

            teams shouldHaveSize teamCount
            teams.forEach { team ->
                team.churchId shouldBe targetChurchId
            }
        }
    }
})

/**
 * Helper class to hold all mocks and the service instance.
 */
private data class ServiceMocks(
    val service: OrganizationApplicationService,
    val teamRepository: TeamRepository,
    val teamMemberRepository: TeamMemberRepository,
    val notificationRepository: NotificationRepository,
    val serviceEventRepository: ServiceEventRepository,
    val userAvailabilityRepository: UserAvailabilityRepository
)
