package com.worshiphub.application.chat

import com.worshiphub.domain.collaboration.ChatMessage
import com.worshiphub.domain.collaboration.repository.ChatMessageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for chat operations.
 */
@Service
@Transactional
open class ChatApplicationService(
    private val chatMessageRepository: ChatMessageRepository
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
        
        return chatMessageRepository.save(message)
    }
    
    /**
     * Gets chat history for a team.
     */
    fun getTeamChatHistory(teamId: UUID, limit: Int = 50): List<ChatMessage> {
        return chatMessageRepository.findByTeamIdOrderByCreatedAtDesc(teamId, limit)
    }
}