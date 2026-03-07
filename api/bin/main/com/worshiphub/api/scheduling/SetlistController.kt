package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.SchedulingApplicationService
import com.worshiphub.application.scheduling.CreateSetlistCommand
import com.worshiphub.api.scheduling.CreateSetlistRequest
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

@Tag(name = "Setlists", description = "Setlist management operations")
@RestController
@RequestMapping("/api/v1/setlists")
class SetlistController(
    private val schedulingApplicationService: SchedulingApplicationService
) {
    
    @Operation(
        summary = "Create a setlist",
        description = "Creates a new setlist with selected songs"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Setlist successfully created"),
        ApiResponse(responseCode = "400", description = "Invalid setlist data")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createSetlist(
        @Valid @RequestBody request: CreateSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, Any> {
        val command = CreateSetlistCommand(
            name = request.name,
            songIds = request.songIds,
            churchId = churchId
        )
        
        val result = schedulingApplicationService.createSetlist(command)
        return if (result.isSuccess) {
            mapOf("setlistId" to result.getOrThrow())
        } else {
            throw RuntimeException(result.exceptionOrNull()?.message ?: "Failed to create setlist")
        }
    }
    
    @Operation(
        summary = "Get setlists",
        description = "Retrieves all setlists for a church"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Setlists retrieved successfully")
    ])
    @GetMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getSetlists(
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): List<Map<String, Any>> {
        // TODO: Implement setlist retrieval
        return listOf(
            mapOf(
                "id" to UUID.randomUUID(),
                "name" to "Sunday Morning Worship",
                "songCount" to 5,
                "totalDuration" to 25
            )
        )
    }
    
    @Operation(
        summary = "Add song to setlist",
        description = "Adds a song to an existing setlist"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Song added successfully"),
        ApiResponse(responseCode = "404", description = "Setlist or song not found")
    ])
    @PostMapping("/{id}/songs")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun addSongToSetlist(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddSongToSetlistRequest
    ): Map<String, String> {
        // TODO: Implement add song to setlist
        return mapOf("message" to "Song added to setlist successfully")
    }
    
    @Operation(
        summary = "Remove song from setlist",
        description = "Removes a song from a setlist"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Song removed successfully"),
        ApiResponse(responseCode = "404", description = "Setlist or song not found")
    ])
    @DeleteMapping("/{id}/songs/{songId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun removeSongFromSetlist(
        @PathVariable id: UUID,
        @PathVariable songId: UUID
    ) {
        // TODO: Implement remove song from setlist
    }
}

data class AddSongToSetlistRequest(
    val songId: UUID,
    val position: Int? = null
)