package com.worshiphub.domain.scheduling

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * AssignedMember entity representing a team member's assignment to a service.
 * 
 * @property id Unique identifier for the assignment
 * @property serviceEventId Reference to the service event
 * @property userId Reference to the assigned user
 * @property role Role for this specific service
 * @property confirmationStatus Current status of the member's response
 * @property assignedAt Timestamp when the member was assigned
 * @property respondedAt Timestamp when the member responded (if any)
 */
@Entity
@Table(name = "assigned_members")
data class AssignedMember(
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
    
    @Column
    val respondedAt: LocalDateTime? = null
)

/**
 * Status of a member's response to a service assignment.
 */
enum class ConfirmationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}