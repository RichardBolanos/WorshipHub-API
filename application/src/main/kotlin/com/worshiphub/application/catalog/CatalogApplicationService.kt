package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Attachment
import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.GlobalSong
import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.repository.AttachmentRepository
import com.worshiphub.domain.catalog.repository.CategoryRepository
import com.worshiphub.domain.catalog.repository.GlobalSongRepository
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.domain.catalog.repository.TagRepository
import com.worshiphub.domain.collaboration.SongComment
import com.worshiphub.domain.collaboration.push.PushEvent
import com.worshiphub.domain.collaboration.repository.SongCommentRepository
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import java.time.LocalDateTime
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Application service for catalog operations. */
@Service
open class CatalogApplicationService(
        private val songRepository: SongRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val attachmentRepository: AttachmentRepository,
        private val songCommentRepository: SongCommentRepository,
        private val globalSongRepository: GlobalSongRepository,
        private val eventPublisher: ApplicationEventPublisher,
        private val userRepository: UserRepository,
        private val serviceEventRepository: ServiceEventRepository,
        private val setlistRepository: SetlistRepository
) {
    private val log = LoggerFactory.getLogger(CatalogApplicationService::class.java)

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

            // Resolve tags if provided
            val tags = command.tagIds?.let { ids ->
                val found = ids.mapNotNull { tagRepository.findById(it) }.toSet()
                if (found.size != ids.size) {
                    val foundIds = found.map { it.id }.toSet()
                    val invalidIds = ids.filter { it !in foundIds }
                    return Result.failure(IllegalArgumentException("Invalid tag IDs: $invalidIds"))
                }
                found
            } ?: emptySet()

            // Resolve categories if provided
            val categories = command.categoryIds?.let { ids ->
                val found = ids.mapNotNull { categoryRepository.findById(it) }.toSet()
                if (found.size != ids.size) {
                    val foundIds = found.map { it.id }.toSet()
                    val invalidIds = ids.filter { it !in foundIds }
                    return Result.failure(IllegalArgumentException("Invalid category IDs: $invalidIds"))
                }
                found
            } ?: emptySet()

            val songWithAssociations = song.copy(tags = tags, categories = categories)
            val savedSong = songRepository.save(songWithAssociations)
            
            // Publish push event for new song (Req 7.1)
            try {
                val creatorUser = userRepository.findById(command.createdBy)
                val addedByName = creatorUser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                val activeMembers = userRepository.findByChurchIdAndIsActiveTrue(command.churchId)
                val recipientIds = activeMembers
                    .map { it.id }
                    .filter { it != command.createdBy }
                if (recipientIds.isNotEmpty()) {
                    eventPublisher.publishEvent(
                        PushEvent.NewSong(
                            recipientUserIds = recipientIds,
                            songTitle = savedSong.title,
                            artist = savedSong.artist,
                            addedByName = addedByName,
                            songId = savedSong.id
                        )
                    )
                }
            } catch (e: Exception) {
                log.warn("Failed to publish NewSong push event for song {}: {}", savedSong.id, e.message)
            }
            
            Result.success(savedSong)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to create song", e))
        }
    }

    /** Gets a single song by its ID. */
    @Transactional(readOnly = true)
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

    /** Gets all songs for a church. */
    @Transactional(readOnly = true)
    fun getAllSongs(churchId: UUID, page: Int, size: Int): Result<List<Song>> {
        return try {
            val songs = songRepository.findByChurchId(churchId, page, size)
            log.info("getAllSongs returned {} songs for church {}", songs.size, churchId)
            Result.success(songs)
        } catch (e: Exception) {
            log.error("getAllSongs FAILED for church {}: {} - {}", churchId, e.javaClass.name, e.message, e)
            Result.failure(RuntimeException("Failed to retrieve songs", e))
        }
    }

    /** Searches songs by title or artist. */
    @Transactional(readOnly = true)
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

            // Resolve tags: null = keep existing, empty = remove all, values = resolve
            val tags = command.tagIds?.let { ids ->
                if (ids.isEmpty()) {
                    emptySet()
                } else {
                    val found = ids.mapNotNull { tagRepository.findById(it) }.toSet()
                    if (found.size != ids.size) {
                        val foundIds = found.map { it.id }.toSet()
                        val invalidIds = ids.filter { it !in foundIds }
                        return Result.failure(IllegalArgumentException("Invalid tag IDs: $invalidIds"))
                    }
                    found
                }
            } ?: existingSong.tags

            // Resolve categories: null = keep existing, empty = remove all, values = resolve
            val categories = command.categoryIds?.let { ids ->
                if (ids.isEmpty()) {
                    emptySet()
                } else {
                    val found = ids.mapNotNull { categoryRepository.findById(it) }.toSet()
                    if (found.size != ids.size) {
                        val foundIds = found.map { it.id }.toSet()
                        val invalidIds = ids.filter { it !in foundIds }
                        return Result.failure(IllegalArgumentException("Invalid category IDs: $invalidIds"))
                    }
                    found
                }
            } ?: existingSong.categories

            val updatedSong =
                    existingSong.copy(
                            title = command.title,
                            artist = command.artist ?: existingSong.artist,
                            key = command.key ?: existingSong.key,
                            bpm = command.bpm ?: existingSong.bpm,
                            lyrics = command.lyrics ?: existingSong.lyrics,
                            chords = command.chords ?: existingSong.chords,
                            tags = tags,
                            categories = categories
                    )

            songRepository.save(updatedSong)
            
            // Publish push event for song update (Req 20.1, 20.3)
            try {
                if (command.updatedBy != null) {
                    val changedFields = mutableListOf<String>()
                    if (command.key != null && command.key != existingSong.key) changedFields.add("key")
                    if (command.bpm != null && command.bpm != existingSong.bpm) changedFields.add("bpm")
                    if (command.lyrics != null && command.lyrics != existingSong.lyrics) changedFields.add("lyrics")
                    if (command.chords != null && command.chords != existingSong.chords) changedFields.add("chords")
                    if (command.title != existingSong.title) changedFields.add("title")
                    if (command.artist != null && command.artist != existingSong.artist) changedFields.add("artist")
                    
                    if (changedFields.isNotEmpty()) {
                        val recipientIds = findUsersWithSongInFutureSetlists(songId, existingSong.churchId, command.updatedBy)
                        if (recipientIds.isNotEmpty()) {
                            val updaterUser = userRepository.findById(command.updatedBy)
                            val updatedByName = updaterUser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                            eventPublisher.publishEvent(
                                PushEvent.SongUpdated(
                                    recipientUserIds = recipientIds,
                                    songTitle = updatedSong.title,
                                    changedFields = changedFields,
                                    updatedByName = updatedByName,
                                    songId = songId
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to publish SongUpdated push event for song {}: {}", songId, e.message)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to update song", e))
        }
    }

    /** Deletes a song. */
    @Transactional
    fun deleteSong(songId: UUID, deletedBy: UUID? = null): Result<Unit> {
        return try {
            val song =
                    songRepository.findById(songId)
                            ?: return Result.failure(
                                    IllegalArgumentException("Song not found: $songId")
                            )

            // Collect recipients before deletion (Req 21.1)
            var recipientIds: List<UUID> = emptyList()
            var affectedSetlistNames: List<String> = emptyList()
            var deletedByName = "Unknown"
            if (deletedBy != null) {
                try {
                    recipientIds = findUsersWithSongInAnySetlist(songId, song.churchId, deletedBy)
                    affectedSetlistNames = findSetlistNamesContainingSong(songId, song.churchId)
                    val deleterUser = userRepository.findById(deletedBy)
                    deletedByName = deleterUser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                } catch (e: Exception) {
                    log.warn("Failed to collect SongDeleted push event data for song {}: {}", songId, e.message)
                }
            }

            songRepository.delete(song)
            
            // Publish push event for song deletion (Req 21.1)
            if (deletedBy != null && recipientIds.isNotEmpty()) {
                try {
                    eventPublisher.publishEvent(
                        PushEvent.SongDeleted(
                            recipientUserIds = recipientIds,
                            songTitle = song.title,
                            deletedByName = deletedByName,
                            affectedSetlists = affectedSetlistNames
                        )
                    )
                } catch (e: Exception) {
                    log.warn("Failed to publish SongDeleted push event for song {}: {}", songId, e.message)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to delete song", e))
        }
    }

    @Transactional(readOnly = true)
    fun filterSongs(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song> {
        return songRepository.filterByCategory(categoryId, tagIds, churchId)
    }

    /** Creates a new category. */
    @Transactional
    fun createCategory(category: Category): Category {
        return categoryRepository.save(category)
    }
    
    /** Gets all categories for a church. */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
        val categories = categoryIds.mapNotNull { categoryRepository.findById(it) }.toSet()
        val updated = song.copy(categories = categories)
        songRepository.save(updated)
    }
    
    /** Assigns tags to a song. */
    @Transactional
    fun assignTagsToSong(songId: UUID, tagIds: List<UUID>) {
        val song = songRepository.findById(songId) ?: throw IllegalArgumentException("Song not found")
        val tags = tagIds.mapNotNull { tagRepository.findById(it) }.toSet()
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
        
        // Publish push event for attachment added (Req 22.1)
        if (command.addedBy != null) {
            try {
                val song = songRepository.findById(command.songId)
                if (song != null) {
                    val recipientIds = getSongCreatorAndCommenters(command.songId, command.addedBy)
                    if (recipientIds.isNotEmpty()) {
                        val adderUser = userRepository.findById(command.addedBy)
                        val addedByName = adderUser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                        eventPublisher.publishEvent(
                            PushEvent.AttachmentAdded(
                                recipientUserIds = recipientIds,
                                songTitle = song.title,
                                attachmentType = command.type.name,
                                addedByName = addedByName,
                                songId = command.songId
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to publish AttachmentAdded push event for song {}: {}", command.songId, e.message)
            }
        }
        
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
        
        // Publish push event for song comment (Req 4.1, 4.2)
        try {
            val song = songRepository.findById(command.songId)
            if (song != null) {
                val recipientIds = getSongCreatorAndCommenters(command.songId, command.userId)
                if (recipientIds.isNotEmpty()) {
                    val commenterUser = userRepository.findById(command.userId)
                    val commenterName = commenterUser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                    val commentExcerpt = if (command.content.length > 100) {
                        command.content.take(100) + "..."
                    } else {
                        command.content
                    }
                    eventPublisher.publishEvent(
                        PushEvent.SongComment(
                            recipientUserIds = recipientIds,
                            commenterName = commenterName,
                            songTitle = song.title,
                            commentExcerpt = commentExcerpt,
                            songId = command.songId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to publish SongComment push event for song {}: {}", command.songId, e.message)
        }
        
        return savedComment.id
    }

    /** Gets comments for a song. */
    @Transactional(readOnly = true)
    fun getSongComments(songId: UUID): List<SongComment> {
        return songCommentRepository.findBySongId(songId)
    }

    /**
     * Searches global song catalog by title or artist.
     */
    fun searchGlobalSongs(query: String): List<GlobalSong> {
        return globalSongRepository.searchByTitleOrArtist(query)
    }

    /**
     * Imports a song from global catalog to church catalog.
     * Finds the global song, creates a new Song in the church's catalog based on its data,
     * and returns the new song's ID.
     */
    @Transactional
    fun importFromGlobal(globalSongId: UUID, churchId: UUID): UUID {
        val globalSong = globalSongRepository.findById(globalSongId)
            ?: throw IllegalArgumentException("Global song not found: $globalSongId")

        val song = Song(
            title = globalSong.title,
            artist = globalSong.artist,
            key = globalSong.key,
            bpm = globalSong.bpm,
            chords = globalSong.chords,
            churchId = churchId
        )

        val savedSong = songRepository.save(song)
        return savedSong.id
    }

    // --- Push notification helper methods ---

    /**
     * Finds users who have the given song in setlists associated with future services,
     * excluding the specified actor. Returns deduplicated user IDs.
     * Used for SongUpdated notifications (Req 20.1, 20.3).
     */
    private fun findUsersWithSongInFutureSetlists(songId: UUID, churchId: UUID, excludeUserId: UUID): List<UUID> {
        val allSetlists = setlistRepository.findByChurchId(churchId)
        val setlistsWithSong = allSetlists.filter { songId in it.songIds }
        if (setlistsWithSong.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        val recipientIds = mutableSetOf<UUID>()
        for (setlist in setlistsWithSong) {
            val futureServices = serviceEventRepository.findBySetlistIdAndScheduledDateAfter(setlist.id, now)
            for (service in futureServices) {
                service.assignedMembers.forEach { member ->
                    recipientIds.add(member.userId)
                }
            }
        }
        recipientIds.remove(excludeUserId)
        return recipientIds.toList()
    }

    /**
     * Finds users who have the given song in any setlist,
     * excluding the specified actor. Used for SongDeleted notifications (Req 21.1).
     */
    private fun findUsersWithSongInAnySetlist(songId: UUID, churchId: UUID, excludeUserId: UUID): List<UUID> {
        val allSetlists = setlistRepository.findByChurchId(churchId)
        val setlistsWithSong = allSetlists.filter { songId in it.songIds }
        if (setlistsWithSong.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        val recipientIds = mutableSetOf<UUID>()
        for (setlist in setlistsWithSong) {
            val services = serviceEventRepository.findBySetlistIdAndScheduledDateAfter(setlist.id, now)
            for (service in services) {
                service.assignedMembers.forEach { member ->
                    recipientIds.add(member.userId)
                }
            }
        }
        recipientIds.remove(excludeUserId)
        return recipientIds.toList()
    }

    /**
     * Finds the names of setlists that contain the given song.
     * Used for SongDeleted notifications (Req 21.1).
     */
    private fun findSetlistNamesContainingSong(songId: UUID, churchId: UUID): List<String> {
        val allSetlists = setlistRepository.findByChurchId(churchId)
        return allSetlists.filter { songId in it.songIds }.map { it.name }
    }

    /**
     * Gets all previous commenters for a song, excluding the actor.
     * Used for SongComment (Req 4.1, 4.2) and AttachmentAdded (Req 22.1) notifications.
     */
    private fun getSongCreatorAndCommenters(songId: UUID, excludeUserId: UUID): List<UUID> {
        val comments = songCommentRepository.findBySongId(songId)
        val commenterIds = comments.map { it.userId }.distinct()
        return commenterIds.filter { it != excludeUserId }
    }
}
