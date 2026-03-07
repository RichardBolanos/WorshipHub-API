package com.worshiphub.application.catalog

import java.util.*

/**
 * Command for creating a category.
 */
data class CreateCategoryCommand(
    val name: String,
    val songId: UUID,
    val churchId: UUID
)