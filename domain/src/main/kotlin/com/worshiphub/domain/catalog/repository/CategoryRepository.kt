package com.worshiphub.domain.catalog.repository

import com.worshiphub.domain.catalog.Category
import java.util.*

interface CategoryRepository {
    fun save(category: Category): Category
    fun findById(id: UUID): Category?
    fun findByChurchId(churchId: UUID): List<Category>
    fun findBySongId(songId: UUID): List<Category>
    fun delete(category: Category)
}