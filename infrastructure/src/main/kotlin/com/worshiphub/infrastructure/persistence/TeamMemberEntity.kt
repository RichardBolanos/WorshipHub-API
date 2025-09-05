package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class TeamRole {
    WORSHIP_LEADER, LEAD_VOCALIST, BACKING_VOCALIST, 
    ACOUSTIC_GUITAR, ELECTRIC_GUITAR, BASS_GUITAR, 
    KEYBOARD, DRUMS, SOUND_ENGINEER
}

@Entity
@Table(name = "team_members")
data class TeamMemberEntity(
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