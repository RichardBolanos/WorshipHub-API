package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.RegisterChurchCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import com.worshiphub.api.common.BadRequestException
import com.worshiphub.api.common.NotFoundException
import com.worshiphub.api.common.ConflictException
import java.util.*

@Tag(name = "Churches", description = "Church registration and management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/churches")
class ChurchController(
    private val organizationApplicationService: OrganizationApplicationService
) {
    
    @Operation(
        summary = "Register a new church",
        description = "Creates a new church organization in the platform. This is typically the first step for church administrators."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Church successfully registered",
                   content = [Content(schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "409", description = "Church with this email already exists")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registerChurch(@Valid @RequestBody request: RegisterChurchRequest): Map<String, Any> {
        val command = RegisterChurchCommand(
            name = request.name,
            address = request.address,
            email = request.email
        )
        
        val result = organizationApplicationService.registerChurch(command)
        return if (result.isSuccess) {
            mapOf("churchId" to result.getOrThrow())
        } else {
            throw ConflictException(result.exceptionOrNull()?.message ?: "Failed to register church")
        }
    }
    
    @Operation(
        summary = "Get church details",
        description = "Retrieves church information by ID",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Church details retrieved successfully",
                   content = [Content(schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "404", description = "Church not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getChurch(
        @Parameter(description = "Church ID", required = true) @PathVariable id: UUID
    ): Map<String, Any> {
        val result = organizationApplicationService.getChurch(id)
        return if (result.isSuccess) {
            val church = result.getOrThrow()
            mapOf(
                "id" to church.id,
                "name" to church.name,
                "address" to church.address,
                "email" to church.email
            )
        } else {
            throw NotFoundException(result.exceptionOrNull()?.message ?: "Church not found")
        }
    }
}