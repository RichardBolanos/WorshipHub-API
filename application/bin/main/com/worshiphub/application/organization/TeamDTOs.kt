package com.worshiphub.application.organization

import com.worshiphub.domain.organization.TeamRole
import com.worshiphub.domain.scheduling.ServiceEventStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * DTO for upcoming service information returned by the application service.
 */
data class UpcomingServiceDTO(
    val id: UUID,
    val name: String,
    val scheduledDate: LocalDateTime,
    val status: ServiceEventStatus,
    val confirmedCount: Int,
    val assignedCount: Int
)

/**
 * DTO for member availability information returned by the application service.
 */
data class MemberAvailabilityDTO(
    val userId: UUID,
    val teamRole: TeamRole,
    val unavailableDates: List<UnavailableDateDTO>
)

/**
 * DTO for an unavailable date entry.
 */
data class UnavailableDateDTO(
    val date: LocalDate,
    val reason: String?
)

/**
 * DTO for team summary statistics returned by the application service.
 */
data class TeamSummaryDTO(
    val totalMembers: Int,
    val recentServicesCount: Int,
    val upcomingServicesCount: Int,
    val roleDistribution: Map<TeamRole, Int>
)
