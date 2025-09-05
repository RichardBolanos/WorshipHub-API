package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class ConfirmationStatus {
    PENDING, ACCEPTED, DECLINED
}

@Entity
@Table(name = "assigned_members")
data class AssignedMemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val serviceEventId: UUID,
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, length = 50)
    val role: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val confirmationStatus: ConfirmationStatus = ConfirmationStatus.PENDING,
    
    @Column(nullable = false)
    val assignedAt: LocalDateTime = LocalDateTime.now(),
    
    val respondedAt: LocalDateTime?
)