package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.RecurrenceRule
import java.time.LocalDateTime
import java.util.*

/**
 * Command for creating a recurring service event with a recurrence rule.
 *
 * @property serviceName Name of the service event
 * @property scheduledDate Date and time of the first service occurrence
 * @property teamId ID of the team to schedule
 * @property churchId ID of the church hosting the service
 * @property recurrenceRule Rule defining how the service repeats
 * @property memberAssignments Optional list of member assignments with roles
 */
data class CreateRecurringServiceCommand(
    val serviceName: String,
    val scheduledDate: LocalDateTime,
    val teamId: UUID,
    val churchId: UUID,
    val recurrenceRule: RecurrenceRule,
    val memberAssignments: List<MemberAssignment> = emptyList()
)
