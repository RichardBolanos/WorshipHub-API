package com.worshiphub.application.organization

import java.util.*

/**
 * Command for creating a worship team.
 */
data class CreateTeamCommand(
    val name: String,
    val description: String?,
    val churchId: UUID,
    val leaderId: UUID
)