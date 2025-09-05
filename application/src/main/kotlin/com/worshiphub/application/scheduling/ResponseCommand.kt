package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.ConfirmationStatus
import java.util.*

/**
 * Command for responding to a service invitation.
 * 
 * @property assignmentId ID of the assignment to respond to
 * @property userId ID of the user responding (for security validation)
 * @property response Response to the invitation (ACCEPTED or DECLINED)
 */
data class ResponseCommand(
    val assignmentId: UUID,
    val userId: UUID,
    val response: ConfirmationStatus
)