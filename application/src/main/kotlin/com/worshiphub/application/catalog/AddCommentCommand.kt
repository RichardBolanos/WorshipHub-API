package com.worshiphub.application.catalog

import java.util.*

/**
 * Command for adding a comment to a song.
 */
data class AddCommentCommand(
    val songId: UUID,
    val userId: UUID,
    val content: String
)