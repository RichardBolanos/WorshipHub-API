package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.ChatMessage
import java.util.*

interface ChatMessageRepository {
    fun save(message: ChatMessage): ChatMessage
    fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage>
    fun findById(id: UUID): ChatMessage?
}