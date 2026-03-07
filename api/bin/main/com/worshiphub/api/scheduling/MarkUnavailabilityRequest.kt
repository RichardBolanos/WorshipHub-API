package com.worshiphub.api.scheduling

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * Request DTO for marking unavailability.
 */
data class MarkUnavailabilityRequest(
    @field:NotNull
    val unavailableDate: LocalDate,
    
    val reason: String?
)