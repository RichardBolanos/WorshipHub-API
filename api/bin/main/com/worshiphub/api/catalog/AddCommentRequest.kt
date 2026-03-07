package com.worshiphub.api.catalog

import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for adding song comments.
 */
data class AddCommentRequest(
    @field:NotBlank
    val content: String
)