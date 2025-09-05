package com.worshiphub.api.organization

import com.worshiphub.application.organization.OrganizationApplicationService
import com.worshiphub.application.organization.RegisterChurchCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import java.util.*

@Tag(name = "Churches", description = "Church registration and management operations")
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
        ApiResponse(responseCode = "201", description = "Church successfully registered"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "409", description = "Church with this email already exists")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun registerChurch(@Valid @RequestBody request: RegisterChurchRequest): Map<String, UUID> {
        val command = RegisterChurchCommand(
            name = request.name,
            address = request.address,
            email = request.email
        )
        
        val churchId = organizationApplicationService.registerChurch(command)
        return mapOf("churchId" to churchId)
    }
}