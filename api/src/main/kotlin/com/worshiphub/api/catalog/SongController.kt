package com.worshiphub.api.catalog

import com.worshiphub.application.catalog.CatalogApplicationService
import com.worshiphub.application.catalog.CreateSongCommand
import com.worshiphub.application.catalog.AddAttachmentCommand
import com.worshiphub.application.catalog.AddCommentCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import com.worshiphub.api.common.PageRequest
import com.worshiphub.api.common.PageResponse
import java.util.*

@Tag(name = "Songs", description = "Song catalog management operations")
@RestController
@RequestMapping("/api/v1/songs")
class SongController(
    private val catalogApplicationService: CatalogApplicationService
) {
    
    @Operation(
        summary = "Create a new song",
        description = "Adds a new song to the church's catalog with title, artist, key, BPM, and ChordPro format chords"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Song successfully created"),
        ApiResponse(responseCode = "400", description = "Invalid song data"),
        ApiResponse(responseCode = "404", description = "Church not found")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun createSong(
        @Valid @RequestBody request: CreateSongRequest,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID
    ): Map<String, UUID> {
        val command = CreateSongCommand(
            title = request.title,
            artist = request.artist,
            key = request.key,
            bpm = request.bpm,
            chords = request.chords,
            churchId = churchId
        )
        
        val songId = catalogApplicationService.createSong(command)
        return mapOf("songId" to songId)
    }
    
    @Operation(
        summary = "Transpose song chords",
        description = "Transposes all chords in a song to a different musical key"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Chords successfully transposed"),
        ApiResponse(responseCode = "400", description = "Invalid key specified"),
        ApiResponse(responseCode = "404", description = "Song not found")
    ])
    @PostMapping("/{songId}/transpose")
    fun transposeSong(
        @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
        @Parameter(description = "Target musical key (e.g., C, D, F#)", required = true) @RequestParam toKey: String
    ): Map<String, String> {
        val transposedChords = catalogApplicationService.transposeSong(songId, toKey)
        return mapOf("chords" to transposedChords)
    }
    
    @Operation(
        summary = "Search songs",
        description = "Searches songs by title or artist within a church's catalog"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Church not found")
    ])
    @GetMapping("/search")
    fun searchSongs(
        @Parameter(description = "Search query (title or artist)", required = true) @RequestParam query: String,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): Map<String, Any> {
        val songs = catalogApplicationService.searchSongs(query, churchId)
        val content = songs.map { song ->
            mapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "key" to song.key
            )
        }
        
        return mapOf(
            "content" to content,
            "page" to page,
            "size" to size,
            "totalElements" to content.size,
            "totalPages" to 1,
            "hasNext" to false,
            "hasPrevious" to false
        )
    }
    
    @Operation(
        summary = "Filter songs by category and tags",
        description = "Filters songs by category and/or tags within a church's catalog"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Filtered songs retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Church not found")
    ])
    @GetMapping("/filter")
    fun filterSongs(
        @Parameter(description = "Category ID") @RequestParam(required = false) categoryId: UUID?,
        @Parameter(description = "Tag IDs (comma-separated)") @RequestParam(required = false) tagIds: List<UUID>?,
        @Parameter(description = "Church ID", required = true) @RequestHeader("Church-Id") churchId: UUID,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): Map<String, Any> {
        val songs = catalogApplicationService.filterSongs(categoryId, tagIds ?: emptyList(), churchId)
        val content = songs.map { song ->
            mapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "key" to song.key
            )
        }
        
        return mapOf(
            "content" to content,
            "page" to page,
            "size" to size,
            "totalElements" to content.size,
            "totalPages" to 1,
            "hasNext" to false,
            "hasPrevious" to false
        )
    }
    
    @Operation(
        summary = "Add song attachment",
        description = "Adds a resource attachment to a song (YouTube link, PDF, etc.)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Attachment successfully added"),
        ApiResponse(responseCode = "400", description = "Invalid attachment data"),
        ApiResponse(responseCode = "404", description = "Song not found")
    ])
    @PostMapping("/{songId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun addAttachment(
        @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
        @Valid @RequestBody request: AddAttachmentRequest
    ): Map<String, UUID> {
        val command = AddAttachmentCommand(
            songId = songId,
            name = request.name,
            url = request.url,
            type = request.type
        )
        
        val attachmentId = catalogApplicationService.addAttachment(command)
        return mapOf("attachmentId" to attachmentId)
    }
    
    @Operation(
        summary = "Add song comment",
        description = "Adds a comment to a song for team discussions about arrangements"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Comment successfully added"),
        ApiResponse(responseCode = "400", description = "Invalid comment data"),
        ApiResponse(responseCode = "404", description = "Song not found")
    ])
    @PostMapping("/{songId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
    fun addComment(
        @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
        @Valid @RequestBody request: AddCommentRequest,
        @Parameter(description = "User ID", required = true) @RequestHeader("User-Id") userId: UUID
    ): Map<String, UUID> {
        val command = AddCommentCommand(
            songId = songId,
            userId = userId,
            content = request.content
        )
        
        val commentId = catalogApplicationService.addComment(command)
        return mapOf("commentId" to commentId)
    }
    
    @Operation(
        summary = "Get song comments",
        description = "Retrieves all comments for a song"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Song not found")
    ])
    @GetMapping("/{songId}/comments")
    fun getSongComments(
        @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID
    ): List<Map<String, Any>> {
        val comments = catalogApplicationService.getSongComments(songId)
        return comments.map { comment ->
            mapOf(
                "id" to comment.id,
                "userId" to comment.userId,
                "content" to comment.content,
                "createdAt" to comment.createdAt
            )
        }
    }
}