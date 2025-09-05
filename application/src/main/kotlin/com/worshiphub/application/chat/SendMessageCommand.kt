package com.worshiphub.application.chat

import java.util.*

/**
 * Command for sending a chat message.
 */
data class SendMessageCommand(
    val teamId: UUID,
    val userId: UUID,
    val content: String
)