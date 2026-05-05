package com.worshiphub.domain.scheduling

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * ServiceEvent aggregate root representing a scheduled worship service.
 * Contains business logic for team scheduling and service management.
 * 
 * @property id Unique identifier for the service event
 * @property name Name of the service (e.g., "Sunday Morning Service")
 * @property scheduledDate Date and time when the service is scheduled
 * @property teamId Reference to the team assigned to this service
 * @property setlistId Reference to the setlist for this service
 * @property assignedMembers List of team members assigned to this service
 * @property status Current status of the service event
 * @property churchId Reference to the church hosting this service
 * @property createdAt Timestamp when the service event was created
 */
@Entity
@Table(name = "service_events")
data class ServiceEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @Column(nullable = false)
    val scheduledDate: LocalDateTime,
    
    @Column(nullable = false)
    val teamId: UUID,
    
    @Column
    val setlistId: UUID? = null,
    
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "service_event_id")
    val assignedMembers: List<AssignedMember> = emptyList(),
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ServiceEventStatus = ServiceEventStatus.DRAFT,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Embedded
    val recurrenceRule: RecurrenceRule? = null,
    
    @Column
    val parentServiceId: UUID? = null,
) {
    
    /**
     * Assigns a member to this service event.
     */
    fun assignMember(member: AssignedMember): ServiceEvent {
        val updatedMembers = assignedMembers + member
        return copy(assignedMembers = updatedMembers)
    }
    
    /**
     * Removes a member assignment from this service.
     */
    fun removeMemberAssignment(memberId: UUID): ServiceEvent {
        val updatedMembers = assignedMembers.filterNot { it.userId == memberId }
        return copy(assignedMembers = updatedMembers)
    }
    
    /**
     * Assigns a setlist to this service.
     */
    fun assignSetlist(setlistId: UUID): ServiceEvent {
        return copy(setlistId = setlistId)
    }
    
    /**
     * Publishes the service event (makes it visible to team members).
     */
    fun publish(): ServiceEvent {
        require(status == ServiceEventStatus.DRAFT) { "Only draft services can be published" }
        return copy(status = ServiceEventStatus.PUBLISHED)
    }
    
    /**
     * Confirms the service event (finalizes the schedule).
     */
    fun confirm(): ServiceEvent {
        require(status == ServiceEventStatus.PUBLISHED) { "Only published services can be confirmed" }
        return copy(status = ServiceEventStatus.CONFIRMED)
    }
    
    /**
     * Cancels the service event.
     */
    fun cancel(): ServiceEvent {
        require(status != ServiceEventStatus.CANCELLED) { "Service is already cancelled" }
        return copy(status = ServiceEventStatus.CANCELLED)
    }
    
    /**
     * Gets all confirmed members for this service.
     */
    fun getConfirmedMembers(): List<AssignedMember> {
        return assignedMembers.filter { it.confirmationStatus == ConfirmationStatus.ACCEPTED }
    }
    
    /**
     * Gets all pending member responses.
     */
    fun getPendingMembers(): List<AssignedMember> {
        return assignedMembers.filter { it.confirmationStatus == ConfirmationStatus.PENDING }
    }
    
    /**
     * Checks if the service is ready (has minimum required confirmations).
     */
    fun isReady(): Boolean {
        val confirmedCount = getConfirmedMembers().size
        return confirmedCount >= 3 // Minimum team size
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceEvent) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ServiceEvent(id=$id, name='$name', scheduledDate=$scheduledDate, status=$status)"
    }
}

/**
 * Status of a service event in its lifecycle.
 */
enum class ServiceEventStatus {
    DRAFT,      // Being prepared
    PUBLISHED,  // Visible to team members
    CONFIRMED,  // Finalized and ready
    CANCELLED   // Cancelled
}