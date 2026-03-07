package com.worshiphub.application.organization

import com.worshiphub.domain.organization.TeamRole
import java.util.*

/**
 * Command for assigning a member to a team.
 */
data class AssignTeamMemberCommand(
    val teamId: UUID,
    val userId: UUID,
    val teamRole: TeamRole
)