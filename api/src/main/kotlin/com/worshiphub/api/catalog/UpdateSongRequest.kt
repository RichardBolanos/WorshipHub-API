package com.worshiphub.api.catalog

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class UpdateSongRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title must not exceed 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Artist is required")
    @field:Size(max = 100, message = "Artist must not exceed 100 characters")
    val artist: String,
    
    @field:NotBlank(message = "Key is required")
    @field:Size(max = 10, message = "Key must not exceed 10 characters")
    val key: String,
    
    @field:Min(value = 60, message = "BPM must be at least 60")
    @field:Max(value = 200, message = "BPM must not exceed 200")
    val bpm: Int? = null,
    
    val chords: String? = null
)