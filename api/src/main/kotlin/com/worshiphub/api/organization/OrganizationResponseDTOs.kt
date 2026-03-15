package com.worshiphub.api.organization

import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.scheduling.ServiceEventStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Team creation response")
data class CreateTeamResponse(
    @Schema(description = "Team ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val teamId: UUID,
    
    @Schema(description = "Success message", example = "Team created successfully")
    val message: String = "Team created successfully"
)

@Schema(description = "Team information")
data class TeamResponse(
    @Schema(description = "Team ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    
    @Schema(description = "Team name", example = "Sunday Morning Worship Team")
    val name: String,
    
    @Schema(description = "Team description", example = "Main worship team for Sunday morning services")
    val description: String?,
    
    @Schema(description = "Team leader ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val leaderId: UUID,

    @Schema(description = "Team leader name", example = "Juan Pérez")
    val leaderName: String?,
    
    @Schema(description = "Church ID", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val churchId: UUID,
    
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime
)

@Schema(description = "Church information")
data class ChurchResponse(
    @Schema(description = "Church ID", example = "987fcdeb-51a2-43d1-9c4e-123456789abc")
    val id: UUID,
    
    @Schema(description = "Church name", example = "Grace Community Church")
    val name: String,
    
    @Schema(description = "Church address", example = "123 Main Street, Springfield, IL 62701")
    val address: String,
    
    @Schema(description = "Church email", example = "info@gracecommunity.org")
    val email: String,
    
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime
)

@Schema(description = "Team member information")
data class TeamMemberResponse(
    @Schema(description = "Team member ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,

    @Schema(description = "Role of the member in the team", example = "LEAD_VOCALIST")
    val teamRole: TeamRole,

    @Schema(description = "Timestamp when the member joined the team")
    val joinedAt: LocalDateTime
)

@Schema(description = "Upcoming service event information")
data class UpcomingServiceResponse(
    @Schema(description = "Service event ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,

    @Schema(description = "Service event name", example = "Sunday Morning Service")
    val name: String,

    @Schema(description = "Scheduled date and time of the service")
    val scheduledDate: LocalDateTime,

    @Schema(description = "Current status of the service event", example = "PUBLISHED")
    val status: ServiceEventStatus,

    @Schema(description = "Number of confirmed members", example = "5")
    val confirmedCount: Int,

    @Schema(description = "Number of assigned members", example = "8")
    val assignedCount: Int
)

@Schema(description = "Member availability information")
data class MemberAvailabilityResponse(
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,

    @Schema(description = "Role of the member in the team", example = "LEAD_VOCALIST")
    val teamRole: TeamRole,

    @Schema(description = "List of dates when the member is unavailable")
    val unavailableDates: List<UnavailableDateResponse>
)

@Schema(description = "Unavailable date information")
data class UnavailableDateResponse(
    @Schema(description = "Date of unavailability", example = "2025-01-15")
    val date: LocalDate,

    @Schema(description = "Reason for unavailability", example = "Vacation")
    val reason: String?
)

@Schema(description = "Team summary with activity statistics")
data class TeamSummaryResponse(
    @Schema(description = "Total number of team members", example = "8")
    val totalMembers: Int,

    @Schema(description = "Number of services in the last 30 days", example = "4")
    val recentServicesCount: Int,

    @Schema(description = "Number of upcoming scheduled services", example = "3")
    val upcomingServicesCount: Int,

    @Schema(description = "Distribution of roles in the team")
    val roleDistribution: Map<TeamRole, Int>
)

@Schema(description = "Response after assigning a member to a team")
data class AssignTeamMemberResponse(
    @Schema(description = "Team member ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val memberId: UUID,

    @Schema(description = "Success message", example = "Member assigned successfully")
    val message: String = "Member assigned successfully"
)
