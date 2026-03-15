package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.CreateTeamCommand
import com.worshiphub.application.organization.AssignTeamMemberCommand
import com.worshiphub.application.organization.UpdateTeamCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ArraySchema
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import com.worshiphub.api.common.BadRequestException
import com.worshiphub.api.common.NotFoundException
import com.worshiphub.api.common.ConflictException
import java.time.LocalDate
import java.util.*

@Tag(name = "Teams", description = "Worship team management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/teams")
class TeamController(
    private val organizationApplicationService: OrganizationApplicationService
) {

    @Operation(
        summary = "Create a new worship team",
        description = "Creates a new worship team within a church organization",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Team successfully created",
                   content = [Content(schema = Schema(implementation = CreateTeamResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @Valid @RequestBody request: CreateTeamRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): CreateTeamResponse {
        val command = CreateTeamCommand(
            name = request.name,
            description = request.description,
            churchId = churchId,
            leaderId = request.leaderId
        )

        val result = organizationApplicationService.createTeam(command)
        return if (result.isSuccess) {
            CreateTeamResponse(teamId = result.getOrThrow())
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to create team")
        }
    }

    // ── Task 4.1: GET /api/v1/teams ──

    @Operation(
        summary = "List teams by church",
        description = "Returns all worship teams belonging to the specified church"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Teams retrieved successfully",
                   content = [Content(array = ArraySchema(schema = Schema(implementation = TeamResponse::class)))]),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @GetMapping
    fun getTeamsByChurchId(
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): List<TeamResponse> {
        val result = organizationApplicationService.getTeamsByChurchId(churchId)
        return if (result.isSuccess) {
            result.getOrThrow().map { team ->
                TeamResponse(
                    id = team.id,
                    name = team.name,
                    description = team.description,
                    leaderId = team.leaderId,
                    leaderName = organizationApplicationService.getUserFullName(team.leaderId),
                    churchId = team.churchId,
                    createdAt = team.createdAt
                )
            }
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to get teams")
        }
    }

    // ── Task 4.2: GET /api/v1/teams/{teamId} ──

    @Operation(
        summary = "Get team details",
        description = "Returns details of a specific worship team"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Team details retrieved successfully",
                   content = [Content(schema = Schema(implementation = TeamResponse::class))]),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @GetMapping("/{teamId}")
    fun getTeamById(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID
    ): TeamResponse {
        val result = organizationApplicationService.getTeamById(teamId)
        return if (result.isSuccess) {
            val team = result.getOrThrow()
            TeamResponse(
                id = team.id,
                name = team.name,
                description = team.description,
                leaderId = team.leaderId,
                leaderName = organizationApplicationService.getUserFullName(team.leaderId),
                churchId = team.churchId,
                createdAt = team.createdAt
            )
        } else {
            throw NotFoundException(result.exceptionOrNull()?.message ?: "Team not found")
        }
    }

    // ── Task 4.3: PUT /api/v1/teams/{teamId} ──

    @Operation(
        summary = "Update a worship team",
        description = "Updates the name, description, and leader of an existing worship team"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Team updated successfully",
                   content = [Content(schema = Schema(implementation = TeamResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PutMapping("/{teamId}")
    fun updateTeam(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Valid @RequestBody request: UpdateTeamRequest
    ): TeamResponse {
        val command = UpdateTeamCommand(
            teamId = teamId,
            name = request.name,
            description = request.description,
            leaderId = request.leaderId
        )

        val result = organizationApplicationService.updateTeam(command)
        return if (result.isSuccess) {
            val team = result.getOrThrow()
            TeamResponse(
                id = team.id,
                name = team.name,
                description = team.description,
                leaderId = team.leaderId,
                leaderName = organizationApplicationService.getUserFullName(team.leaderId),
                churchId = team.churchId,
                createdAt = team.createdAt
            )
        } else {
            val message = result.exceptionOrNull()?.message ?: "Failed to update team"
            if (message.contains("not found", ignoreCase = true)) {
                throw NotFoundException(message)
            } else {
                throw BadRequestException(message)
            }
        }
    }

    // ── Task 4.4: DELETE /api/v1/teams/{teamId} ──

    @Operation(
        summary = "Delete a worship team",
        description = "Deletes a worship team and all its associated members"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Team deleted successfully"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN')")
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeam(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID
    ) {
        val result = organizationApplicationService.deleteTeam(teamId)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Failed to delete team"
            if (message.contains("not found", ignoreCase = true)) {
                throw NotFoundException(message)
            } else {
                throw BadRequestException(message)
            }
        }
    }

    // ── Task 4.5: POST /api/v1/teams/{teamId}/members ──

    @Operation(
        summary = "Assign a member to a team",
        description = "Assigns a user as a member of the specified worship team with a given role"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Member assigned successfully",
                   content = [Content(schema = Schema(implementation = AssignTeamMemberResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "409", description = "User is already a member of this team"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun assignTeamMember(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Valid @RequestBody request: AssignTeamMemberRequest
    ): AssignTeamMemberResponse {
        val command = AssignTeamMemberCommand(
            teamId = teamId,
            userId = request.userId,
            teamRole = request.teamRole
        )

        val result = organizationApplicationService.assignTeamMember(command)
        return if (result.isSuccess) {
            AssignTeamMemberResponse(memberId = result.getOrThrow())
        } else {
            val message = result.exceptionOrNull()?.message ?: "Failed to assign team member"
            if (message.contains("already a member", ignoreCase = true)) {
                throw ConflictException(message)
            } else if (message.contains("not found", ignoreCase = true)) {
                throw NotFoundException(message)
            } else {
                throw BadRequestException(message)
            }
        }
    }

    // ── Task 4.6: GET /api/v1/teams/{teamId}/members ──

    @Operation(
        summary = "List team members",
        description = "Returns all members of the specified worship team"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Team members retrieved successfully",
                   content = [Content(array = ArraySchema(schema = Schema(implementation = TeamMemberResponse::class)))]),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @GetMapping("/{teamId}/members")
    fun getTeamMembers(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID
    ): List<TeamMemberResponse> {
        val result = organizationApplicationService.getTeamMembers(teamId)
        return if (result.isSuccess) {
            result.getOrThrow().map { member ->
                TeamMemberResponse(
                    id = member.id,
                    userId = member.userId,
                    teamRole = member.teamRole,
                    joinedAt = member.joinedAt
                )
            }
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to get team members")
        }
    }

    // ── Task 4.7: PUT /api/v1/teams/{teamId}/members/{userId}/role ──

    @Operation(
        summary = "Update team member role",
        description = "Updates the role of a member within the specified worship team"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Member role updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "404", description = "Team member not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PutMapping("/{teamId}/members/{userId}/role")
    fun updateTeamMemberRole(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Parameter(description = "User ID", required = true) @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateMemberRoleRequest
    ) {
        val result = organizationApplicationService.updateTeamMemberRole(teamId, userId, request.teamRole)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Failed to update member role"
            if (message.contains("not found", ignoreCase = true)) {
                throw NotFoundException(message)
            } else {
                throw BadRequestException(message)
            }
        }
    }

    // ── Task 4.8: DELETE /api/v1/teams/{teamId}/members/{userId} ──

    @Operation(
        summary = "Remove a member from a team",
        description = "Removes a user from the specified worship team"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Member removed successfully"),
        ApiResponse(responseCode = "404", description = "Team member not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeTeamMember(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Parameter(description = "User ID", required = true) @PathVariable userId: UUID
    ) {
        val result = organizationApplicationService.removeTeamMember(teamId, userId)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Failed to remove team member"
            if (message.contains("not found", ignoreCase = true)) {
                throw NotFoundException(message)
            } else {
                throw BadRequestException(message)
            }
        }
    }

    // ── Task 4.9: GET /api/v1/teams/{teamId}/upcoming-services ──

    @Operation(
        summary = "Get upcoming services for a team",
        description = "Returns upcoming service events for the specified worship team, sorted by date ascending"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Upcoming services retrieved successfully",
                   content = [Content(array = ArraySchema(schema = Schema(implementation = UpcomingServiceResponse::class)))]),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @GetMapping("/{teamId}/upcoming-services")
    fun getUpcomingServices(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID
    ): List<UpcomingServiceResponse> {
        val result = organizationApplicationService.getUpcomingServices(teamId)
        return if (result.isSuccess) {
            result.getOrThrow().map { dto ->
                UpcomingServiceResponse(
                    id = dto.id,
                    name = dto.name,
                    scheduledDate = dto.scheduledDate,
                    status = dto.status,
                    confirmedCount = dto.confirmedCount,
                    assignedCount = dto.assignedCount
                )
            }
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to get upcoming services")
        }
    }

    // ── Task 4.10: GET /api/v1/teams/{teamId}/availability ──

    @Operation(
        summary = "Get team member availability",
        description = "Returns availability information for all members of the specified team within a date range"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Availability retrieved successfully",
                   content = [Content(array = ArraySchema(schema = Schema(implementation = MemberAvailabilityResponse::class)))]),
        ApiResponse(responseCode = "400", description = "Invalid date parameters"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @GetMapping("/{teamId}/availability")
    fun getTeamAvailability(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Parameter(description = "Start date (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<MemberAvailabilityResponse> {
        val result = organizationApplicationService.getTeamAvailability(teamId, startDate, endDate)
        return if (result.isSuccess) {
            result.getOrThrow().map { dto ->
                MemberAvailabilityResponse(
                    userId = dto.userId,
                    teamRole = dto.teamRole,
                    unavailableDates = dto.unavailableDates.map { ud ->
                        UnavailableDateResponse(
                            date = ud.date,
                            reason = ud.reason
                        )
                    }
                )
            }
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to get team availability")
        }
    }

    // ── Task 4.11: GET /api/v1/teams/{teamId}/summary ──

    @Operation(
        summary = "Get team summary",
        description = "Returns a summary of team activity including member count, role distribution, and service statistics"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Team summary retrieved successfully",
                   content = [Content(schema = Schema(implementation = TeamSummaryResponse::class))]),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @GetMapping("/{teamId}/summary")
    fun getTeamSummary(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID
    ): TeamSummaryResponse {
        val result = organizationApplicationService.getTeamSummary(teamId)
        return if (result.isSuccess) {
            val dto = result.getOrThrow()
            TeamSummaryResponse(
                totalMembers = dto.totalMembers,
                recentServicesCount = dto.recentServicesCount,
                upcomingServicesCount = dto.upcomingServicesCount,
                roleDistribution = dto.roleDistribution
            )
        } else {
            throw BadRequestException(result.exceptionOrNull()?.message ?: "Failed to get team summary")
        }
    }
}
