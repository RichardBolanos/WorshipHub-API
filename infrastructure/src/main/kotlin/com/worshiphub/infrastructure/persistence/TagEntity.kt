package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "tags")
data class TagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 50)
    val name: String,
    
    @Column(nullable = false)
    val churchId: UUID
)