package com.worshiphub.api.scheduling

import com.worshiphub.application.scheduling.SchedulingApplicationService
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

@Tag(name = "Setlist Management", description = "Advanced setlist song management operations")
@RestController
@RequestMapping("/api/v1/services/setlists")
class SetlistManagementController(
    private val schedulingApplicationService: SchedulingApplicationService
) {
    
    @Operation(
        summary = "Add song to setlist",
        description = "Adds a song to an existing setlist at a specific position"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Song added to setlist successfully"),
        ApiResponse(responseCode = "404", description = "Setlist or song not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PostMapping("/{setlistId}/songs")
    @ResponseStatus(HttpStatus.CREATED)
    fun addSongToSetlist(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID,
        @Valid @RequestBody request: Map<String, Any>
    ): Map<String, String> {
        val songId = UUID.fromString(request["songId"].toString())
        val position = request["position"] as? Int ?: 0
        
        schedulingApplicationService.addSongToSetlist(setlistId, songId, position)
        return mapOf("message" to "Song added to setlist successfully")
    }
    
    @Operation(
        summary = "Remove song from setlist",
        description = "Removes a song from a setlist"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Song removed from setlist successfully"),
        ApiResponse(responseCode = "404", description = "Setlist or song not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @DeleteMapping("/{setlistId}/songs/{songId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeSongFromSetlist(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID,
        @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID
    ) {
        schedulingApplicationService.removeSongFromSetlist(setlistId, songId)
    }
    
    @Operation(
        summary = "Reorder setlist songs",
        description = "Changes the order of songs in a setlist"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Setlist reordered successfully"),
        ApiResponse(responseCode = "404", description = "Setlist not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PreAuthorize("hasRole('CHURCH_ADMIN') or hasRole('WORSHIP_LEADER')")
    @PatchMapping("/{setlistId}/songs/reorder")
    fun reorderSetlistSongs(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID,
        @Valid @RequestBody request: Map<String, List<UUID>>
    ): Map<String, String> {
        val songOrder = request["songOrder"] ?: emptyList()
        schedulingApplicationService.reorderSetlistSongs(setlistId, songOrder)
        return mapOf("message" to "Setlist reordered successfully")
    }
    
    @Operation(
        summary = "Get setlist details",
        description = "Retrieves detailed information about a setlist including all songs"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Setlist details retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Setlist not found")
    ])
    @GetMapping("/{setlistId}")
    fun getSetlistDetails(
        @Parameter(description = "Setlist ID", required = true) @PathVariable setlistId: UUID
    ): Map<String, Any> {
        val setlist = schedulingApplicationService.getSetlistDetails(setlistId)
        return mapOf(
            "id" to (setlist["id"] ?: ""),
            "name" to (setlist["name"] ?: ""),
            "songs" to (setlist["songs"] ?: emptyList<String>()),
            "totalDuration" to (setlist["totalDuration"] ?: 0),
            "createdAt" to setlist.getOrDefault("createdAt", "2024-01-01")
        )
    }
}