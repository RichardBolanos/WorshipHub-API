package com.worshiphub.application.organization

import java.util.*

/**
 * Command for updating an existing worship team.
 */
data class UpdateTeamCommand(
    val teamId: UUID,
    val name: String,
    val description: String?,
    val leaderId: UUID
)
