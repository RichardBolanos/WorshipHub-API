package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Attachment
import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.ChordTransposer
import com.worshiphub.domain.catalog.GlobalSong
import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import com.worshiphub.domain.catalog.repository.CategoryRepository
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.domain.catalog.repository.TagRepository
import com.worshiphub.domain.collaboration.SongComment
import com.worshiphub.domain.collaboration.repository.SongCommentRepository
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Application service for catalog operations. */
@Service
open class CatalogApplicationService(
        private val songRepository: SongRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val attachmentRepository: AttachmentRepository,
        private val songCommentRepository: SongCommentRepository
) {

    /** Creates a new song in the catalog. */
    @Transactional
    fun createSong(command: CreateSongCommand): Result<Song> {
        return try {
            // Check for duplicates
            if (songRepository.existsByTitleAndArtistAndChurchId(
                            command.title,
                            command.artist,
                            command.churchId
                    )
            ) {
                return Result.failure(
                        IllegalArgumentException("Song with same title and artist already exists")
                )
            }

            val song =
                    Song(
                            title = command.title,
                            artist = command.artist,
                            key = command.key,
                            bpm = command.bpm,
                            lyrics = command.lyrics,
                            chords = command.chords,
                            churchId = command.churchId
                    )

            val savedSong = songRepository.save(song)
            Result.success(savedSong)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create song", e))
        }
    }

    /** Gets a single song by its ID. */
    fun getSongById(songId: UUID): Result<Song> {
        return try {
            val song =
                    songRepository.findById(songId)
                            ?: return Result.failure(
                                    IllegalArgumentException("Song not found: $songId")
                            )
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to retrieve song", e))
        }
    }

    /** Transposes song chords to a different key. */
    fun transposeSong(songId: UUID, toKey: String): Result<String> {
        return try {
            val song =
                    songRepository.findById(songId)
                            ?: return Result.failure(
                                    IllegalArgumentException("Song not found: $songId")
                            )

            val transposed =
                    song.chords?.let { ChordTransposer.transpose(it, song.key ?: "", toKey) } ?: ""

            Result.success(transposed)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to transpose song", e))
        }
    }

    /** Gets all songs for a church. */
    fun getAllSongs(churchId: UUID, page: Int, size: Int): Result<List<Song>> {
        return try {
            val songs = songRepository.findByChurchId(churchId, page, size)
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to retrieve songs", e))
        }
    }

    /** Searches songs by title or artist. */
    fun searchSongs(query: String, churchId: UUID, page: Int, size: Int): Result<List<Song>> {
        return try {
            val songs = songRepository.searchByTitleOrArtist(query, churchId, page, size)
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to search songs", e))
        }
    }

    /** Updates an existing song. */
    @Transactional
    fun updateSong(songId: UUID, command: UpdateSongCommand): Result<Unit> {
        return try {
            val existingSong =
                    songRepository.findById(songId)
                            ?: return Result.failure(
                                    IllegalArgumentException("Song not found: $songId")
                            )

            val updatedSong =
                    existingSong.copy(
                            title = command.title,
                            artist = command.artist ?: existingSong.artist,
                            key = command.key ?: existingSong.key,
                            bpm = command.bpm ?: existingSong.bpm,
                            lyrics = command.lyrics ?: existingSong.lyrics,
                            chords = command.chords ?: existingSong.chords
                    )

            songRepository.save(updatedSong)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to update song", e))
        }
    }

    /** Deletes a song. */
    @Transactional
    fun deleteSong(songId: UUID): Result<Unit> {
        return try {
            val song =
                    songRepository.findById(songId)
                            ?: return Result.failure(
                                    IllegalArgumentException("Song not found: $songId")
                            )

            songRepository.delete(song)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to delete song", e))
        }
    }

    fun filterSongs(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song> {
        return songRepository.filterByCategory(categoryId, tagIds, churchId)
    }

    /** Creates a new category. */
    @Transactional
    fun createCategory(category: Category): Category {
        return categoryRepository.save(category)
    }
    
    /** Gets all categories for a church. */
    fun getAllCategories(churchId: UUID): List<Category> {
        return categoryRepository.findByChurchId(churchId)
    }
    
    /** Updates a category. */
    @Transactional
    fun updateCategory(category: Category): Category {
        return categoryRepository.save(category)
    }
    
    /** Deletes a category. */
    @Transactional
    fun deleteCategory(categoryId: UUID) {
        categoryRepository.findById(categoryId)?.let { categoryRepository.delete(it) }
    }

    /** Creates a new tag. */
    @Transactional
    fun createTag(tag: Tag): Tag {
        return tagRepository.save(tag)
    }
    
    /** Gets all tags for a church. */
    fun getAllTags(churchId: UUID): List<Tag> {
        return tagRepository.findByChurchId(churchId)
    }
    
    /** Updates a tag. */
    @Transactional
    fun updateTag(tag: Tag): Tag {
        return tagRepository.save(tag)
    }
    
    /** Deletes a tag. */
    @Transactional
    fun deleteTag(tagId: UUID) {
        tagRepository.findById(tagId)?.let { tagRepository.delete(it) }
    }
    
    /** Assigns categories to a song. */
    @Transactional
    fun assignCategoriesToSong(songId: UUID, categoryIds: List<UUID>) {
        val song = songRepository.findById(songId) ?: throw IllegalArgumentException("Song not found")
        val categories = categoryIds.mapNotNull { categoryRepository.findById(it) }
        val updated = song.copy(categories = categories)
        songRepository.save(updated)
    }
    
    /** Assigns tags to a song. */
    @Transactional
    fun assignTagsToSong(songId: UUID, tagIds: List<UUID>) {
        val song = songRepository.findById(songId) ?: throw IllegalArgumentException("Song not found")
        val tags = tagIds.mapNotNull { tagRepository.findById(it) }
        val updated = song.copy(tags = tags)
        songRepository.save(updated)
    }

    /** Adds an attachment to a song. */
    @Transactional
    fun addAttachment(command: AddAttachmentCommand): UUID {
        val attachment =
                Attachment(
                        songId = command.songId,
                        name = command.name,
                        url = command.url,
                        type = command.type
                )

        val savedAttachment = attachmentRepository.save(attachment)
        return savedAttachment.id
    }

    /** Adds a comment to a song. */
    @Transactional
    fun addComment(command: AddCommentCommand): UUID {
        val comment =
                SongComment(
                        songId = command.songId,
                        userId = command.userId,
                        content = command.content
                )

        val savedComment = songCommentRepository.save(comment)
        return savedComment.id
    }

    /** Gets comments for a song. */
    fun getSongComments(songId: UUID): List<SongComment> {
        return songCommentRepository.findBySongId(songId)
    }

    /**
     * Searches global song catalog. TODO: Implement actual global catalog search when feature is
     * ready
     */
    fun searchGlobalSongs(query: String): List<GlobalSong> {
        // Global catalog feature not yet implemented
        // Return empty list for now - this is expected behavior
        return emptyList()
    }

    /**
     * Imports a song from global catalog to church catalog. TODO: Implement actual global catalog
     * import when feature is ready
     */
    @Transactional
    fun importFromGlobal(globalSongId: UUID, churchId: UUID): UUID {
        // Global catalog feature not yet implemented
        // For now, throw exception to indicate feature unavailable
        throw UnsupportedOperationException("Global catalog import feature is not yet implemented")
    }
}
