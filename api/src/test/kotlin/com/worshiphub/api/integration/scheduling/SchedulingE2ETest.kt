package com.worshiphub.api.integration.scheduling

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * E2E integration tests for the Scheduling bounded context.
 * Covers Requirements 14-18: Service lifecycle, recurring services, setlists CRUD,
 * advanced setlist management, and availability.
 */
class SchedulingE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var serviceEventRepository: ServiceEventRepository

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 14: Service Event Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 14 - Service Event Lifecycle")
    inner class ServiceEventLifecycle {

        // Validates: Requirement 14.1
        @Test
        fun `should schedule service with valid data and return 201 with serviceId`() {
            val registration = testData.registerChurch(adminEmail = "svc-create@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create a member to assign
            val member = userRepository.save(
                User(
                    email = "svc-member1@testchurch.com",
                    firstName = "Service",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val request = mapOf(
                "serviceName" to "Sunday Morning Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf(
                        "userId" to member.id.toString(),
                        "role" to "Lead Vocalist"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.serviceId").exists())
        }

        // Validates: Requirement 14.2
        @Test
        fun `should list service events and return 200 with list`() {
            val registration = testData.registerChurch(adminEmail = "svc-list@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create a service event directly via repository to avoid transactional flush issues
            val futureDate = LocalDateTime.now().plusDays(7)
            val serviceEvent = com.worshiphub.domain.scheduling.ServiceEvent(
                name = "Sunday Morning Worship",
                scheduledDate = futureDate,
                teamId = teamId,
                churchId = registration.churchId
            )
            serviceEventRepository.save(serviceEvent)

            // List service events and verify the created event appears
            mockMvc.perform(
                get("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].serviceName").value("Sunday Morning Worship"))
                .andExpect(jsonPath("$[0].scheduledDate").exists())
                .andExpect(jsonPath("$[0].teamId").exists())
                .andExpect(jsonPath("$[0].status").exists())
        }

        // Validates: Requirement 14.3
        @Test
        fun `should accept service invitation and return 200`() {
            val registration = testData.registerChurch(adminEmail = "svc-accept@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "svc-accept-member@testchurch.com",
                    firstName = "Accept",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val scheduleRequest = mapOf(
                "serviceName" to "Sunday Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                )
            )

            // Schedule the service and get serviceId
            val scheduleResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(scheduleRequest))
            )
                .expectCreated()

            val serviceId = scheduleResult.extractUUID("$.serviceId")

            // Find the actual assignment ID from the repository
            val serviceEvent = serviceEventRepository.findById(serviceId)!!
            val assignmentId = serviceEvent.assignedMembers.first { it.userId == member.id }.id

            val responseRequest = mapOf("response" to "ACCEPTED")

            mockMvc.perform(
                patch("/api/v1/services/$serviceId/assignments/$assignmentId")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(responseRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
        }

        // Validates: Requirement 14.4
        @Test
        fun `should decline service invitation and return 200`() {
            val registration = testData.registerChurch(adminEmail = "svc-decline@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "svc-decline-member@testchurch.com",
                    firstName = "Decline",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val scheduleRequest = mapOf(
                "serviceName" to "Sunday Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                )
            )

            val scheduleResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(scheduleRequest))
            )
                .expectCreated()

            val serviceId = scheduleResult.extractUUID("$.serviceId")

            // Find the actual assignment ID from the repository
            val serviceEvent = serviceEventRepository.findById(serviceId)!!
            val assignmentId = serviceEvent.assignedMembers.first { it.userId == member.id }.id

            val responseRequest = mapOf("response" to "DECLINED")

            mockMvc.perform(
                patch("/api/v1/services/$serviceId/assignments/$assignmentId")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(responseRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.status").value("DECLINED"))
        }

        // Validates: Requirement 14.5
        @Test
        fun `should get confirmation status and return 200 with assignments`() {
            val registration = testData.registerChurch(adminEmail = "svc-confirm@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "svc-confirm-member@testchurch.com",
                    firstName = "Confirm",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val scheduleRequest = mapOf(
                "serviceName" to "Sunday Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                )
            )

            val scheduleResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(scheduleRequest))
            )
                .expectCreated()

            val serviceId = scheduleResult.extractUUID("$.serviceId")

            mockMvc.perform(
                get("/api/v1/services/$serviceId/confirmations")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].status").exists())
        }

        // Validates: Business Rule - Service event conflict within 2 hours
        @Test
        fun `should return 400 when scheduling service within 2 hours of existing service`() {
            val registration = testData.registerChurch(adminEmail = "svc-conflict@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "svc-conflict-member@testchurch.com",
                    firstName = "Conflict",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(14)

            // Create the first service directly via repository to ensure it's visible in the same transaction
            val firstService = com.worshiphub.domain.scheduling.ServiceEvent(
                name = "First Service",
                scheduledDate = futureDate,
                teamId = teamId,
                churchId = registration.churchId,
                status = com.worshiphub.domain.scheduling.ServiceEventStatus.PUBLISHED
            )
            serviceEventRepository.save(firstService)

            // Try to schedule another service within 1 hour of the first (within 2-hour window)
            val conflictingDate = futureDate.plusHours(1)

            val conflictRequest = mapOf(
                "serviceName" to "Conflicting Service",
                "scheduledDate" to conflictingDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Drums")
                )
            )

            mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(conflictRequest))
            )
                .expectBadRequest()
        }

        // Validates: Business Rule - User availability enforcement
        @Test
        fun `should return 400 when assigning unavailable member to service`() {
            val registration = testData.registerChurch(adminEmail = "svc-unavail@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "svc-unavail-member@testchurch.com",
                    firstName = "Unavail",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val serviceDate = LocalDateTime.now().plusDays(14)

            // Mark the member as unavailable for the service date
            val unavailRequest = mapOf(
                "unavailableDate" to serviceDate.toLocalDate().toString(),
                "reason" to "Family event"
            )

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(unavailRequest))
            )
                .expectCreated()

            // Try to schedule a service on that date with the unavailable member
            val scheduleRequest = mapOf(
                "serviceName" to "Service With Unavailable Member",
                "scheduledDate" to serviceDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                )
            )

            mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(scheduleRequest))
            )
                .expectBadRequest()
        }

        // Validates: Requirement 14.6
        @Test
        fun `should return 400 when scheduling service with invalid data`() {
            val registration = testData.registerChurch(adminEmail = "svc-invalid@testchurch.com")

            // Send a request with blank serviceName — this passes deserialization
            // but fails @NotBlank validation
            val request = mapOf(
                "serviceName" to "  ",
                "scheduledDate" to LocalDateTime.now().plusDays(1).toString(),
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
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectBadRequest()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 15: Recurring Services
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 15 - Recurring Services")
    inner class RecurringServices {

        // Validates: Requirement 15.1
        @Test
        fun `should create recurring service with recurrenceRule and return 201`() {
            val registration = testData.registerChurch(adminEmail = "recur-create@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "recur-member1@testchurch.com",
                    firstName = "Recur",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val endDate = LocalDate.now().plusMonths(3)
            val request = mapOf(
                "serviceName" to "Weekly Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                ),
                "recurrenceRule" to mapOf(
                    "frequency" to "WEEKLY",
                    "recurrenceEndDate" to endDate.toString()
                )
            )

            mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.serviceId").exists())
        }

        // Validates: Requirement 15.2
        @Test
        fun `should update recurrence rule and return 200`() {
            val registration = testData.registerChurch(adminEmail = "recur-update@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "recur-update-member@testchurch.com",
                    firstName = "RecurUpd",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val endDate = LocalDate.now().plusMonths(3)
            val createRequest = mapOf(
                "serviceName" to "Weekly Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                ),
                "recurrenceRule" to mapOf(
                    "frequency" to "WEEKLY",
                    "recurrenceEndDate" to endDate.toString()
                )
            )

            val createResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest))
            )
                .expectCreated()

            val serviceId = createResult.extractUUID("$.serviceId")

            val updateRequest = mapOf(
                "frequency" to "MONTHLY",
                "recurrenceEndDate" to LocalDate.now().plusMonths(6).toString()
            )

            mockMvc.perform(
                put("/api/v1/services/$serviceId/recurrence")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectOk()
        }

        // Validates: Requirement 15.3
        @Test
        fun `should delete recurring service and return 204`() {
            val registration = testData.registerChurch(adminEmail = "recur-delete@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "recur-delete-member@testchurch.com",
                    firstName = "RecurDel",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val endDate = LocalDate.now().plusMonths(3)
            val createRequest = mapOf(
                "serviceName" to "Weekly Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                ),
                "recurrenceRule" to mapOf(
                    "frequency" to "WEEKLY",
                    "recurrenceEndDate" to endDate.toString()
                )
            )

            val createResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest))
            )
                .expectCreated()

            val serviceId = createResult.extractUUID("$.serviceId")

            mockMvc.perform(
                delete("/api/v1/services/$serviceId/recurring")
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

        // Validates: Requirement 15.4
        @Test
        fun `should return 400 when updating recurrence with unsupported frequency`() {
            val registration = testData.registerChurch(adminEmail = "recur-bad@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val member = userRepository.save(
                User(
                    email = "recur-bad-member@testchurch.com",
                    firstName = "RecurBad",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val futureDate = LocalDateTime.now().plusDays(7)
            val endDate = LocalDate.now().plusMonths(3)
            val createRequest = mapOf(
                "serviceName" to "Weekly Worship",
                "scheduledDate" to futureDate.toString(),
                "teamId" to teamId.toString(),
                "memberAssignments" to listOf(
                    mapOf("userId" to member.id.toString(), "role" to "Lead Vocalist")
                ),
                "recurrenceRule" to mapOf(
                    "frequency" to "WEEKLY",
                    "recurrenceEndDate" to endDate.toString()
                )
            )

            val createResult = mockMvc.perform(
                post("/api/v1/services")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest))
            )
                .expectCreated()

            val serviceId = createResult.extractUUID("$.serviceId")

            val updateRequest = mapOf(
                "frequency" to "DAILY",
                "recurrenceEndDate" to LocalDate.now().plusMonths(6).toString()
            )

            mockMvc.perform(
                put("/api/v1/services/$serviceId/recurrence")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectBadRequest()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 16: Setlists CRUD
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 16 - Setlists CRUD")
    inner class SetlistsCrud {

        // Validates: Requirement 16.1
        @Test
        fun `should create setlist with name and songIds and return 201 with setlistId`() {
            val registration = testData.registerChurch(adminEmail = "setlist-create@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val request = mapOf(
                "name" to "Sunday Setlist",
                "songIds" to listOf(songId.toString())
            )

            mockMvc.perform(
                post("/api/v1/setlists")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.setlistId").exists())
        }

        // Validates: Requirement 16.2
        @Test
        fun `should list setlists and return 200 with content`() {
            val registration = testData.registerChurch(adminEmail = "setlist-list@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                get("/api/v1/setlists")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].name").exists())
                .andExpect(jsonPath("$.content[0].songIds").isArray)
                .andExpect(jsonPath("$.content[0].createdAt").exists())
        }

        // Validates: Requirement 16.3
        @Test
        fun `should get setlist by ID and return 200`() {
            val registration = testData.registerChurch(adminEmail = "setlist-get@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                get("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.songIds").isArray)
        }

        // Validates: Requirement 16.4
        @Test
        fun `should update setlist and return 200`() {
            val registration = testData.registerChurch(adminEmail = "setlist-update@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            val updateRequest = mapOf(
                "name" to "Updated Setlist",
                "description" to "Updated description",
                "songIds" to listOf(songId.toString()),
                "estimatedDuration" to 45.0
            )

            mockMvc.perform(
                put("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Setlist updated successfully"))
        }

        // Validates: Requirement 16.5
        @Test
        fun `should delete setlist and return 204`() {
            val registration = testData.registerChurch(adminEmail = "setlist-delete@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                delete("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectNoContent()
        }

        // Validates: Requirement 16.6
        @Test
        fun `should add song to setlist and return 200`() {
            val registration = testData.registerChurch(adminEmail = "setlist-addsong@testchurch.com")

            val songId1 = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "First Song"
            )
            val songId2 = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Second Song",
                artist = "Another Artist"
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId1)
            )

            val addRequest = mapOf(
                "songId" to songId2.toString(),
                "position" to 1
            )

            mockMvc.perform(
                post("/api/v1/setlists/$setlistId/songs")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Song added to setlist successfully"))

            // Follow-up GET to verify the setlist's songIds were updated
            mockMvc.perform(
                get("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.songIds").isArray)
                .andExpect(jsonPath("$.songIds.length()").value(2))
        }

        // Validates: Requirement 16.7
        @Test
        fun `should remove song from setlist and return 204`() {
            val registration = testData.registerChurch(adminEmail = "setlist-removesong@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                delete("/api/v1/setlists/$setlistId/songs/$songId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectNoContent()

            // Follow-up GET to verify the setlist's songIds were updated (song removed)
            mockMvc.perform(
                get("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$.songIds").isArray)
                .andExpect(jsonPath("$.songIds.length()").value(0))
        }
        // Validates: Business Rule - Setlist duration limit (90 minutes max)
        @Test
        fun `should return 400 when setlist duration exceeds 90 minutes`() {
            val registration = testData.registerChurch(adminEmail = "setlist-maxdur@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Duration Test Song"
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            // Try to update setlist with duration exceeding 90 minutes
            val updateRequest = mapOf(
                "name" to "Over-Duration Setlist",
                "description" to "This setlist is too long",
                "songIds" to listOf(songId.toString()),
                "estimatedDuration" to 91.0
            )

            mockMvc.perform(
                put("/api/v1/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .expectBadRequest()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 17: Advanced Setlist Management
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 17 - Advanced Setlist Management")
    inner class AdvancedSetlistManagement {

        // Validates: Requirement 17.1
        @Test
        fun `should reorder songs in setlist and return 200`() {
            val registration = testData.registerChurch(adminEmail = "setlist-reorder@testchurch.com")

            val songId1 = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Reorder Song 1"
            )
            val songId2 = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                title = "Reorder Song 2",
                artist = "Another Artist"
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId1, songId2)
            )

            // Reorder: swap the songs
            val reorderRequest = mapOf(
                "songOrder" to listOf(songId2.toString(), songId1.toString())
            )

            mockMvc.perform(
                patch("/api/v1/services/setlists/$setlistId/songs/reorder")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reorderRequest))
            )
                .expectOk()
                .andExpect(jsonPath("$.message").value("Setlist reordered successfully"))
        }

        // Validates: Requirement 17.2
        @Test
        fun `should get setlist details and return 200 with id, name, songs, totalDuration, createdAt`() {
            val registration = testData.registerChurch(adminEmail = "setlist-details@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                get("/api/v1/services/setlists/$setlistId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.songs").exists())
                .andExpect(jsonPath("$.totalDuration").exists())
                .andExpect(jsonPath("$.createdAt").exists())
        }

        // Validates: Requirement 17.3
        @Test
        fun `should calculate setlist duration and return 200 with durationMinutes`() {
            val registration = testData.registerChurch(adminEmail = "setlist-duration@testchurch.com")

            val songId = testData.createSong(
                userId = registration.adminUserId,
                churchId = registration.churchId
            )

            val setlistId = testData.createSetlist(
                userId = registration.adminUserId,
                churchId = registration.churchId,
                songIds = listOf(songId)
            )

            mockMvc.perform(
                get("/api/v1/services/setlists/$setlistId/duration")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .expectOk()
                .andExpect(jsonPath("$.durationMinutes").exists())
        }

        // Validates: Requirement 17.4
        @Test
        fun `should auto-generate setlist and return 201 with setlistId`() {
            val registration = testData.registerChurch(adminEmail = "setlist-generate@testchurch.com")

            // Create enough songs for auto-generation
            for (i in 1..5) {
                testData.createSong(
                    userId = registration.adminUserId,
                    churchId = registration.churchId,
                    title = "Generated Song $i",
                    artist = "Artist $i"
                )
            }

            val request = mapOf(
                "name" to "Auto-Generated Setlist",
                "rules" to mapOf(
                    "openingSongs" to 1,
                    "worshipSongs" to 2,
                    "offeringSongs" to 1,
                    "closingSongs" to 1
                )
            )

            mockMvc.perform(
                post("/api/v1/services/setlists/generate")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.setlistId").exists())
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 18: Availability
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 18 - Availability")
    inner class Availability {

        // Validates: Requirement 18.1
        @Test
        fun `should mark unavailability and return 201 with availabilityId`() {
            val registration = testData.registerChurch(adminEmail = "avail-mark@testchurch.com")

            val member = userRepository.save(
                User(
                    email = "avail-member1@testchurch.com",
                    firstName = "Avail",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            val request = mapOf(
                "unavailableDate" to LocalDate.now().plusDays(14).toString(),
                "reason" to "Family vacation"
            )

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .expectCreated()
                .andExpect(jsonPath("$.availabilityId").exists())
        }

        // Validates: Requirement 18.2
        @Test
        fun `should get own unavailability records and return 200 with list`() {
            val registration = testData.registerChurch(adminEmail = "avail-get@testchurch.com")

            val member = userRepository.save(
                User(
                    email = "avail-get-member@testchurch.com",
                    firstName = "AvailGet",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            // Mark unavailability first
            val markRequest = mapOf(
                "unavailableDate" to LocalDate.now().plusDays(14).toString(),
                "reason" to "Conference"
            )

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(markRequest))
            )
                .expectCreated()

            // Get own unavailability records
            mockMvc.perform(
                get("/api/v1/services/availability/me")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].unavailableDate").exists())
                .andExpect(jsonPath("$[0].reason").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
        }

        // Validates: Requirement 18.3
        @Test
        fun `should get unavailability filtered by date range and return 200`() {
            val registration = testData.registerChurch(adminEmail = "avail-filter@testchurch.com")

            val member = userRepository.save(
                User(
                    email = "avail-filter-member@testchurch.com",
                    firstName = "AvailFilter",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            // Mark two unavailability dates
            val date1 = LocalDate.now().plusDays(10)
            val date2 = LocalDate.now().plusDays(30)

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("unavailableDate" to date1.toString(), "reason" to "Trip")))
            )
                .expectCreated()

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("unavailableDate" to date2.toString(), "reason" to "Conference")))
            )
                .expectCreated()

            // Filter by date range that includes only the first date
            val startDate = LocalDate.now().plusDays(5)
            val endDate = LocalDate.now().plusDays(15)

            mockMvc.perform(
                get("/api/v1/services/availability/me")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
        }

        // Validates: Requirement 18.4
        @Test
        fun `should delete unavailability record and return 204`() {
            val registration = testData.registerChurch(adminEmail = "avail-delete@testchurch.com")

            val member = userRepository.save(
                User(
                    email = "avail-delete-member@testchurch.com",
                    firstName = "AvailDel",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            // Mark unavailability
            val markRequest = mapOf(
                "unavailableDate" to LocalDate.now().plusDays(14).toString(),
                "reason" to "Personal"
            )

            val markResult = mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(markRequest))
            )
                .expectCreated()

            val availabilityId = markResult.extractUUID("$.availabilityId")

            // Delete the record
            mockMvc.perform(
                delete("/api/v1/services/availability/$availabilityId")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
            )
                .expectNoContent()
        }

        // Validates: Requirement 18.5
        @Test
        fun `should get team availability and return 200 with member availability data`() {
            val registration = testData.registerChurch(adminEmail = "avail-team@testchurch.com")
            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create a team member
            val member = userRepository.save(
                User(
                    email = "avail-team-member@testchurch.com",
                    firstName = "AvailTeam",
                    lastName = "Member",
                    passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                    churchId = registration.churchId,
                    role = UserRole.TEAM_MEMBER,
                    isActive = true,
                    isEmailVerified = true
                )
            )

            // Add member to team
            val addMemberRequest = mapOf(
                "userId" to member.id.toString(),
                "teamRole" to "LEAD_VOCALIST"
            )

            mockMvc.perform(
                post("/api/v1/teams/$teamId/members")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequest))
            )
                .andExpect(status().isCreated)

            // Mark member as unavailable
            val unavailDate = LocalDate.now().plusDays(14)
            val markRequest = mapOf(
                "unavailableDate" to unavailDate.toString(),
                "reason" to "Travel"
            )

            mockMvc.perform(
                post("/api/v1/services/availability/unavailable")
                    .with(
                        TestSecurityHelper.withAuth(
                            member.id,
                            registration.churchId,
                            listOf("TEAM_MEMBER")
                        )
                    )
                    .header("User-Id", member.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(markRequest))
            )
                .expectCreated()

            // Get team availability
            val startDate = LocalDate.now().plusDays(1)
            val endDate = LocalDate.now().plusDays(30)

            mockMvc.perform(
                get("/api/v1/teams/$teamId/availability")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
            )
                .expectOk()
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].teamRole").exists())
                .andExpect(jsonPath("$[0].unavailableDates").isArray)
        }
    }
}
