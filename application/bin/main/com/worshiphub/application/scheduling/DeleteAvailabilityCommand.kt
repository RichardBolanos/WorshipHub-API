package com.worshiphub.application.scheduling

import java.util.*

/**
 * Command for deleting a user availability (unavailability) record.
 *
 * @property availabilityId ID of the availability record to delete
 * @property userId ID of the user requesting the deletion (for authorization)
 */
data class DeleteAvailabilityCommand(
    val availabilityId: UUID,
    val userId: UUID
)
