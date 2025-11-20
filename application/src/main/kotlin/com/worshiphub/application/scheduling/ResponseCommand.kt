package com.worshiphub.application.scheduling

import com.worshiphub.domain.scheduling.ConfirmationStatus
import java.util.*

/**
 * Command for responding to a service invitation.
 * 
 * @property serviceEventId ID of the service event containing the assignment
 * @property assignmentId ID of the assignment to respond to
 * @property userId ID of the user responding (for security validation)
 * @property response Response to the invitation (ACCEPTED or DECLINED)
 */
data class ResponseCommand(
    val serviceEventId: UUID,
    val assignmentId: UUID,
    val userId: UUID,
    val response: ConfirmationStatus
)