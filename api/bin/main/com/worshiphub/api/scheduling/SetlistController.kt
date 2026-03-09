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
    ): Map<String, Any> {
        val setlists = schedulingApplicationService.getAllSetlists(churchId)
        return mapOf(
            "content" to setlists.map { setlist ->
                mapOf<String, Any?>(
                    "id" to setlist.id,
                    "name" to setlist.name,
                    "description" to setlist.description,
                    "songIds" to setlist.songIds,
                    "estimatedDuration" to (setlist.estimatedDuration ?: 0),
                    "eventDate" to setlist.eventDate,
                    "createdAt" to setlist.createdAt,
                    "updatedAt" to setlist.updatedAt
                )
            }
        )
    }
    
    @Operation(
        summary = "Get setlist by ID",
        description = "Retrieves a specific setlist"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun getSetlistById(
        @PathVariable id: UUID,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, Any?> {
        val setlist = schedulingApplicationService.getSetlistById(id, churchId)
        return mapOf(
            "id" to setlist.id,
            "name" to setlist.name,
            "description" to setlist.description,
            "songIds" to setlist.songIds,
            "estimatedDuration" to (setlist.estimatedDuration ?: 0),
            "eventDate" to setlist.eventDate,
            "createdAt" to setlist.createdAt,
            "updatedAt" to setlist.updatedAt
        )
    }
    
    @Operation(
        summary = "Update setlist",
        description = "Updates an existing setlist"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun updateSetlist(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, String> {
        schedulingApplicationService.updateSetlist(id, request.name, request.description, request.songIds, request.estimatedDuration, churchId)
        return mapOf("message" to "Setlist updated successfully")
    }
    
    @Operation(
        summary = "Delete setlist",
        description = "Deletes a setlist"
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun deleteSetlist(
        @PathVariable id: UUID,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ) {
        schedulingApplicationService.deleteSetlist(id, churchId)
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
        @Valid @RequestBody request: AddSongToSetlistRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, String> {
        schedulingApplicationService.addSongToSetlist(id, request.songId, request.position ?: -1)
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
        @PathVariable songId: UUID,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ) {
        schedulingApplicationService.removeSongFromSetlist(id, songId)
    }
}

data class AddSongToSetlistRequest(
    val songId: UUID,
    val position: Int? = null
)

data class UpdateSetlistRequest(
    val name: String,
    val description: String?,
    val songIds: List<UUID>,
    val estimatedDuration: Double
)