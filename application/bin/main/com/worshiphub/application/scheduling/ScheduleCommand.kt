package com.worshiphub.application.scheduling

import java.time.LocalDateTime
import java.util.*

/**
 * Command for scheduling a team for a service.
 * 
 * @property serviceName Name of the service event
 * @property scheduledDate Date and time of the service
 * @property teamId ID of the team to schedule
 * @property setlistId Optional setlist for the service
 * @property memberAssignments List of member assignments with roles
 * @property churchId ID of the church hosting the service
 */
data class ScheduleCommand(
    val serviceName: String,
    val scheduledDate: LocalDateTime,
    val teamId: UUID,
    val setlistId: UUID? = null,
    val memberAssignments: List<MemberAssignment>,
    val churchId: UUID
)

/**
 * Member assignment within a schedule command.
 * 
 * @property userId ID of the user to assign
 * @property role Role for this service
 */
data class MemberAssignment(
    val userId: UUID,
    val role: String
)