package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

@Schema(description = "Request data for creating a song category or tag")
data class CreateCategoryRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    @Schema(
        description = "Category or tag name", 
        example = "Worship", 
        required = true,
        minLength = 1,
        maxLength = 50
    )
    val name: String,
    
    @Schema(
        description = "ID of the song this category/tag belongs to",
        required = true
    )
    val songId: UUID
)