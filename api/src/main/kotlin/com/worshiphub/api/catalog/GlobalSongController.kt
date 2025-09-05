package com.worshiphub.api.catalog

import com.worshiphub.application.catalog.CatalogApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Global Song Catalog", description = "Global song catalog search and import operations")
@RestController
@RequestMapping("/api/v1/global-songs")
class GlobalSongController(
    private val catalogApplicationService: CatalogApplicationService
) {
    
    @Operation(
        summary = "Search global song catalog",
        description = "Searches the global catalog of verified songs available for import"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        ApiResponse(responseCode = "400", description = "Invalid search query")
    ])
    @GetMapping("/search")
    fun searchGlobalSongs(
        @Parameter(description = "Search query (title or artist)", required = true) @RequestParam query: String
    ): List<Map<String, Any>> {
        val songs = catalogApplicationService.searchGlobalSongs(query)
        return songs.map { song ->
            mapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "key" to song.key,
                "isVerified" to song.isVerified
            )
        }
    }
    
    @Operation(
        summary = "Import song from global catalog",
        description = "Imports a verified song from the global catalog to the church's local catalog"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Song imported successfully"),
        ApiResponse(responseCode = "404", description = "Global song or church not found"),
        ApiResponse(responseCode = "409", description = "Song already exists in church catalog")
    ])
    @PostMapping("/{globalSongId}/import")
    @ResponseStatus(HttpStatus.CREATED)
    fun importSong(
        @Parameter(description = "Global song ID", required = true) @PathVariable globalSongId: UUID,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, UUID> {
        val songId = catalogApplicationService.importFromGlobal(globalSongId, churchId)
        return mapOf("songId" to songId)
    }
}