package com.worshiphub.application.scheduling

import java.util.*

/**
 * Command for creating a setlist.
 */
data class CreateSetlistCommand(
    val name: String,
    val songIds: List<UUID>,
    val churchId: UUID
)