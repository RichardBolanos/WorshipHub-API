package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.AttachmentType
import java.util.*

/**
 * Command for adding an attachment to a song.
 */
data class AddAttachmentCommand(
    val songId: UUID,
    val name: String,
    val url: String,
    val type: AttachmentType,
    val addedBy: UUID? = null  // User who is adding the attachment (for push notifications)
)