package com.worshiphub.api.integration.organization

import com.worshiphub.api.integration.BaseE2ETest
import com.worshiphub.api.integration.TestConstants
import com.worshiphub.api.integration.TestSecurityHelper
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
 * E2E integration tests for the Organization bounded context.
 * Covers Requirements 6-9: Role management, user profile, teams CRUD, and team members.
 */
class OrganizationE2ETest : BaseE2ETest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 6: Role Management
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 6 - Role Management")
    inner class RoleManagement {

        // Validates: Requirement 6.1
        @Test
        fun `should change user role and return 200`() {
            val registration = testData.registerChurch(adminEmail = "role-admin@testchurch.com")

            // Create a TEAM_MEMBER user in the same church
            val member = User(
                email = "role-member@testchurch.com",
                firstName = "Role",
                lastName = "Member",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val request = mapOf(
                "newRole" to "WORSHIP_LEADER",
                "reason" to "Promoted to worship leader"
            )

            mockMvc.perform(
                put("/api/v1/roles/users/${savedMember.id}")
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
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("User role changed successfully"))
        }

        // Validates: Requirement 6.2
        @Test
        fun `should return 400 when Church_Admin attempts to demote themselves`() {
            val registration = testData.registerChurch(adminEmail = "self-demote@testchurch.com")

            val request = mapOf(
                "newRole" to "TEAM_MEMBER",
                "reason" to "Self demotion attempt"
            )

            mockMvc.perform(
                put("/api/v1/roles/users/${registration.adminUserId}")
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
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Cannot demote yourself from Church Admin role"))
        }

        // Validates: Requirement 6.3
        @Test
        fun `should return 400 when changing role for user in different church`() {
            // Register two different churches
            val church1 = testData.registerChurch(
                adminEmail = "church1-admin@testchurch.com",
                churchName = "Church One",
                churchEmail = "church1@test.com"
            )
            val church2 = testData.registerChurch(
                adminEmail = "church2-admin@testchurch.com",
                churchName = "Church Two",
                churchEmail = "church2@test.com"
            )

            val request = mapOf(
                "newRole" to "WORSHIP_LEADER"
            )

            // Church1 admin tries to change role of Church2 admin
            mockMvc.perform(
                put("/api/v1/roles/users/${church2.adminUserId}")
                    .with(
                        TestSecurityHelper.withAuth(
                            church1.adminUserId,
                            church1.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        // Validates: Requirement 6.4
        @Test
        fun `should list church users and return 200 with user details`() {
            val registration = testData.registerChurch(adminEmail = "list-users-admin@testchurch.com")

            // Activate the admin user so it appears in the active users list
            val adminUser = userRepository.findByEmail("list-users-admin@testchurch.com")!!
            userRepository.save(adminUser.copy(isActive = true, isEmailVerified = true))

            mockMvc.perform(
                get("/api/v1/roles/users")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.users").isArray)
                .andExpect(jsonPath("$.users[0].userId").exists())
                .andExpect(jsonPath("$.users[0].email").exists())
                .andExpect(jsonPath("$.users[0].firstName").exists())
                .andExpect(jsonPath("$.users[0].lastName").exists())
                .andExpect(jsonPath("$.users[0].role").exists())
                .andExpect(jsonPath("$.users[0].isActive").exists())
                .andExpect(jsonPath("$.users[0].isEmailVerified").exists())
        }

        // Validates: Requirement 6.5
        @Test
        fun `should list available roles without SUPER_ADMIN and return 200`() {
            val registration = testData.registerChurch(adminEmail = "list-roles-admin@testchurch.com")

            mockMvc.perform(
                get("/api/v1/roles/available")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.roles").isArray)
                .andExpect(jsonPath("$.roles[0].name").exists())
                .andExpect(jsonPath("$.roles[0].displayName").exists())
                .andExpect(jsonPath("$.roles[0].description").exists())
                .andExpect(jsonPath("$.roles[0].canManageChurch").exists())
                .andExpect(jsonPath("$.roles[0].canManageTeams").exists())
                .andExpect(jsonPath("$.roles[0].canScheduleServices").exists())
                // Verify SUPER_ADMIN is not in the list
                .andExpect(jsonPath("$.roles[?(@.name == 'SUPER_ADMIN')]").isEmpty)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 7: User Profile
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 7 - User Profile")
    inner class UserProfile {

        // Validates: Requirement 7.1
        @Test
        fun `should get authenticated user profile and return 200 with all fields`() {
            val registration = testData.registerChurch(adminEmail = "profile-get@testchurch.com")

            mockMvc.perform(
                get("/api/v1/users/profile")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(registration.adminUserId.toString()))
                .andExpect(jsonPath("$.email").value("profile-get@testchurch.com"))
                .andExpect(jsonPath("$.firstName").value(TestConstants.ADMIN_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(TestConstants.ADMIN_LAST_NAME))
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.churchId").value(registration.churchId.toString()))
                .andExpect(jsonPath("$.isEmailVerified").exists())
                .andExpect(jsonPath("$.hasPassword").exists())
        }

        // Validates: Requirement 7.2
        @Test
        fun `should update profile with firstName and lastName and return 200`() {
            val registration = testData.registerChurch(adminEmail = "profile-update@testchurch.com")

            val request = mapOf(
                "firstName" to "UpdatedFirst",
                "lastName" to "UpdatedLast"
            )

            mockMvc.perform(
                patch("/api/v1/users/profile")
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
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 8: Teams CRUD
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 8 - Teams CRUD")
    inner class TeamsCrud {

        // Validates: Requirement 8.1
        @Test
        fun `should create team and return 201 with teamId`() {
            val registration = testData.registerChurch(adminEmail = "team-create@testchurch.com")

            val request = mapOf(
                "name" to TestConstants.TEAM_NAME,
                "description" to TestConstants.TEAM_DESCRIPTION,
                "leaderId" to registration.adminUserId.toString()
            )

            mockMvc.perform(
                post("/api/v1/teams")
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
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.teamId").exists())
        }

        // Validates: Requirement 8.2
        @Test
        fun `should list teams for church and return 200 with team details`() {
            val registration = testData.registerChurch(adminEmail = "team-list@testchurch.com")

            // Create a team first
            testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            mockMvc.perform(
                get("/api/v1/teams")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .header("Church-Id", registration.churchId.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].leaderId").exists())
                .andExpect(jsonPath("$[0].churchId").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
        }

        // Validates: Requirement 8.3
        @Test
        fun `should get team by ID and return 200 with complete details`() {
            val registration = testData.registerChurch(adminEmail = "team-get@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            mockMvc.perform(
                get("/api/v1/teams/$teamId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(teamId.toString()))
                .andExpect(jsonPath("$.name").value(TestConstants.TEAM_NAME))
                .andExpect(jsonPath("$.description").value(TestConstants.TEAM_DESCRIPTION))
                .andExpect(jsonPath("$.leaderId").value(registration.adminUserId.toString()))
                .andExpect(jsonPath("$.churchId").value(registration.churchId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
        }

        // Validates: Requirement 8.4
        @Test
        fun `should update team and return 200 with updated details`() {
            val registration = testData.registerChurch(adminEmail = "team-update@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            val updateRequest = mapOf(
                "name" to "Updated Team Name",
                "description" to "Updated description",
                "leaderId" to registration.adminUserId.toString()
            )

            mockMvc.perform(
                put("/api/v1/teams/$teamId")
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
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(teamId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Team Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
        }

        // Validates: Requirement 8.5
        @Test
        fun `should delete team and return 204`() {
            val registration = testData.registerChurch(adminEmail = "team-delete@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            mockMvc.perform(
                delete("/api/v1/teams/$teamId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isNoContent)
        }

        // Validates: Requirement 8.6
        @Test
        fun `should return 404 when getting non-existent team`() {
            val registration = testData.registerChurch(adminEmail = "team-notfound@testchurch.com")

            val nonExistentTeamId = UUID.randomUUID()

            mockMvc.perform(
                get("/api/v1/teams/$nonExistentTeamId")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isNotFound)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Requirement 9: Team Members
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Req 9 - Team Members")
    inner class TeamMembers {

        // Validates: Requirement 9.1
        @Test
        fun `should assign member to team and return 201 with memberId`() {
            val registration = testData.registerChurch(adminEmail = "member-assign@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create a user to assign as team member
            val member = User(
                email = "team-member-assign@testchurch.com",
                firstName = "Team",
                lastName = "Member",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val request = mapOf(
                "userId" to savedMember.id.toString(),
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
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.memberId").exists())
        }

        // Validates: Requirement 9.2
        @Test
        fun `should return 409 when assigning duplicate member to team`() {
            val registration = testData.registerChurch(adminEmail = "member-dup@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create a user to assign
            val member = User(
                email = "team-member-dup@testchurch.com",
                firstName = "Dup",
                lastName = "Member",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val request = mapOf(
                "userId" to savedMember.id.toString(),
                "teamRole" to "LEAD_VOCALIST"
            )

            // First assignment
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
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)

            // Duplicate assignment
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
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
        }

        // Validates: Requirement 9.3
        @Test
        fun `should list team members and return 200 with member details`() {
            val registration = testData.registerChurch(adminEmail = "member-list@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create and assign a member
            val member = User(
                email = "team-member-list@testchurch.com",
                firstName = "List",
                lastName = "Member",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val assignRequest = mapOf(
                "userId" to savedMember.id.toString(),
                "teamRole" to "BACKING_VOCALIST"
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
                    .content(objectMapper.writeValueAsString(assignRequest))
            )
                .andExpect(status().isCreated)

            // List members
            mockMvc.perform(
                get("/api/v1/teams/$teamId/members")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].teamRole").exists())
                .andExpect(jsonPath("$[0].joinedAt").exists())
        }

        // Validates: Requirement 9.4
        @Test
        fun `should update team member role and return 200`() {
            val registration = testData.registerChurch(adminEmail = "member-role-update@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create and assign a member
            val member = User(
                email = "team-member-role@testchurch.com",
                firstName = "Role",
                lastName = "Update",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val assignRequest = mapOf(
                "userId" to savedMember.id.toString(),
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
                    .content(objectMapper.writeValueAsString(assignRequest))
            )
                .andExpect(status().isCreated)

            // Update role
            val updateRoleRequest = mapOf(
                "teamRole" to "DRUMS"
            )

            mockMvc.perform(
                put("/api/v1/teams/$teamId/members/${savedMember.id}/role")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRoleRequest))
            )
                .andExpect(status().isOk)
        }

        // Validates: Requirement 9.5
        @Test
        fun `should remove team member and return 204`() {
            val registration = testData.registerChurch(adminEmail = "member-remove@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            // Create and assign a member
            val member = User(
                email = "team-member-remove@testchurch.com",
                firstName = "Remove",
                lastName = "Member",
                passwordHash = passwordEncoder.encode(TestConstants.VALID_PASSWORD),
                churchId = registration.churchId,
                role = UserRole.TEAM_MEMBER,
                isActive = true,
                isEmailVerified = true
            )
            val savedMember = userRepository.save(member)

            val assignRequest = mapOf(
                "userId" to savedMember.id.toString(),
                "teamRole" to "ACOUSTIC_GUITAR"
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
                    .content(objectMapper.writeValueAsString(assignRequest))
            )
                .andExpect(status().isCreated)

            // Remove member
            mockMvc.perform(
                delete("/api/v1/teams/$teamId/members/${savedMember.id}")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isNoContent)
        }

        // Validates: Requirement 9.6
        @Test
        fun `should get team summary and return 200 with statistics`() {
            val registration = testData.registerChurch(adminEmail = "member-summary@testchurch.com")

            val teamId = testData.createTeam(
                churchId = registration.churchId,
                leaderId = registration.adminUserId
            )

            mockMvc.perform(
                get("/api/v1/teams/$teamId/summary")
                    .with(
                        TestSecurityHelper.withAuth(
                            registration.adminUserId,
                            registration.churchId,
                            listOf("CHURCH_ADMIN")
                        )
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalMembers").exists())
                .andExpect(jsonPath("$.recentServicesCount").exists())
                .andExpect(jsonPath("$.upcomingServicesCount").exists())
                .andExpect(jsonPath("$.roleDistribution").exists())
        }
    }
}
