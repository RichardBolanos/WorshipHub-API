package com.worshiphub.api.chat

import com.worshiphub.application.chat.ChatApplicationService
import com.worshiphub.application.chat.SendMessageCommand
import com.worshiphub.domain.collaboration.ChatMessage
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Team Chat", description = "Real-time team chat operations via WebSocket and REST")
@Controller
class ChatController(
    private val chatApplicationService: ChatApplicationService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val securityContext: SecurityContext
) {
    
    /**
     * Handles incoming chat messages via WebSocket.
     */
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(
        @Payload message: SendChatMessageDto,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        try {
            // Extract userId from session attributes (set by auth interceptor)
            val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
                ?: throw IllegalStateException("User not authenticated")
            
            val command = SendMessageCommand(
                teamId = message.teamId,
                userId = userId,
                content = message.content
            )
            
            val savedMessage = chatApplicationService.sendMessage(command)
            
            val response = savedMessage.toDto()
            
            // Broadcast to team channel
            messagingTemplate.convertAndSend("/topic/team/${message.teamId}", response)
        } catch (e: Exception) {
            // Send error to user
            messagingTemplate.convertAndSendToUser(
                headerAccessor.user?.name ?: "anonymous",
                "/queue/errors",
                mapOf("error" to e.message)
            )
        }
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
        return try {
            chatApplicationService.getTeamChatHistory(teamId, limit).map { it.toDto() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Operation(
        summary = "Send a chat message via REST",
        description = "Sends a message to a team chat and broadcasts it via WebSocket"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Message sent successfully"),
        ApiResponse(responseCode = "400", description = "Invalid message data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @PostMapping("/api/v1/teams/{teamId}/messages")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    fun sendMessageRest(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Valid @RequestBody request: SendChatMessageRestDto
    ): ChatMessageResponseDto {
        val userId = securityContext.getCurrentUserId()

        val command = SendMessageCommand(
            teamId = teamId,
            userId = userId,
            content = request.content
        )

        val savedMessage = chatApplicationService.sendMessage(command)
        val response = savedMessage.toDto()

        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/team/$teamId", response)

        return response
    }

    private fun ChatMessage.toDto() = ChatMessageResponseDto(
        id = id,
        teamId = teamId,
        userId = userId,
        content = content,
        createdAt = createdAt
    )
}