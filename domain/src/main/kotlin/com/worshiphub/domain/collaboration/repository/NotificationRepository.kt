package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.Notification
import java.util.*

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: UUID): Notification?
    fun findByUserId(userId: UUID): List<Notification>
    fun delete(notification: Notification)
}