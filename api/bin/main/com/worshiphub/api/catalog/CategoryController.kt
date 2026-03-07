package com.worshiphub.api.catalog

import com.worshiphub.application.catalog.CatalogApplicationService
import com.worshiphub.application.catalog.CreateCategoryCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Categories & Tags", description = "Song categorization and tagging operations")
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val catalogApplicationService: CatalogApplicationService
) {
    
    @Operation(
        summary = "Create song category",
        description = "Creates a new category for organizing songs (e.g., 'Worship', 'Praise', 'Christmas')"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Category created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid category data"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "409", description = "Category already exists")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createCategory(
        @Valid @RequestBody request: CreateCategoryRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, UUID> {
        val command = CreateCategoryCommand(
            name = request.name,
            songId = request.songId,
            churchId = churchId
        )
        
        val categoryId = catalogApplicationService.createCategory(command)
        return mapOf("categoryId" to categoryId)
    }
    
    @Operation(
        summary = "Create song tag",
        description = "Creates a new tag for labeling songs (e.g., 'Fast', 'Slow', 'Easter', 'Youth')"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Tag created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid tag data"),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "409", description = "Tag already exists")
    ])
    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createTag(
        @Valid @RequestBody request: CreateCategoryRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, UUID> {
        val tagId = catalogApplicationService.createTag(request.name, request.songId, churchId)
        return mapOf("tagId" to tagId)
    }
}