package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "setlists")
data class SetlistEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    val estimatedDuration: Int?,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)