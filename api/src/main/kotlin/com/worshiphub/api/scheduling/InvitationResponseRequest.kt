package com.worshiphub.api.scheduling

import com.worshiphub.domain.scheduling.ConfirmationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Team member response to a service invitation")
data class InvitationResponseRequest(
    @field:NotNull
    @Schema(
        description = "Response to the service invitation", 
        example = "ACCEPTED", 
        required = true,
        allowableValues = ["ACCEPTED", "DECLINED", "PENDING"]
    )
    val response: ConfirmationStatus
)

@Schema(description = "Response confirmation for invitation")
data class InvitationResponseResponse(
    @Schema(
        description = "Confirmed response status", 
        example = "ACCEPTED"
    )
    val status: ConfirmationStatus
)