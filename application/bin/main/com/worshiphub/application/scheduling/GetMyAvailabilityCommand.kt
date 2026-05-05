package com.worshiphub.application.scheduling

import java.time.LocalDate
import java.util.*

/**
 * Command for querying the current user's unavailability records.
 *
 * @property userId ID of the user whose availability to retrieve
 * @property startDate Optional start date for filtering (inclusive)
 * @property endDate Optional end date for filtering (inclusive)
 */
data class GetMyAvailabilityCommand(
    val userId: UUID,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
