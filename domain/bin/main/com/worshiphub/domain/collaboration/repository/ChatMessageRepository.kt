package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.ChatMessage
import java.time.LocalDateTime
import java.util.*

interface ChatMessageRepository {
    fun save(message: ChatMessage): ChatMessage
    fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage>
    fun findById(id: UUID): ChatMessage?
    fun findByTeamIdAndCreatedAtAfterOrderByCreatedAtAsc(teamId: UUID, since: LocalDateTime, limit: Int): List<ChatMessage>
}