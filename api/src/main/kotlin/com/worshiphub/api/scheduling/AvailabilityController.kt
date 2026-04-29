package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.DeleteAvailabilityCommand
import com.worshiphub.application.scheduling.GetMyAvailabilityCommand
import com.worshiphub.application.scheduling.MarkUnavailabilityCommand
import com.worshiphub.application.scheduling.SchedulingApplicationService
import com.worshiphub.api.common.ForbiddenException
import com.worshiphub.api.common.NotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@Tag(name = "Availability", description = "User availability management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/services/availability")
@PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
class AvailabilityController(
    private val schedulingApplicationService: SchedulingApplicationService
) {

    @Operation(
        summary = "Delete an unavailability record",
        description = "Deletes a user's unavailability record. Only the owner can delete their own records.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Unavailability record deleted successfully"),
        ApiResponse(responseCode = "404", description = "Unavailability record not found"),
        ApiResponse(responseCode = "403", description = "User not authorized to delete this record")
    ])
    @DeleteMapping("/{availabilityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAvailability(
        @Parameter(description = "Availability record ID", required = true) @PathVariable availabilityId: UUID,
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ) {
        val command = DeleteAvailabilityCommand(
            availabilityId = availabilityId,
            userId = userId
        )

        val result = schedulingApplicationService.deleteAvailability(command)
        if (result.isFailure) {
            when (val exception = result.exceptionOrNull()) {
                is NoSuchElementException -> throw NotFoundException(exception.message ?: "Registro de indisponibilidad no encontrado")
                is SecurityException -> throw ForbiddenException(exception.message ?: "No tiene permiso para eliminar este registro")
                else -> throw RuntimeException(exception?.message ?: "Error al eliminar registro")
            }
        }
    }

    @Operation(
        summary = "Get my unavailability records",
        description = "Returns the current user's unavailability records, optionally filtered by date range, ordered by date ascending.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Unavailability records retrieved successfully",
                   content = [Content(schema = Schema(implementation = AvailabilityDetailResponse::class))])
    ])
    @GetMapping("/me")
    fun getMyAvailability(
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID,
        @Parameter(description = "Start date filter (inclusive, YYYY-MM-DD)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @Parameter(description = "End date filter (inclusive, YYYY-MM-DD)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): List<AvailabilityDetailResponse> {
        val command = GetMyAvailabilityCommand(
            userId = userId,
            startDate = startDate,
            endDate = endDate
        )

        return schedulingApplicationService.getMyAvailability(command).map { availability ->
            AvailabilityDetailResponse(
                id = availability.id,
                unavailableDate = availability.unavailableDate,
                reason = availability.reason,
                createdAt = availability.createdAt
            )
        }
    }

    @Operation(
        summary = "Mark user unavailability",
        description = "Allows team members to mark dates when they are unavailable for services",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Unavailability marked successfully",
                   content = [Content(schema = Schema(implementation = AvailabilityResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid date or request data")
    ])
    @PostMapping("/unavailable")
    @ResponseStatus(HttpStatus.CREATED)
    fun markUnavailability(
        @Valid @RequestBody request: MarkUnavailabilityRequest,
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): AvailabilityResponse {
        val command = MarkUnavailabilityCommand(
            userId = userId,
            unavailableDate = request.unavailableDate,
            reason = request.reason
        )

        val availabilityId = schedulingApplicationService.markUnavailability(command)
        return AvailabilityResponse(availabilityId = availabilityId)
    }
}
