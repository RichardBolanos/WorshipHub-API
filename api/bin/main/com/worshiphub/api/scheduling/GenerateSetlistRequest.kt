package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.SetlistRules
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for setlist auto-generation.
 */
data class GenerateSetlistRequest(
    @field:NotBlank
    val name: String,
    
    @field:Valid
    val rules: SetlistRules = SetlistRules()
)