package com.worshiphub.api.organization

import com.worshiphub.domain.organization.TeamRole
import jakarta.validation.constraints.NotNull
import java.util.*

/**
 * Request DTO for team member assignment.
 */
data class AssignTeamMemberRequest(
    @field:NotNull
    val userId: UUID,
    
    @field:NotNull
    val teamRole: TeamRole
)