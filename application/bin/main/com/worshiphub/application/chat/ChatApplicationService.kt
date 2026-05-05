package com.worshiphub.application.chat

import com.worshiphub.domain.collaboration.ChatMessage
import com.worshiphub.domain.collaboration.push.PushEvent
import com.worshiphub.domain.collaboration.repository.ChatMessageRepository
import com.worshiphub.domain.organization.repository.TeamMemberRepository
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Application service for chat operations.
 */
@Service
@Transactional
open class ChatApplicationService(
    private val chatMessageRepository: ChatMessageRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    
    /**
     * Sends a message to a team chat.
     */
    fun sendMessage(command: SendMessageCommand): ChatMessage {
        val message = ChatMessage(
            teamId = command.teamId,
            userId = command.userId,
            content = command.content
        )
        
        val savedMessage = chatMessageRepository.save(message)

        // Publish push event for chat message (Requisitos 3.1, 3.2, 3.3)
        try {
            val teamMembers = teamMemberRepository.findByTeamId(command.teamId)
            val recipientIds = teamMembers
                .map { it.userId }
                .filter { it != command.userId } // Exclude sender (Req 3.1)

            if (recipientIds.isNotEmpty()) {
                val sender = userRepository.findById(command.userId)
                val senderName = sender?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"

                val team = teamRepository.findById(command.teamId)
                val teamName = team?.name ?: "Unknown"

                // Truncate message excerpt to max 100 characters (Req 3.3)
                val messageExcerpt = if (command.content.length > 100) {
                    command.content.take(100)
                } else {
                    command.content
                }

                eventPublisher.publishEvent(
                    PushEvent.ChatMessage(
                        recipientUserIds = recipientIds,
                        senderName = senderName,
                        teamName = teamName,
                        messageExcerpt = messageExcerpt,
                        teamId = command.teamId
                    )
                )
            }
        } catch (e: Exception) {
            // Log but don't fail the message send if push notification fails
            // The message is already saved successfully
        }

        return savedMessage
    }
    
    /**
     * Gets chat history for a team.
     */
    fun getTeamChatHistory(teamId: UUID, limit: Int = 50): List<ChatMessage> {
        return chatMessageRepository.findByTeamIdOrderByCreatedAtDesc(teamId, limit)
    }

    /**
     * Gets chat messages created after the given timestamp for incremental polling.
     * Returns messages ordered by creation date ascending (oldest first).
     *
     * This method supports the polling strategy for real-time chat after
     * the WebSocket/STOMP migration to FCM. The frontend should call this
     * every 5-10 seconds (configurable) when the chat screen is active.
     */
    fun getTeamChatMessagesSince(teamId: UUID, since: LocalDateTime, limit: Int = 50): List<ChatMessage> {
        return chatMessageRepository.findByTeamIdAndCreatedAtAfterOrderByCreatedAtAsc(teamId, since, limit)
    }
}