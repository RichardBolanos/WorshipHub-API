package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.ChatMessage
import com.worshiphub.domain.collaboration.repository.ChatMessageRepository
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface JpaChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    @Query("SELECT c FROM ChatMessage c WHERE c.teamId = :teamId ORDER BY c.createdAt DESC LIMIT :limit")
    fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage>

    @Query("SELECT c FROM ChatMessage c WHERE c.teamId = :teamId AND c.createdAt > :since ORDER BY c.createdAt ASC LIMIT :limit")
    fun findByTeamIdAndCreatedAtAfterOrderByCreatedAtAsc(teamId: UUID, since: LocalDateTime, limit: Int): List<ChatMessage>
}

@Repository
open class ChatMessageRepositoryImpl(
    private val jpaRepository: JpaChatMessageRepository,
    private val entityManager: EntityManager
) : ChatMessageRepository {
    
    override fun save(message: ChatMessage): ChatMessage {
        return if (jpaRepository.existsById(message.id)) {
            jpaRepository.save(message)
        } else {
            entityManager.persist(message)
            message
        }
    }
    
    override fun findByTeamIdOrderByCreatedAtDesc(teamId: UUID, limit: Int): List<ChatMessage> =
        jpaRepository.findByTeamIdOrderByCreatedAtDesc(teamId, limit)
    
    override fun findById(id: UUID): ChatMessage? = 
        jpaRepository.findById(id).orElse(null)

    override fun findByTeamIdAndCreatedAtAfterOrderByCreatedAtAsc(teamId: UUID, since: LocalDateTime, limit: Int): List<ChatMessage> =
        jpaRepository.findByTeamIdAndCreatedAtAfterOrderByCreatedAtAsc(teamId, since, limit)
}
