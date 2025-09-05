package com.worshiphub.api.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Generic message response")
data class MessageResponse(
    @Schema(description = "Response message", example = "Operation completed successfully")
    val message: String
)