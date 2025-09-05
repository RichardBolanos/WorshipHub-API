package com.worshiphub.api.common

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PageRequest(
    @field:Min(0, message = "Page number must be non-negative")
    val page: Int = 0,
    
    @field:Min(1, message = "Page size must be at least 1")
    @field:Max(100, message = "Page size must not exceed 100")
    val size: Int = 20,
    
    val sort: String? = null
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)