package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.util.*

@Schema(description = "Request data for creating a new song in the church catalog")
data class CreateSongRequest(
        @field:NotBlank(message = "Title is required")
        @field:Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        @Schema(description = "Song title", example = "Amazing Grace", required = true)
        val title: String,
        @field:Size(
                min = 1,
                max = 100,
                message = "Artist name must be between 1 and 100 characters"
        )
        @Schema(description = "Artist or composer name", example = "John Newton")
        val artist: String? = null,
        @field:Pattern(
                regexp = "^[A-G][#b]?(m|maj|min|sus2|sus4|add9|7|maj7|min7|dim|aug)?\$",
                message = "Invalid musical key format (e.g., C, Dm, G7, F#maj)"
        )
        @Schema(description = "Musical key of the song", example = "G")
        val key: String? = null,
        @field:Min(value = 0, message = "BPM must be at least 0")
        @field:Max(value = 200, message = "BPM must not exceed 200")
        @Schema(description = "Beats per minute (tempo) of the song", example = "120")
        val bpm: Int? = null,
        @field:Size(max = 50000, message = "Lyrics content too long (max 50000 characters)")
        @Schema(description = "Full song lyrics")
        val lyrics: String? = null,
        @field:Size(max = 10000, message = "Chords content too long (max 10000 characters)")
        @Schema(description = "Song chords in ChordPro format")
        val chords: String? = null,
        @Schema(description = "List of tag IDs to associate with the song")
        val tagIds: List<UUID>? = null,
        @Schema(description = "List of category IDs to associate with the song")
        val categoryIds: List<UUID>? = null
)
