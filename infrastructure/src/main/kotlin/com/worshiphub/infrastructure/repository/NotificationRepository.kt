package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.collaboration.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

// @Repository
// interface NotificationRepository : JpaRepository<Notification, UUID> {
//     fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>
// }