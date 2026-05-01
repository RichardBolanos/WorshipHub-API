package com.worshiphub.application.scheduling

import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.*
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for SchedulingApplicationService:
 * - createRecurringService
 * - updateRecurrenceRule
 * - deleteRecurringService (with cascade)
 * - deleteAvailability (404, 403, success)
 */
class SchedulingServiceAvailabilityUnitTest {

    private val serviceEventRepository = mockk<ServiceEventRepository>()
    private val setlistRepository = mockk<SetlistRepository>()
    private val userAvailabilityRepository = mockk<UserAvailabilityRepository>()
    private val teamRepository = mockk<TeamRepository>()
    private val userRepository = mockk<UserRepository>()

    private val service = SchedulingApplicationService(
        serviceEventRepository, setlistRepository, userAvailabilityRepository, teamRepository, userRepository
    )

    private val teamId = UUID.randomUUID()
    private val churchId = UUID.randomUUID()
    private val leaderId = UUID.randomUUID()

    private fun sampleTeam() = Team(
        id = teamId,
        name = "Worship Team",
        churchId = churchId,
        leaderId = leaderId
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    // ── createRecurringService ──

    @Nested
    inner class CreateRecurringServiceTests {

        @Test
        fun `should create parent and child instances for WEEKLY recurrence`() {
            val startDate = LocalDateTime.of(2025, 1, 5, 10, 0)
            val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 1, 26))
            val command = CreateRecurringServiceCommand(
                serviceName = "Sunday Service",
                scheduledDate = startDate,
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = rule
            )

            every { teamRepository.findById(teamId) } returns sampleTeam()
            every { serviceEventRepository.save(any()) } answers { firstArg() }

            val result = service.createRecurringService(command)

            assertTrue(result.isSuccess)
            // 1 parent + 3 children (Jan 12, 19, 26)
            verify(exactly = 4) { serviceEventRepository.save(any()) }
        }

        @Test
        fun `should set parentServiceId on child instances`() {
            val startDate = LocalDateTime.of(2025, 1, 5, 10, 0)
            val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 1, 12))
            val command = CreateRecurringServiceCommand(
                serviceName = "Sunday Service",
                scheduledDate = startDate,
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = rule
            )

            val savedEvents = mutableListOf<ServiceEvent>()
            every { teamRepository.findById(teamId) } returns sampleTeam()
            every { serviceEventRepository.save(capture(savedEvents)) } answers { firstArg() }

            service.createRecurringService(command)

            // First saved is parent (no parentServiceId), second is child
            assertNull(savedEvents[0].parentServiceId)
            assertEquals(savedEvents[0].id, savedEvents[1].parentServiceId)
        }

        @Test
        fun `should fail when team not found`() {
            val command = CreateRecurringServiceCommand(
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 5, 10, 0),
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            )

            every { teamRepository.findById(teamId) } returns null

            val result = service.createRecurringService(command)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Team not found") == true)
        }

        @Test
        fun `should reject when end date is before start date`() {
            val command = CreateRecurringServiceCommand(
                serviceName = "Service",
                scheduledDate = LocalDateTime.of(2025, 6, 15, 10, 0),
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 1, 1))
            )

            every { teamRepository.findById(teamId) } returns sampleTeam()

            val result = service.createRecurringService(command)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("posterior") == true)
        }

        @Test
        fun `should store recurrenceRule on parent service`() {
            val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 3, 15))
            val command = CreateRecurringServiceCommand(
                serviceName = "Monthly Service",
                scheduledDate = LocalDateTime.of(2025, 1, 15, 10, 0),
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = rule
            )

            val savedEvents = mutableListOf<ServiceEvent>()
            every { teamRepository.findById(teamId) } returns sampleTeam()
            every { serviceEventRepository.save(capture(savedEvents)) } answers { firstArg() }

            service.createRecurringService(command)

            val parent = savedEvents[0]
            assertEquals(rule, parent.recurrenceRule)
        }
    }

    // ── updateRecurrenceRule ──

    @Nested
    inner class UpdateRecurrenceRuleTests {

        @Test
        fun `should preserve ACCEPTED instances when updating rule`() {
            val parentId = UUID.randomUUID()
            val parentService = ServiceEvent(
                id = parentId,
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 5, 10, 0),
                teamId = teamId,
                churchId = churchId,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 2, 28))
            )

            val acceptedChild = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.now().plusDays(7),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId,
                assignedMembers = listOf(
                    AssignedMember(
                        serviceEventId = UUID.randomUUID(),
                        userId = UUID.randomUUID(),
                        role = "Vocalist",
                        confirmationStatus = ConfirmationStatus.ACCEPTED
                    )
                )
            )

            val draftChild = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.now().plusDays(14),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId
            )

            every { serviceEventRepository.findById(parentId) } returns parentService
            every { serviceEventRepository.findByParentServiceId(parentId) } returns listOf(acceptedChild, draftChild)
            every { serviceEventRepository.save(any()) } answers { firstArg() }
            every { serviceEventRepository.deleteAll(any()) } just Runs

            val newRule = RecurrenceRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2025, 6, 30))
            val result = service.updateRecurrenceRule(parentId, newRule)

            assertTrue(result.isSuccess)
            // Should delete only the draft child, not the accepted one
            verify {
                serviceEventRepository.deleteAll(match { events ->
                    events.size == 1 && events[0].id == draftChild.id
                })
            }
        }

        @Test
        fun `should fail when parent service not found`() {
            val serviceId = UUID.randomUUID()
            every { serviceEventRepository.findById(serviceId) } returns null

            val result = service.updateRecurrenceRule(serviceId, RecurrenceRule(RecurrenceFrequency.WEEKLY))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
        }
    }

    // ── deleteRecurringService ──

    @Nested
    inner class DeleteRecurringServiceTests {

        @Test
        fun `should delete DRAFT and PUBLISHED children without ACCEPTED members`() {
            val parentId = UUID.randomUUID()
            val parentService = ServiceEvent(
                id = parentId,
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 5, 10, 0),
                teamId = teamId,
                churchId = churchId,
                status = ServiceEventStatus.DRAFT,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            )

            val draftChild = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 12, 10, 0),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId,
                status = ServiceEventStatus.DRAFT
            )

            val publishedChild = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 19, 10, 0),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId,
                status = ServiceEventStatus.PUBLISHED
            )

            every { serviceEventRepository.findById(parentId) } returns parentService
            every { serviceEventRepository.findByParentServiceId(parentId) } returns listOf(draftChild, publishedChild)
            every { serviceEventRepository.deleteAll(any()) } just Runs
            every { serviceEventRepository.delete(any()) } just Runs
            every { userAvailabilityRepository.deleteByDateAndTeamMembers(any(), any()) } just Runs

            val result = service.deleteRecurringService(parentId)

            assertTrue(result.isSuccess)
            verify { serviceEventRepository.deleteAll(match { it.size == 2 }) }
            verify { serviceEventRepository.delete(parentService) }
        }

        @Test
        fun `should not delete children with ACCEPTED members`() {
            val parentId = UUID.randomUUID()
            val parentService = ServiceEvent(
                id = parentId,
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 5, 10, 0),
                teamId = teamId,
                churchId = churchId,
                status = ServiceEventStatus.DRAFT,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            )

            val acceptedChild = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 12, 10, 0),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId,
                status = ServiceEventStatus.PUBLISHED,
                assignedMembers = listOf(
                    AssignedMember(
                        serviceEventId = UUID.randomUUID(),
                        userId = UUID.randomUUID(),
                        role = "Vocalist",
                        confirmationStatus = ConfirmationStatus.ACCEPTED
                    )
                )
            )

            every { serviceEventRepository.findById(parentId) } returns parentService
            every { serviceEventRepository.findByParentServiceId(parentId) } returns listOf(acceptedChild)
            every { serviceEventRepository.delete(any()) } just Runs
            every { userAvailabilityRepository.deleteByDateAndTeamMembers(any(), any()) } just Runs

            val result = service.deleteRecurringService(parentId)

            assertTrue(result.isSuccess)
            // Should NOT call deleteAll since the only child has ACCEPTED members
            verify(exactly = 0) { serviceEventRepository.deleteAll(any()) }
        }

        @Test
        fun `should cascade-delete availability records when deleting children`() {
            val parentId = UUID.randomUUID()
            val parentService = ServiceEvent(
                id = parentId,
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 5, 10, 0),
                teamId = teamId,
                churchId = churchId,
                status = ServiceEventStatus.DRAFT,
                recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY)
            )

            val child = ServiceEvent(
                id = UUID.randomUUID(),
                name = "Service",
                scheduledDate = LocalDateTime.of(2025, 1, 12, 10, 0),
                teamId = teamId,
                churchId = churchId,
                parentServiceId = parentId,
                status = ServiceEventStatus.DRAFT
            )

            every { serviceEventRepository.findById(parentId) } returns parentService
            every { serviceEventRepository.findByParentServiceId(parentId) } returns listOf(child)
            every { serviceEventRepository.deleteAll(any()) } just Runs
            every { serviceEventRepository.delete(any()) } just Runs
            every { userAvailabilityRepository.deleteByDateAndTeamMembers(any(), any()) } just Runs

            service.deleteRecurringService(parentId)

            // Should cascade-delete availability for child date
            verify {
                userAvailabilityRepository.deleteByDateAndTeamMembers(
                    LocalDate.of(2025, 1, 12),
                    teamId
                )
            }
            // Should cascade-delete availability for parent date
            verify {
                userAvailabilityRepository.deleteByDateAndTeamMembers(
                    LocalDate.of(2025, 1, 5),
                    teamId
                )
            }
        }

        @Test
        fun `should fail when service not found`() {
            val serviceId = UUID.randomUUID()
            every { serviceEventRepository.findById(serviceId) } returns null

            val result = service.deleteRecurringService(serviceId)

            assertTrue(result.isFailure)
        }
    }

    // ── deleteAvailability ──

    @Nested
    inner class DeleteAvailabilityTests {

        @Test
        fun `should delete availability when correct user`() {
            val availabilityId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val availability = UserAvailability(
                id = availabilityId,
                userId = userId,
                unavailableDate = LocalDate.of(2025, 1, 15),
                reason = "Vacation"
            )

            every { userAvailabilityRepository.findById(availabilityId) } returns availability
            every { userAvailabilityRepository.delete(availability) } just Runs

            val result = service.deleteAvailability(
                DeleteAvailabilityCommand(availabilityId, userId)
            )

            assertTrue(result.isSuccess)
            verify { userAvailabilityRepository.delete(availability) }
        }

        @Test
        fun `should return 404 failure when availability not found`() {
            val availabilityId = UUID.randomUUID()
            every { userAvailabilityRepository.findById(availabilityId) } returns null

            val result = service.deleteAvailability(
                DeleteAvailabilityCommand(availabilityId, UUID.randomUUID())
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NoSuchElementException)
        }

        @Test
        fun `should return 403 failure when wrong user`() {
            val availabilityId = UUID.randomUUID()
            val ownerId = UUID.randomUUID()
            val wrongUserId = UUID.randomUUID()
            val availability = UserAvailability(
                id = availabilityId,
                userId = ownerId,
                unavailableDate = LocalDate.of(2025, 1, 15)
            )

            every { userAvailabilityRepository.findById(availabilityId) } returns availability

            val result = service.deleteAvailability(
                DeleteAvailabilityCommand(availabilityId, wrongUserId)
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
            verify(exactly = 0) { userAvailabilityRepository.delete(any()) }
        }
    }
}
