package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.repository.NotificationRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaNotificationRepository : JpaRepository<Notification, UUID> {
    fun findByUserId(userId: UUID): List<Notification>
}

@Repository
open class NotificationRepositoryImpl(
    private val jpaRepository: JpaNotificationRepository
) : NotificationRepository {
    
    override fun save(notification: Notification): Notification = jpaRepository.save(notification)
    override fun findById(id: UUID): Notification? = jpaRepository.findById(id).orElse(null)
    override fun findByUserId(userId: UUID): List<Notification> = jpaRepository.findByUserId(userId)
    override fun delete(notification: Notification) = jpaRepository.delete(notification)
}