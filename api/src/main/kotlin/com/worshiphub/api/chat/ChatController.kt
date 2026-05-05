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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@Tag(name = "Team Chat", description = "REST API for team chat operations. Uses polling + FCM data messages for real-time updates.")
@RestController
class ChatController(
    private val chatApplicationService: ChatApplicationService,
    private val securityContext: SecurityContext
) {

    @Operation(
        summary = "Get team chat history",
        description = "Retrieves recent chat messages for a team, ordered by creation date descending."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Chat history retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "User not authorized to view team chat")
    ])
    @GetMapping("/api/v1/teams/{teamId}/chat/history")
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

    /**
     * Polling endpoint for incremental chat updates.
     * The frontend should call this endpoint every 5-10 seconds (configurable)
     * when the chat screen is active, passing the timestamp of the last received message.
     *
     * FCM data messages are sent as a complement to trigger immediate refresh
     * when a new message arrives, reducing polling latency.
     *
     * Polling strategy:
     * - Interval: 5-10 seconds (configurable on the frontend)
     * - Parameter: `since` (ISO 8601 timestamp of the last received message)
     * - Returns: only messages created after the `since` timestamp, ordered ascending
     * - FCM data messages act as a signal to trigger an immediate poll
     */
    @Operation(
        summary = "Get new chat messages since timestamp (polling endpoint)",
        description = """Retrieves chat messages created after the given timestamp for incremental polling.
            |The frontend should poll this endpoint every 5-10 seconds when the chat screen is active.
            |FCM data messages complement polling by triggering immediate refresh on new messages."""
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New messages retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "403", description = "User not authorized to view team chat")
    ])
    @GetMapping("/api/v1/teams/{teamId}/chat/messages")
    fun getMessagesSince(
        @Parameter(description = "Team ID", required = true) @PathVariable teamId: UUID,
        @Parameter(
            description = "ISO 8601 timestamp to fetch messages after (e.g., 2024-01-07T14:30:00). If omitted, returns the most recent messages.",
            required = false
        )
        @RequestParam(required = false) since: LocalDateTime?,
        @Parameter(description = "Maximum number of messages to retrieve") @RequestParam(defaultValue = "50") limit: Int
    ): List<ChatMessageResponseDto> {
        return try {
            if (since != null) {
                chatApplicationService.getTeamChatMessagesSince(teamId, since, limit).map { it.toDto() }
            } else {
                chatApplicationService.getTeamChatHistory(teamId, limit).map { it.toDto() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Operation(
        summary = "Send a chat message via REST",
        description = "Sends a message to a team chat. A silent FCM data message is sent to team members to trigger chat refresh."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Message sent successfully"),
        ApiResponse(responseCode = "400", description = "Invalid message data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER') or hasRole('TEAM_MEMBER')")
    @PostMapping("/api/v1/teams/{teamId}/messages")
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
        return savedMessage.toDto()
    }

    private fun ChatMessage.toDto() = ChatMessageResponseDto(
        id = id,
        teamId = teamId,
        userId = userId,
        content = content,
        createdAt = createdAt
    )
}
