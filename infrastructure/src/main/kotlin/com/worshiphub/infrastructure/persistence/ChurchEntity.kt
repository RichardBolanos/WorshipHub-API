package com.worshiphub.infrastructure.persistence

import com.worshiphub.domain.organization.Church
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "churches")
data class ChurchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val address: String,
    
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): Church = Church(
        id = id,
        name = name,
        address = address,
        email = email,
        createdAt = createdAt
    )
    
    companion object {
        fun fromDomain(church: Church): ChurchEntity = ChurchEntity(
            id = church.id,
            name = church.name,
            address = church.address,
            email = church.email,
            createdAt = church.createdAt
        )
    }
}