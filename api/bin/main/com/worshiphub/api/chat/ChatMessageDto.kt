package com.worshiphub.api.chat

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.*

@Schema(description = "WebSocket message data for sending team chat messages")
data class SendChatMessageDto(
    @field:NotNull
    @Schema(
        description = "ID of the team to send message to", 
        example = "123e4567-e89b-12d3-a456-426614174000", 
        required = true
    )
    val teamId: UUID,
    
    @field:NotBlank
    @field:Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
    @Schema(
        description = "Chat message content", 
        example = "Great practice today! See you all Sunday.", 
        required = true,
        minLength = 1,
        maxLength = 1000
    )
    val content: String
)

@Schema(description = "Chat message response with sender information and timestamp")
data class ChatMessageResponseDto(
    @Schema(
        description = "Unique message identifier", 
        example = "987fcdeb-51a2-43d1-9c4e-123456789abc"
    )
    val id: UUID,
    
    @Schema(
        description = "Team ID where message was sent", 
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val teamId: UUID,
    
    @Schema(
        description = "ID of user who sent the message", 
        example = "456e7890-e89b-12d3-a456-426614174111"
    )
    val userId: UUID,
    
    @Schema(
        description = "Message content", 
        example = "Great practice today! See you all Sunday."
    )
    val content: String,
    
    @Schema(
        description = "Message creation timestamp", 
        example = "2024-01-07T14:30:00"
    )
    val createdAt: LocalDateTime,
    
    @Schema(
        description = "Display name of message sender", 
        example = "John Doe"
    )
    val userName: String? = null
)