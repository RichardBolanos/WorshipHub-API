package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

/**
 * Request DTO for specifying a recurrence rule when creating or updating a recurring service.
 */
@Schema(description = "Recurrence rule for a recurring service event")
data class RecurrenceRuleRequest(
    @field:NotBlank
    @Schema(
        description = "Recurrence frequency",
        example = "WEEKLY",
        required = true,
        allowableValues = ["WEEKLY", "MONTHLY", "YEARLY"]
    )
    val frequency: String,

    @Schema(
        description = "Optional end date for the recurrence series (YYYY-MM-DD)",
        example = "2025-06-30"
    )
    val recurrenceEndDate: LocalDate? = null
)
