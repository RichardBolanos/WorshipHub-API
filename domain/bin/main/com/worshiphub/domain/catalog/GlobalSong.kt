package com.worshiphub.domain.catalog

import java.time.LocalDateTime
import java.util.*

/**
 * GlobalSong domain entity for the global song catalog.
 * Pure domain model without persistence annotations.
 * 
 * @property id Unique identifier for the global song
 * @property title Song title
 * @property artist Original artist or composer
 * @property key Default musical key
 * @property bpm Default beats per minute
 * @property chords Official chords in ChordPro format
 * @property isVerified Whether the song is officially verified
 * @property createdAt Timestamp when added to global catalog
 */
data class GlobalSong(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val artist: String,
    val key: String,
    val bpm: Int? = null,
    val chords: String? = null,
    val isVerified: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)