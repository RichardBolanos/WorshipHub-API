package com.worshiphub.application.scheduling

import java.time.LocalDate
import java.util.*

/**
 * Command for marking user unavailability.
 */
data class MarkUnavailabilityCommand(
    val userId: UUID,
    val unavailableDate: LocalDate,
    val reason: String?
)