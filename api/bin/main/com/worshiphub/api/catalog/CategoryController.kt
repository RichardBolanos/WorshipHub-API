package com.worshiphub.api.catalog

import com.worshiphub.application.catalog.CatalogApplicationService
import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.Tag
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag as SwaggerTag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@SwaggerTag(name = "Categories & Tags", description = "Song categorization and tagging operations")
@RestController
@RequestMapping("/api/v1")
class CategoryController(
    private val catalogApplicationService: CatalogApplicationService,
    private val securityContext: SecurityContext
) {
    
    @Operation(summary = "Get all categories", description = "Retrieves all categories for the church")
    @GetMapping("/categories")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getAllCategories(): List<CategoryResponse> {
        val churchId = securityContext.getCurrentChurchId()
        return catalogApplicationService.getAllCategories(churchId).map { 
            CategoryResponse(it.id, it.name, it.description)
        }
    }
    
    @Operation(summary = "Create category", description = "Creates a new category for organizing songs")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Category created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid category data")
    ])
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createCategory(@Valid @RequestBody request: CreateCategoryTagRequest): CategoryResponse {
        val churchId = securityContext.getCurrentChurchId()
        val category = Category(
            name = request.name,
            description = request.description,
            churchId = churchId
        )
        val saved = catalogApplicationService.createCategory(category)
        return CategoryResponse(saved.id, saved.name, saved.description)
    }
    
    @Operation(summary = "Update category")
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun updateCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateCategoryTagRequest
    ): CategoryResponse {
        val churchId = securityContext.getCurrentChurchId()
        val category = Category(id = id, name = request.name, description = request.description, churchId = churchId)
        val updated = catalogApplicationService.updateCategory(category)
        return CategoryResponse(updated.id, updated.name, updated.description)
    }
    
    @Operation(summary = "Delete category")
    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun deleteCategory(@PathVariable id: UUID) {
        catalogApplicationService.deleteCategory(id)
    }
    
    @Operation(summary = "Get all tags", description = "Retrieves all tags for the church")
    @GetMapping("/tags")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getAllTags(): List<TagResponse> {
        val churchId = securityContext.getCurrentChurchId()
        return catalogApplicationService.getAllTags(churchId).map { 
            TagResponse(it.id, it.name, it.color)
        }
    }
    
    @Operation(summary = "Create tag", description = "Creates a new tag for labeling songs")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Tag created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid tag data")
    ])
    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createTag(@Valid @RequestBody request: CreateCategoryTagRequest): TagResponse {
        val churchId = securityContext.getCurrentChurchId()
        val tag = Tag(name = request.name, color = request.color, churchId = churchId)
        val saved = catalogApplicationService.createTag(tag)
        return TagResponse(saved.id, saved.name, saved.color)
    }
    
    @Operation(summary = "Update tag")
    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun updateTag(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateCategoryTagRequest
    ): TagResponse {
        val churchId = securityContext.getCurrentChurchId()
        val tag = Tag(id = id, name = request.name, color = request.color, churchId = churchId)
        val updated = catalogApplicationService.updateTag(tag)
        return TagResponse(updated.id, updated.name, updated.color)
    }
    
    @Operation(summary = "Delete tag")
    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun deleteTag(@PathVariable id: UUID) {
        catalogApplicationService.deleteTag(id)
    }
    
    @Operation(summary = "Assign categories to song")
    @PostMapping("/songs/{songId}/categories")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun assignCategoriesToSong(
        @PathVariable songId: UUID,
        @RequestBody categoryIds: List<UUID>
    ): Map<String, String> {
        catalogApplicationService.assignCategoriesToSong(songId, categoryIds)
        return mapOf("message" to "Categories assigned successfully")
    }
    
    @Operation(summary = "Assign tags to song")
    @PostMapping("/songs/{songId}/tags")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun assignTagsToSong(
        @PathVariable songId: UUID,
        @RequestBody tagIds: List<UUID>
    ): Map<String, String> {
        catalogApplicationService.assignTagsToSong(songId, tagIds)
        return mapOf("message" to "Tags assigned successfully")
    }
}