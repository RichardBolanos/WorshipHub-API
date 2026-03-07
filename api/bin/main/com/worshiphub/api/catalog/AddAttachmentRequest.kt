package com.worshiphub.api.catalog

import com.worshiphub.domain.catalog.AttachmentType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*

@Schema(description = "Request data for adding a resource attachment to a song")
data class AddAttachmentRequest(
    @field:NotBlank(message = "Attachment name is required")
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Schema(
        description = "Display name for the attachment", 
        example = "Lead Sheet PDF", 
        required = true,
        minLength = 1,
        maxLength = 100
    )
    val name: String,
    
    @field:NotBlank(message = "URL is required")
    @field:Size(max = 500, message = "URL too long")
    @field:Pattern(
        regexp = "^https?://.*$",
        message = "URL must start with http:// or https://"
    )
    @Schema(
        description = "URL to the resource (YouTube, Spotify, PDF, etc.)", 
        example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 
        required = true,
        maxLength = 500,
        pattern = "^https?://.*$"
    )
    val url: String,
    
    @field:NotNull(message = "Attachment type is required")
    @Schema(
        description = "Type of attachment resource", 
        example = "YOUTUBE", 
        required = true,
        allowableValues = ["YOUTUBE", "SPOTIFY", "PDF", "AUDIO", "OTHER"]
    )
    val type: AttachmentType
)