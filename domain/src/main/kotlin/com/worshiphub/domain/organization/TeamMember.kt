package com.worshiphub.domain.organization

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * TeamMember entity representing a user's membership and role within a specific team.
 * 
 * @property id Unique identifier for the team membership
 * @property teamId Reference to the team
 * @property userId Reference to the user
 * @property teamRole Specific role within this team
 * @property joinedAt Timestamp when the user joined the team
 */
@Entity
@Table(name = "team_members")
data class TeamMember(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val teamId: UUID,
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val teamRole: TeamRole,
    
    @Column(nullable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Specific roles within a worship team.
 */
enum class TeamRole {
    LEAD_VOCALIST,
    BACKING_VOCALIST,
    ACOUSTIC_GUITAR,
    ELECTRIC_GUITAR,
    BASS_GUITAR,
    DRUMS,
    KEYBOARD,
    SOUND_ENGINEER,
    WORSHIP_LEADER
}