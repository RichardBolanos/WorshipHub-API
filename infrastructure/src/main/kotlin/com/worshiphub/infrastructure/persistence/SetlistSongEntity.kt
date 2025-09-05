package com.worshiphub.infrastructure.persistence

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "setlist_songs")
@IdClass(SetlistSongId::class)
data class SetlistSongEntity(
    @Id
    val setlistId: UUID,
    
    @Id
    val songOrder: Int,
    
    val songId: UUID?
)

data class SetlistSongId(
    val setlistId: UUID = UUID.randomUUID(),
    val songOrder: Int = 0
) : java.io.Serializable