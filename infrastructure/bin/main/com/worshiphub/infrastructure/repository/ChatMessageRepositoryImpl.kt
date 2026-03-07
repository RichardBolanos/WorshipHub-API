package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.ChatMessage
import com.worshiphub.domain.collaboration.repository.ChatMessageRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    @Query("SELECT c FROM ChatMessage c WHERE c.teamId = :teamId ORDER BY c.createdAt DESC LIMIT :limit")
    fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage>
}

@Repository
open class ChatMessageRepositoryImpl(
    private val jpaRepository: JpaChatMessageRepository
) : ChatMessageRepository {
    
    override fun save(message: ChatMessage): ChatMessage = jpaRepository.save(message)
    
    override fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage> =
        jpaRepository.findByTeamIdOrderByCreatedAtDesc(teamId, limit)
    
    override fun findById(id: UUID): ChatMessage? = 
        jpaRepository.findById(id).orElse(null)
}