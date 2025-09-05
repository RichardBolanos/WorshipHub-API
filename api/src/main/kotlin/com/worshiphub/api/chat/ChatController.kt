package com.worshiphub.api.chat

import com.worshiphub.application.chat.ChatApplicationService
import com.worshiphub.application.chat.SendMessageCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Team Chat", description = "Real-time team chat operations via WebSocket and REST")
@Controller
class ChatController(
    private val chatApplicationService: ChatApplicationService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    
    /**
     * Handles incoming chat messages via WebSocket.
     */
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(
        @Payload message: SendChatMessageDto,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        // TODO: Extract userId from JWT token in headers
        val userId = UUID.randomUUID() // Placeholder
        
        val command = SendMessageCommand(
            teamId = message.teamId,
            userId = userId,
            content = message.content
        )
        
        val savedMessage = chatApplicationService.sendMessage(command)
        
        val response = ChatMessageResponseDto(
            id = savedMessage.id,
            teamId = savedMessage.teamId,
            userId = savedMessage.userId,
            content = savedMessage.content,
            createdAt = savedMessage.createdAt
        )
        
        // Broadcast to team channel
        messagingTemplate.convertAndSend("/topic/team/${message.teamId}", response)
    }
    
    @Operation(
        summary = "Get team chat history",
        description = "Retrieves recent chat messages for a team. WebSocket messages are sent to /topic/team/{teamId}"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Chat history retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "User not authorized to view team chat")
    ])
    @GetMapping("/api/v1/teams/{teamId}/chat/history")
    @ResponseBody
    fun getChatHistory(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Parameter(description = "Maximum number of messages to retrieve") @RequestParam(defaultValue = "50") limit: Int
    ): List<ChatMessageResponseDto> {
        val messages = chatApplicationService.getTeamChatHistory(teamId, limit)
        return messages.map { message ->
            ChatMessageResponseDto(
                id = message.id,
                teamId = message.teamId,
                userId = message.userId,
                content = message.content,
                createdAt = message.createdAt
            )
        }
    }
}