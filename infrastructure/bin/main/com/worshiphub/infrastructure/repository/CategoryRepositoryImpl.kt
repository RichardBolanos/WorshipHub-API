package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.repository.CategoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaCategoryRepository : JpaRepository<Category, UUID> {
    fun findByChurchId(churchId: UUID): List<Category>
}

@Repository
open class CategoryRepositoryImpl(
    private val jpaRepository: JpaCategoryRepository
) : CategoryRepository {
    
    override fun save(category: Category): Category = jpaRepository.save(category)
    override fun findById(id: UUID): Category? = jpaRepository.findById(id).orElse(null)
    override fun findByChurchId(churchId: UUID): List<Category> = jpaRepository.findByChurchId(churchId)
    override fun delete(category: Category) = jpaRepository.delete(category)
}