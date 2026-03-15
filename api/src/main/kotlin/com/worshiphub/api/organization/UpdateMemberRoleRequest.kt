package com.worshiphub.api.organization

import com.worshiphub.domain.organization.TeamRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request data for updating a team member's role")
data class UpdateMemberRoleRequest(
    @field:NotNull
    @Schema(
        description = "New role for the team member",
        example = "LEAD_VOCALIST",
        required = true
    )
    val teamRole: TeamRole
)
