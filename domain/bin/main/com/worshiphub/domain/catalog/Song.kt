package com.worshiphub.domain.catalog

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Song aggregate root representing a worship song in the catalog. Contains business logic for
 * musical operations and catalog management.
 *
 * @property id Unique identifier for the song
 * @property title Song title
 * @property artist Original artist or composer
 * @property key Musical key (e.g., "C", "G", "Am")
 * @property bpm Beats per minute (tempo)
 * @property chords Song chords in ChordPro format
 * @property duration Estimated duration in minutes
 * @property categories List of categories this song belongs to
 * @property tags List of tags for filtering
 * @property attachments List of related resources
 * @property churchId Reference to the church that owns this song
 * @property createdAt Timestamp when the song was added
 */
@Entity
@Table(name = "songs")
data class Song(
        @Id @GeneratedValue(strategy = GenerationType.UUID) val id: UUID = UUID.randomUUID(),
        @Column(nullable = false, length = 200) val title: String,
        @Column(nullable = true, length = 100) val artist: String?,
        @Column(name = "song_key", nullable = true, length = 10) val key: String?,
        @Column val bpm: Int? = null,
        @Column(columnDefinition = "TEXT") val chords: String? = null,
        @Column(columnDefinition = "TEXT") val lyrics: String? = null,
        @Column val duration: Int? = null, // Duration in minutes
        @OneToMany(mappedBy = "songId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
        val categories: List<Category> = emptyList(),
        @OneToMany(mappedBy = "songId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
        val tags: List<Tag> = emptyList(),
        @OneToMany(mappedBy = "songId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
        val attachments: List<Attachment> = emptyList(),
        @Column(nullable = false) val churchId: UUID,
        @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now()
) {

    /** Transposes the song to a different musical key. */
    fun transpose(toKey: String): Song {
        require(toKey.isNotBlank()) { "Target key cannot be blank" }
        val transposedChords = chords?.let { ChordTransposer.transpose(it, key ?: "", toKey) }
        return copy(key = toKey, chords = transposedChords)
    }

    /** Updates the song duration. */
    fun updateDuration(durationMinutes: Int): Song {
        require(durationMinutes > 0) { "Duration must be positive" }
        return copy(duration = durationMinutes)
    }

    /** Adds a category to the song. */
    fun addCategory(category: Category): Song {
        val updatedCategories = categories + category
        return copy(categories = updatedCategories)
    }

    /** Adds a tag to the song. */
    fun addTag(tag: Tag): Song {
        val updatedTags = tags + tag
        return copy(tags = updatedTags)
    }

    /** Adds an attachment to the song. */
    fun addAttachment(attachment: Attachment): Song {
        val updatedAttachments = attachments + attachment
        return copy(attachments = updatedAttachments)
    }

    /** Checks if song belongs to a specific category. */
    fun hasCategory(categoryName: String): Boolean {
        return categories.any { it.name.equals(categoryName, ignoreCase = true) }
    }

    /** Checks if song has a specific tag. */
    fun hasTag(tagName: String): Boolean {
        return tags.any { it.name.equals(tagName, ignoreCase = true) }
    }
}
