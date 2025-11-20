package com.worshiphub.api.catalog

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Song information response")
data class SongResponse(
    @Schema(description = "Song unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    
    @Schema(description = "Song title", example = "Amazing Grace")
    val title: String,
    
    @Schema(description = "Artist or composer name", example = "John Newton")
    val artist: String,
    
    @Schema(description = "Musical key", example = "G")
    val key: String,
    
    @Schema(description = "Beats per minute", example = "120")
    val bpm: Int?,
    
    @Schema(description = "ChordPro format chords")
    val chords: String?,
    
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime
)

@Schema(description = "Song creation response")
data class CreateSongResponse(
    @Schema(description = "Created song ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val songId: UUID,
    
    @Schema(description = "Success message", example = "Song created successfully")
    val message: String = "Song created successfully"
)

@Schema(description = "Chord transposition response")
data class TransposeChordsResponse(
    @Schema(description = "Transposed chords in ChordPro format")
    val chords: String,
    
    @Schema(description = "Target key", example = "D")
    val targetKey: String
)

@Schema(description = "Song attachment response")
data class AttachmentResponse(
    @Schema(description = "Attachment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val attachmentId: UUID,
    
    @Schema(description = "Attachment name", example = "Lead Sheet PDF")
    val name: String,
    
    @Schema(description = "Attachment URL", example = "https://example.com/sheet.pdf")
    val url: String,
    
    @Schema(description = "Attachment type", example = "PDF")
    val type: String
)

@Schema(description = "Song comment response")
data class CommentResponse(
    @Schema(description = "Comment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,
    
    @Schema(description = "User who made the comment", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,
    
    @Schema(description = "Comment content", example = "Let's play this in D major for Sunday")
    val content: String,
    
    @Schema(description = "Comment creation timestamp")
    val createdAt: LocalDateTime
)