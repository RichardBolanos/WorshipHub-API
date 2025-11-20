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
) {
    init {
        require(page >= 0) { "Page number must be non-negative" }
        require(size in 1..100) { "Page size must be between 1 and 100" }
        sort?.let { require(it.length <= 50) { "Sort parameter too long" } }
    }
}

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)