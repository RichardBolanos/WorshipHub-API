package com.worshiphub.application.organization

import com.worshiphub.domain.organization.UserRole
import java.util.*

/**
 * Command for inviting a user to a church.
 */
data class InviteUserCommand(
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val churchId: UUID
)