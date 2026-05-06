package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to cancel a scheduled service")
data class CancelServiceRequest(
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(
        description = "Optional cancellation reason",
        example = "Weather conditions",
        maxLength = 500
    )
    val reason: String? = null
)
