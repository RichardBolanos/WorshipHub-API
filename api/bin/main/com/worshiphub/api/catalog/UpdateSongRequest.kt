package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class UpdateSongRequest(
        @field:NotBlank(message = "Title is required")
        @field:Size(max = 200, message = "Title must not exceed 200 characters")
        val title: String,
        @field:Size(max = 100, message = "Artist must not exceed 100 characters")
        val artist: String? = null,
        @field:Size(max = 10, message = "Key must not exceed 10 characters")
        val key: String? = null,
        @field:Min(value = 0, message = "BPM must be at least 0")
        @field:Max(value = 200, message = "BPM must not exceed 200")
        val bpm: Int? = null,
        val lyrics: String? = null,
        val chords: String? = null,
        @Schema(description = "List of tag IDs to associate with the song. Null means no change, empty list removes all.")
        val tagIds: List<UUID>? = null,
        @Schema(description = "List of category IDs to associate with the song. Null means no change, empty list removes all.")
        val categoryIds: List<UUID>? = null
)
