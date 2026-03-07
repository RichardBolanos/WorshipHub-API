package com.worshiphub.api.scheduling

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * Request DTO for setlist creation.
 */
data class CreateSetlistRequest(
    @field:NotBlank
    val name: String,
    
    @field:NotEmpty
    val songIds: List<UUID>
)