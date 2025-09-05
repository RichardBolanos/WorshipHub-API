package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*

@Schema(description = "Request data for creating a new song in the church catalog")
data class CreateSongRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    @Schema(
        description = "Song title", 
        example = "Amazing Grace", 
        required = true,
        minLength = 1,
        maxLength = 200
    )
    val title: String,
    
    @field:NotBlank(message = "Artist is required")
    @field:Size(min = 1, max = 100, message = "Artist name must be between 1 and 100 characters")
    @Schema(
        description = "Artist or composer name", 
        example = "John Newton", 
        required = true,
        minLength = 1,
        maxLength = 100
    )
    val artist: String,
    
    @field:NotBlank(message = "Key is required")
    @field:Pattern(regexp = "^[A-G][#b]?(m|maj|min|sus2|sus4|add9|7|maj7|min7|dim|aug)?$", 
                  message = "Invalid musical key format (e.g., C, Dm, G7, F#maj)")
    @Schema(
        description = "Musical key of the song", 
        example = "G", 
        required = true,
        pattern = "^[A-G][#b]?(m|maj|min|sus2|sus4|add9|7|maj7|min7|dim|aug)?$"
    )
    val key: String,
    
    @field:Min(value = 60, message = "BPM must be at least 60")
    @field:Max(value = 200, message = "BPM must not exceed 200")
    @Schema(
        description = "Beats per minute (tempo) of the song", 
        example = "120",
        minimum = "60",
        maximum = "200"
    )
    val bpm: Int?,
    
    @field:Size(max = 10000, message = "Chords content too long (max 10000 characters)")
    @Schema(
        description = "Song chords in ChordPro format", 
        example = "[G]Amazing [C]grace how [G]sweet the [D]sound\n[G]That saved a [C]wretch like [G]me[D][G]",
        maxLength = 10000
    )
    val chords: String?
)