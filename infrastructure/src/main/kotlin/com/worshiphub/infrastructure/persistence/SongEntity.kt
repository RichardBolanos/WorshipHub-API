package com.worshiphub.infrastructure.persistence

import com.worshiphub.domain.catalog.Song
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "songs")
class SongEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(nullable = false, length = 100)
    val artist: String,
    
    @Column(name = "song_key", nullable = false, length = 10)
    val key: String,
    
    @Column
    val bpm: Int? = null,
    
    @Column(columnDefinition = "TEXT")
    val chords: String? = null,
    
    @Column(nullable = false)
    val churchId: UUID,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        key = key,
        bpm = bpm,
        chords = chords,
        churchId = churchId,
        createdAt = createdAt
    )
    
    companion object {
        fun fromDomain(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            key = song.key,
            bpm = song.bpm,
            chords = song.chords,
            churchId = song.churchId,
            createdAt = song.createdAt
        )
    }
}