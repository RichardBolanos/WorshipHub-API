package com.worshiphub.domain.organization

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Team aggregate representing a worship team within a church.
 * 
 * @property id Unique identifier for the team
 * @property name Name of the worship team
 * @property description Optional description of the team's purpose
 * @property churchId Reference to the church this team belongs to
 * @property leaderId Reference to the user who leads this team
 * @property createdAt Timestamp when the team was created
 */
@Entity
@Table(name = "teams")
data class Team(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @Column(length = 500)
    val description: String? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val leaderId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)