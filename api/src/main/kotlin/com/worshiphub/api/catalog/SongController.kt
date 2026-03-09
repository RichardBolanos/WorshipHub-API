package com.worshiphub.api.catalog

import com.worshiphub.api.common.BadRequestException
import com.worshiphub.api.common.NotFoundException
import com.worshiphub.api.common.PageResponse
import com.worshiphub.application.catalog.AddAttachmentCommand
import com.worshiphub.application.catalog.AddCommentCommand
import com.worshiphub.application.catalog.CatalogApplicationService
import com.worshiphub.application.catalog.CreateSongCommand
import com.worshiphub.application.catalog.UpdateSongCommand
import com.worshiphub.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Songs", description = "Song catalog management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/songs")
class SongController(
        private val catalogApplicationService: CatalogApplicationService,
        private val securityContext: SecurityContext
) {

        @Operation(
                summary = "Create a new song",
                description =
                        "Adds a new song to the church's catalog with title, artist, key, BPM, and ChordPro format chords",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "201",
                                        description = "Song successfully created",
                                        content =
                                                [
                                                        Content(
                                                                schema =
                                                                        Schema(
                                                                                implementation =
                                                                                        CreateSongResponse::class
                                                                        )
                                                        )]
                                ),
                                ApiResponse(
                                        responseCode = "400",
                                        description =
                                                "Invalid song data - missing required fields or invalid format"
                                ),
                                ApiResponse(
                                        responseCode = "404",
                                        description = "Church not found with provided ID"
                                ),
                                ApiResponse(
                                        responseCode = "403",
                                        description =
                                                "User lacks WORSHIP_LEADER or CHURCH_ADMIN role"
                                ),
                                ApiResponse(
                                        responseCode = "409",
                                        description =
                                                "Song with same title already exists in church catalog"
                                )]
        )
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
        fun createSong(@Valid @RequestBody request: CreateSongRequest): SongResponse {
                val churchId = securityContext.getCurrentChurchId()
                val command =
                        CreateSongCommand(
                                title = request.title,
                                artist = request.artist,
                                key = request.key,
                                bpm = request.bpm,
                                lyrics = request.lyrics,
                                chords = request.chords,
                                churchId = churchId
                        )

                val result = catalogApplicationService.createSong(command)
                return if (result.isSuccess) {
                        val song = result.getOrThrow()
                        SongResponse(
                                id = song.id,
                                title = song.title,
                                artist = song.artist,
                                key = song.key,
                                bpm = song.bpm,
                                chords = song.chords,
                                lyrics = song.lyrics,
                                categories = song.categories.map { CategoryResponse(it.id, it.name, it.description) },
                                tags = song.tags.map { TagResponse(it.id, it.name, it.color) },
                                createdAt = song.createdAt
                        )
                } else {
                        throw BadRequestException(
                                result.exceptionOrNull()?.message ?: "Failed to create song"
                        )
                }
        }

        @Operation(
                summary = "Transpose song chords",
                description = "Transposes all chords in a song to a different musical key",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Chords successfully transposed"
                                ),
                                ApiResponse(
                                        responseCode = "400",
                                        description = "Invalid key specified"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @PostMapping("/{songId}/transpose")
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun transposeSong(
                @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
                @Parameter(description = "Target musical key (e.g., C, D, F#)", required = true)
                @RequestParam
                toKey: String
        ): TransposeChordsResponse {
                val result = catalogApplicationService.transposeSong(songId, toKey)
                return if (result.isSuccess) {
                        TransposeChordsResponse(chords = result.getOrThrow(), targetKey = toKey)
                } else {
                        throw BadRequestException(
                                result.exceptionOrNull()?.message ?: "Failed to transpose song"
                        )
                }
        }

        @Operation(
                summary = "Get all songs",
                description = "Retrieves all songs from a church's catalog with pagination support",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Songs retrieved successfully"
                                ),
                                ApiResponse(responseCode = "404", description = "Church not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @GetMapping
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun getAllSongs(
                @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
                @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
        ): PageResponse<SongResponse> {
                val churchId = securityContext.getCurrentChurchId()
                val result = catalogApplicationService.getAllSongs(churchId, page, size)
                val songs =
                        if (result.isSuccess) {
                                result.getOrThrow()
                        } else {
                                throw NotFoundException(
                                        result.exceptionOrNull()?.message ?: "Failed to get songs"
                                )
                        }
                val content =
                        songs.map { song ->
                                SongResponse(
                                        id = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        key = song.key,
                                        bpm = song.bpm,
                                        chords = song.chords,
                                        lyrics = song.lyrics,
                                        categories = song.categories.map { CategoryResponse(it.id, it.name, it.description) },
                                        tags = song.tags.map { TagResponse(it.id, it.name, it.color) },
                                        createdAt = song.createdAt
                                )
                        }

                return PageResponse(
                        content = content,
                        page = page,
                        size = size,
                        totalElements = content.size.toLong(),
                        totalPages = 1,
                        hasNext = false,
                        hasPrevious = false
                )
        }

        @Operation(
                summary = "Search songs",
                description = "Searches songs by title or artist within a church's catalog",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Search results retrieved successfully"
                                ),
                                ApiResponse(responseCode = "404", description = "Church not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @GetMapping("/search")
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun searchSongs(
                @Parameter(description = "Search query (title or artist)", required = true)
                @RequestParam
                query: String,
                @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
                @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
        ): PageResponse<SongResponse> {
                val churchId = securityContext.getCurrentChurchId()
                val result = catalogApplicationService.searchSongs(query, churchId, page, size)
                val songs =
                        if (result.isSuccess) {
                                result.getOrThrow()
                        } else {
                                throw NotFoundException(
                                        result.exceptionOrNull()?.message
                                                ?: "Failed to search songs"
                                )
                        }
                val content =
                        songs.map { song ->
                                SongResponse(
                                        id = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        key = song.key,
                                        bpm = song.bpm,
                                        chords = song.chords,
                                        lyrics = song.lyrics,
                                        categories = song.categories.map { CategoryResponse(it.id, it.name, it.description) },
                                        tags = song.tags.map { TagResponse(it.id, it.name, it.color) },
                                        createdAt = song.createdAt
                                )
                        }

                return PageResponse(
                        content = content,
                        page = page,
                        size = size,
                        totalElements = content.size.toLong(),
                        totalPages = 1,
                        hasNext = false,
                        hasPrevious = false
                )
        }

        @Operation(
                summary = "Filter songs by category and tags",
                description = "Filters songs by category and/or tags within a church's catalog",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Filtered songs retrieved successfully"
                                ),
                                ApiResponse(responseCode = "404", description = "Church not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @GetMapping("/filter")
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun filterSongs(
                @Parameter(description = "Category ID")
                @RequestParam(required = false)
                categoryId: UUID?,
                @Parameter(description = "Tag IDs (comma-separated)")
                @RequestParam(required = false)
                tagIds: List<UUID>?,
                @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
                @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
        ): PageResponse<SongResponse> {
                val churchId = securityContext.getCurrentChurchId()
                val songs =
                        catalogApplicationService.filterSongs(
                                categoryId,
                                tagIds ?: emptyList(),
                                churchId
                        )
                val content =
                        songs.map { song ->
                                SongResponse(
                                        id = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        key = song.key,
                                        bpm = song.bpm,
                                        chords = song.chords,
                                        lyrics = song.lyrics,
                                        categories = song.categories.map { CategoryResponse(it.id, it.name, it.description) },
                                        tags = song.tags.map { TagResponse(it.id, it.name, it.color) },
                                        createdAt = song.createdAt
                                )
                        }

                return PageResponse(
                        content = content,
                        page = page,
                        size = size,
                        totalElements = content.size.toLong(),
                        totalPages = 1,
                        hasNext = false,
                        hasPrevious = false
                )
        }

        @Operation(
                summary = "Add song attachment",
                description = "Adds a resource attachment to a song (YouTube link, PDF, etc.)",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "201",
                                        description = "Attachment successfully added"
                                ),
                                ApiResponse(
                                        responseCode = "400",
                                        description = "Invalid attachment data"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @PostMapping("/{songId}/attachments")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
        fun addAttachment(
                @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
                @Valid @RequestBody request: AddAttachmentRequest
        ): AttachmentResponse {
                val command =
                        AddAttachmentCommand(
                                songId = songId,
                                name = request.name,
                                url = request.url,
                                type = request.type
                        )

                val attachmentId = catalogApplicationService.addAttachment(command)
                return AttachmentResponse(
                        attachmentId = attachmentId,
                        name = request.name,
                        url = request.url,
                        type = request.type.toString()
                )
        }

        @Operation(
                summary = "Add song comment",
                description = "Adds a comment to a song for team discussions about arrangements",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "201",
                                        description = "Comment successfully added"
                                ),
                                ApiResponse(
                                        responseCode = "400",
                                        description = "Invalid comment data"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @PostMapping("/{songId}/comments")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun addComment(
                @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID,
                @Valid @RequestBody request: AddCommentRequest
        ): Map<String, UUID> {
                val userId = securityContext.getCurrentUserId()
                val command =
                        AddCommentCommand(
                                songId = songId,
                                userId = userId,
                                content = request.content
                        )

                val commentId = catalogApplicationService.addComment(command)
                return mapOf("commentId" to commentId)
        }

        @Operation(
                summary = "Get song comments",
                description = "Retrieves all comments for a song",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Comments retrieved successfully"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @GetMapping("/{songId}/comments")
        @PreAuthorize(
                "hasRole('TEAM_MEMBER') or hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')"
        )
        fun getSongComments(
                @Parameter(description = "Song ID", required = true) @PathVariable songId: UUID
        ): List<CommentResponse> {
                val comments = catalogApplicationService.getSongComments(songId)
                return comments.map { comment ->
                        CommentResponse(
                                id = comment.id,
                                userId = comment.userId,
                                content = comment.content,
                                createdAt = comment.createdAt
                        )
                }
        }

        @Operation(
                summary = "Update a song",
                description = "Updates an existing song in the catalog",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Song successfully updated"
                                ),
                                ApiResponse(
                                        responseCode = "400",
                                        description = "Invalid song data"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
        fun updateSong(
                @PathVariable id: UUID,
                @Valid @RequestBody request: UpdateSongRequest
        ): Map<String, Any> {
                val command =
                        UpdateSongCommand(
                                title = request.title,
                                artist = request.artist,
                                key = request.key,
                                bpm = request.bpm,
                                lyrics = request.lyrics,
                                chords = request.chords
                        )

                val result = catalogApplicationService.updateSong(id, command)
                return if (result.isSuccess) {
                        mapOf("id" to id, "message" to "Song updated successfully")
                } else {
                        throw BadRequestException(
                                result.exceptionOrNull()?.message ?: "Failed to update song"
                        )
                }
        }

        @Operation(
                summary = "Delete a song",
                description = "Removes a song from the catalog",
                security = [SecurityRequirement(name = "bearerAuth")]
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "204",
                                        description = "Song successfully deleted"
                                ),
                                ApiResponse(responseCode = "404", description = "Song not found"),
                                ApiResponse(
                                        responseCode = "403",
                                        description = "Insufficient permissions"
                                )]
        )
        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @PreAuthorize("hasRole('WORSHIP_LEADER') or hasRole('CHURCH_ADMIN')")
        fun deleteSong(@PathVariable id: UUID) {
                val result = catalogApplicationService.deleteSong(id)
                if (result.isFailure) {
                        throw NotFoundException(
                                result.exceptionOrNull()?.message ?: "Failed to delete song"
                        )
                }
        }
}
