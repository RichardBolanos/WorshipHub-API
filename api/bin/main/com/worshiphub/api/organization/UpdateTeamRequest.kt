package com.worshiphub.api.organization

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

@Schema(description = "Request data for updating an existing worship team")
data class UpdateTeamRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
    @Schema(
        description = "Name of the worship team",
        example = "Sunday Morning Worship Team",
        required = true,
        minLength = 1,
        maxLength = 100
    )
    val name: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(
        description = "Optional description of the team's purpose and focus",
        example = "Main worship team for Sunday morning services, focusing on contemporary worship",
        maxLength = 500
    )
    val description: String?,

    @field:NotNull
    @Schema(
        description = "ID of the user who will lead this team",
        example = "123e4567-e89b-12d3-a456-426614174000",
        required = true
    )
    val leaderId: UUID
)
