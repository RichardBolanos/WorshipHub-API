package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.Notification
import com.worshiphub.domain.collaboration.NotificationType
import com.worshiphub.domain.collaboration.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for notification operations.
 */
@Service
open class NotificationApplicationService(
    private val notificationRepository: NotificationRepository
) {
    
    /**
     * Sends a notification to a user.
     */
    @Transactional
    fun sendNotification(userId: UUID, title: String, message: String, type: NotificationType): Result<UUID> {
        return try {
            val notification = Notification(
                userId = userId,
                title = title,
                message = message,
                type = type
            )
            
            val savedNotification = notificationRepository.save(notification)
            Result.success(savedNotification.id)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to send notification", e))
        }
    }
    
    /**
     * Gets notifications for a user.
     */
    fun getUserNotifications(userId: UUID): Result<List<Notification>> {
        return try {
            val notifications = notificationRepository.findByUserId(userId)
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to get notifications", e))
        }
    }
    
    /**
     * Marks a notification as read.
     */
    @Transactional
    fun markAsRead(notificationId: UUID): Result<Unit> {
        return try {
            val notification = notificationRepository.findById(notificationId)
                ?: return Result.failure(RuntimeException("Notification not found: $notificationId"))
            
            val updatedNotification = notification.copy(isRead = true)
            notificationRepository.save(updatedNotification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to mark notification as read", e))
        }
    }
}