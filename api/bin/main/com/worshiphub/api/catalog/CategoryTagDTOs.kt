package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

@Schema(description = "Request for creating/updating category or tag")
data class CreateCategoryTagRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    @Schema(description = "Name", example = "Worship", required = true)
    val name: String,
    
    @field:Size(max = 200)
    @Schema(description = "Description (categories only)", example = "Songs for worship time")
    val description: String? = null,
    
    @field:Size(min = 7, max = 7)
    @Schema(description = "Color hex code (tags only)", example = "#FF5733")
    val color: String? = null
)

@Schema(description = "Category response")
data class CategoryResponse(
    @Schema(description = "Category ID")
    val id: UUID,
    
    @Schema(description = "Category name")
    val name: String,
    
    @Schema(description = "Category description")
    val description: String? = null
)

@Schema(description = "Tag response")
data class TagResponse(
    @Schema(description = "Tag ID")
    val id: UUID,
    
    @Schema(description = "Tag name")
    val name: String,
    
    @Schema(description = "Tag color")
    val color: String? = null
)
