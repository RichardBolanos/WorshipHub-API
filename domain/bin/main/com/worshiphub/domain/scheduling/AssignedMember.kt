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
    
    @Column(name = "service_event_id", nullable = false)
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
) {
    
    /**
     * Accepts the service assignment.
     */
    fun accept(): AssignedMember {
        require(confirmationStatus == ConfirmationStatus.PENDING) { "Can only accept pending assignments" }
        return copy(
            confirmationStatus = ConfirmationStatus.ACCEPTED,
            respondedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Declines the service assignment.
     */
    fun decline(): AssignedMember {
        require(confirmationStatus == ConfirmationStatus.PENDING) { "Can only decline pending assignments" }
        return copy(
            confirmationStatus = ConfirmationStatus.DECLINED,
            respondedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Changes the role for this assignment.
     */
    fun changeRole(newRole: String): AssignedMember {
        require(newRole.isNotBlank()) { "Role cannot be blank" }
        return copy(role = newRole)
    }
    
    /**
     * Checks if the member has responded to the assignment.
     */
    fun hasResponded(): Boolean {
        return confirmationStatus != ConfirmationStatus.PENDING
    }
    
    /**
     * Checks if the assignment is confirmed (accepted).
     */
    fun isConfirmed(): Boolean {
        return confirmationStatus == ConfirmationStatus.ACCEPTED
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssignedMember) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "AssignedMember(id=$id, userId=$userId, role='$role', status=$confirmationStatus)"
    }
}

/**
 * Status of a member's response to a service assignment.
 */
enum class ConfirmationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}