package com.worshiphub.application.organization

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationRepository
import com.worshiphub.domain.organization.Church
import com.worshiphub.domain.organization.Team
import com.worshiphub.domain.organization.TeamMember
import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.organization.repository.ChurchRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.TeamMemberRepository
import com.worshiphub.domain.scheduling.ConfirmationStatus
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.UserAvailabilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for organization operations.
 */
@Service
open class OrganizationApplicationService(
    private val userRepository: UserRepository,
    private val churchRepository: ChurchRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val notificationRepository: NotificationRepository,
    private val serviceEventRepository: ServiceEventRepository,
    private val userAvailabilityRepository: UserAvailabilityRepository
) {

    /**
     * Gets church details by ID.
     */
    fun getChurch(churchId: UUID): Result<Church> {
        return try {
            val church = churchRepository.findById(churchId)
                ?: return Result.failure(RuntimeException("Church not found: $churchId"))
            Result.success(church)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Church not found", e))
        }
    }

    /**
     * Registers a new church in the platform.
     */
    @Transactional
    fun registerChurch(command: RegisterChurchCommand): Result<UUID> {
        return try {
            val church = Church(
                name = command.name,
                address = command.address,
                email = command.email
            )
            val savedChurch = churchRepository.save(church)
            Result.success(savedChurch.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to register church", e))
        }
    }

    /**
     * Creates a new worship team.
     */
    @Transactional
    fun createTeam(command: CreateTeamCommand): Result<UUID> {
        return try {
            val team = Team(
                name = command.name,
                description = command.description,
                churchId = command.churchId,
                leaderId = command.leaderId
            )
            val savedTeam = teamRepository.save(team)
            Result.success(savedTeam.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create team", e))
        }
    }

    // ── Task 3.1: getTeamsByChurchId ──

    /**
     * Gets all teams belonging to a church.
     */
    fun getTeamsByChurchId(churchId: UUID): Result<List<Team>> {
        return try {
            val teams = teamRepository.findByChurchId(churchId)
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get teams for church: $churchId", e))
        }
    }

    // ── Task 3.2: getTeamById ──

    /**
     * Gets a team by its ID. Returns failure with "Team not found" if not found.
     */
    fun getTeamById(teamId: UUID): Result<Team> {
        return try {
            val team = teamRepository.findById(teamId)
                ?: return Result.failure(RuntimeException("Team not found: $teamId"))
            Result.success(team)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get team: $teamId", e))
        }
    }

    // ── Task 3.3: updateTeam ──

    /**
     * Updates an existing team. Creates TEAM_LEADER_CHANGED notification if leader changed.
     */
    @Transactional
    fun updateTeam(command: UpdateTeamCommand): Result<Team> {
        return try {
            val existingTeam = teamRepository.findById(command.teamId)
                ?: return Result.failure(RuntimeException("Team not found: ${command.teamId}"))

            val leaderChanged = existingTeam.leaderId != command.leaderId

            val updatedTeam = existingTeam.copy(
                name = command.name,
                description = command.description,
                leaderId = command.leaderId
            )
            val savedTeam = teamRepository.save(updatedTeam)

            if (leaderChanged) {
                val members = teamMemberRepository.findByTeamId(command.teamId)
                members.forEach { member ->
                    notificationRepository.save(
                        Notification(
                            userId = member.userId,
                            title = "Team Leader Changed",
                            message = "The leader of team '${savedTeam.name}' has been changed.",
                            type = NotificationType.TEAM_LEADER_CHANGED
                        )
                    )
                }
            }

            Result.success(savedTeam)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to update team: ${command.teamId}", e))
        }
    }

    // ── Task 3.4: deleteTeam ──

    /**
     * Deletes a team and all its members.
     */
    @Transactional
    fun deleteTeam(teamId: UUID): Result<Unit> {
        return try {
            val team = teamRepository.findById(teamId)
                ?: return Result.failure(RuntimeException("Team not found: $teamId"))

            teamMemberRepository.deleteByTeamId(teamId)
            teamRepository.delete(team)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to delete team: $teamId", e))
        }
    }

    // ── Task 3.5: assignTeamMember (refactored) ──

    /**
     * Assigns a member to a team. Checks for duplicates and creates TEAM_MEMBER_ADDED notifications.
     */
    @Transactional
    fun assignTeamMember(command: AssignTeamMemberCommand): Result<UUID> {
        return try {
            val existingMember = teamMemberRepository.findByTeamIdAndUserId(command.teamId, command.userId)
            if (existingMember != null) {
                return Result.failure(RuntimeException("User is already a member of this team"))
            }

            val currentMembers = teamMemberRepository.findByTeamId(command.teamId)

            val teamMember = TeamMember(
                teamId = command.teamId,
                userId = command.userId,
                teamRole = command.teamRole
            )
            val savedTeamMember = teamMemberRepository.save(teamMember)

            val team = teamRepository.findById(command.teamId)
            val teamName = team?.name ?: "the team"
            currentMembers.forEach { member ->
                notificationRepository.save(
                    Notification(
                        userId = member.userId,
                        title = "New Team Member",
                        message = "A new member has been added to '$teamName'.",
                        type = NotificationType.TEAM_MEMBER_ADDED
                    )
                )
            }

            Result.success(savedTeamMember.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to assign team member", e))
        }
    }

    // ── Task 3.6: removeTeamMember (refactored) ──

    /**
     * Removes a member from a team. Verifies existence and creates TEAM_MEMBER_REMOVED notifications.
     */
    @Transactional
    fun removeTeamMember(teamId: UUID, userId: UUID): Result<Unit> {
        return try {
            teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                ?: return Result.failure(RuntimeException("Team member not found"))

            teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId)

            val remainingMembers = teamMemberRepository.findByTeamId(teamId)
            val team = teamRepository.findById(teamId)
            val teamName = team?.name ?: "the team"
            remainingMembers.forEach { member ->
                notificationRepository.save(
                    Notification(
                        userId = member.userId,
                        title = "Team Member Removed",
                        message = "A member has been removed from '$teamName'.",
                        type = NotificationType.TEAM_MEMBER_REMOVED
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to remove team member", e))
        }
    }

    /**
     * Gets team members.
     */
    fun getTeamMembers(teamId: UUID): Result<List<TeamMember>> {
        return try {
            val teamMembers = teamMemberRepository.findByTeamId(teamId)
            Result.success(teamMembers)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get team members", e))
        }
    }

    /**
     * Invites a user to join a church.
     */
    @Transactional
    fun inviteUser(command: InviteUserCommand): UUID {
        val user = User(
            email = command.email,
            firstName = command.firstName,
            lastName = command.lastName,
            passwordHash = "",
            churchId = command.churchId,
            role = command.role,
            isActive = false,
            isEmailVerified = false
        )
        val savedUser = userRepository.save(user)
        return savedUser.id
    }

    // ── Task 3.7: updateTeamMemberRole (refactored) ──

    /**
     * Updates a team member's role. Verifies existence and creates TEAM_ROLE_CHANGED notification.
     */
    @Transactional
    fun updateTeamMemberRole(teamId: UUID, userId: UUID, role: TeamRole): Result<Unit> {
        return try {
            val teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                ?: return Result.failure(RuntimeException("Team member not found"))

            val updatedTeamMember = teamMember.copy(teamRole = role)
            teamMemberRepository.save(updatedTeamMember)

            val team = teamRepository.findById(teamId)
            val teamName = team?.name ?: "the team"
            notificationRepository.save(
                Notification(
                    userId = userId,
                    title = "Role Changed",
                    message = "Your role in '$teamName' has been changed to ${role.name}.",
                    type = NotificationType.TEAM_ROLE_CHANGED
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to update team member role", e))
        }
    }

    fun getUserProfile(userId: UUID): Map<String, Any> {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found: $userId")

        return mapOf(
            "id" to user.id.toString(),
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "role" to user.role.name,
            "churchId" to user.churchId.toString(),
            "isEmailVerified" to user.isEmailVerified,
            "hasPassword" to (user.passwordHash?.isNotBlank() ?: "")
        )
    }

    @Transactional
    fun updateUserProfile(userId: UUID, updates: Map<String, String>) {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found: $userId")

        val updatedUser = user.copy(
            firstName = updates["firstName"] ?: user.firstName,
            lastName = updates["lastName"] ?: user.lastName
        )
        userRepository.save(updatedUser)
    }

    /**
     * Resolves a user's full name from their ID. Returns null if user not found.
     */
    fun getUserFullName(userId: UUID): String? {
        val user = userRepository.findById(userId) ?: return null
        return "${user.firstName} ${user.lastName}".trim()
    }

    // ── Task 3.8: getUpcomingServices ──

    /**
     * Gets upcoming services for a team, sorted by scheduledDate ascending.
     * Includes confirmed vs assigned member counts.
     */
    fun getUpcomingServices(teamId: UUID): Result<List<UpcomingServiceDTO>> {
        return try {
            val upcomingServices = serviceEventRepository.findUpcomingByTeamId(teamId)
            val sortedServices = upcomingServices.sortedBy { it.scheduledDate }

            val dtos = sortedServices.map { service ->
                val confirmedCount = service.assignedMembers.count {
                    it.confirmationStatus == ConfirmationStatus.ACCEPTED
                }
                val assignedCount = service.assignedMembers.size

                UpcomingServiceDTO(
                    id = service.id,
                    name = service.name,
                    scheduledDate = service.scheduledDate,
                    status = service.status,
                    confirmedCount = confirmedCount,
                    assignedCount = assignedCount
                )
            }

            Result.success(dtos)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get upcoming services for team: $teamId", e))
        }
    }

    // ── Task 3.9: getTeamAvailability ──

    /**
     * Gets team member availability for a date range.
     */
    fun getTeamAvailability(
        teamId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<MemberAvailabilityDTO>> {
        return try {
            val members = teamMemberRepository.findByTeamId(teamId)

            val availabilityDtos = members.map { member ->
                val unavailabilities = userAvailabilityRepository.findByUserIdAndDateRange(
                    member.userId, startDate, endDate
                )

                val unavailableDateDtos = unavailabilities
                    .filter { it.isWithinRange(startDate, endDate) }
                    .map { UnavailableDateDTO(date = it.unavailableDate, reason = it.reason) }

                MemberAvailabilityDTO(
                    userId = member.userId,
                    teamRole = member.teamRole,
                    unavailableDates = unavailableDateDtos
                )
            }

            Result.success(availabilityDtos)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get team availability for team: $teamId", e))
        }
    }

    // ── Task 3.10: getTeamSummary ──

    /**
     * Gets a summary of team activity including member count, role distribution, and service counts.
     */
    fun getTeamSummary(teamId: UUID): Result<TeamSummaryDTO> {
        return try {
            val totalMembers = teamMemberRepository.countByTeamId(teamId)

            val members = teamMemberRepository.findByTeamId(teamId)
            val roleDistribution = members.groupBy { it.teamRole }
                .mapValues { it.value.size }

            val now = LocalDateTime.now()
            val thirtyDaysAgo = now.minusDays(30)
            val recentServices = serviceEventRepository.findByTeamIdAndDateRange(teamId, thirtyDaysAgo, now)
            val recentServicesCount = recentServices.size

            val upcomingServices = serviceEventRepository.findUpcomingByTeamId(teamId)
            val upcomingServicesCount = upcomingServices.size

            Result.success(
                TeamSummaryDTO(
                    totalMembers = totalMembers,
                    recentServicesCount = recentServicesCount,
                    upcomingServicesCount = upcomingServicesCount,
                    roleDistribution = roleDistribution
                )
            )
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get team summary for team: $teamId", e))
        }
    }
}
