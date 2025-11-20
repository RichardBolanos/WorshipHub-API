package com.worshiphub.api.scheduling

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request to respond to a service invitation")
data class InvitationResponseRequest(
    @field:NotBlank(message = "Response is required")
    @Schema(description = "Response to invitation", example = "ACCEPTED", allowableValues = ["ACCEPTED", "DECLINED"], required = true)
    val response: String
)