package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.Category
import com.worshiphub.domain.catalog.Tag
import com.worshiphub.domain.catalog.Attachment
import com.worshiphub.domain.catalog.GlobalSong
import com.worshiphub.domain.catalog.ChordTransposer
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.domain.collaboration.SongComment
// Removed incorrect import - BusinessException should be in domain or create simple exceptions
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Application service for catalog operations.
 */
@Service
@Transactional
open class CatalogApplicationService(
    private val songRepository: SongRepository
) {
    
    /**
     * Creates a new song in the catalog.
     */
    fun createSong(command: CreateSongCommand): UUID {
        // Check for duplicates
        if (songRepository.existsByTitleAndArtistAndChurchId(command.title, command.artist, command.churchId)) {
            throw IllegalArgumentException("Song with same title and artist already exists")
        }
        
        val song = Song(
            title = command.title,
            artist = command.artist,
            key = command.key,
            bpm = command.bpm,
            chords = command.chords,
            churchId = command.churchId
        )
        
        val savedSong = songRepository.save(song)
        return savedSong.id
    }
    
    /**
     * Transposes song chords to a different key.
     */
    fun transposeSong(songId: UUID, toKey: String): String {
        val song = songRepository.findById(songId) 
            ?: throw IllegalArgumentException("Song not found: $songId")
        
        return song.chords?.let { 
            ChordTransposer.transpose(it, song.key, toKey) 
        } ?: ""
    }
    
    /**
     * Searches songs by title or artist.
     */
    fun searchSongs(query: String, churchId: UUID): List<Song> {
        return songRepository.searchByTitleOrArtist(query, churchId)
    }
    
    fun filterSongs(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song> {
        return songRepository.filterByCategory(categoryId, tagIds, churchId)
    }
    
    /**
     * Creates a new category.
     */
    fun createCategory(command: CreateCategoryCommand): UUID {
        val category = Category(
            name = command.name,
            churchId = command.churchId
        )
        
        // TODO: Persist through repository
        return category.id
    }
    
    /**
     * Creates a new tag.
     */
    fun createTag(name: String, churchId: UUID): UUID {
        val tag = Tag(
            name = name,
            churchId = churchId
        )
        
        // TODO: Persist through repository
        return tag.id
    }
    
    /**
     * Adds an attachment to a song.
     */
    fun addAttachment(command: AddAttachmentCommand): UUID {
        val attachment = Attachment(
            songId = command.songId,
            name = command.name,
            url = command.url,
            type = command.type
        )
        
        // TODO: Persist through repository
        return attachment.id
    }
    
    /**
     * Adds a comment to a song.
     */
    fun addComment(command: AddCommentCommand): UUID {
        val comment = SongComment(
            songId = command.songId,
            userId = command.userId,
            content = command.content
        )
        
        // TODO: Persist through repository
        return comment.id
    }
    
    /**
     * Gets comments for a song.
     */
    fun getSongComments(songId: UUID): List<SongComment> {
        // TODO: Fetch from repository
        return emptyList()
    }
    
    /**
     * Searches global song catalog.
     */
    fun searchGlobalSongs(query: String): List<GlobalSong> {
        // TODO: Implement global repository search
        return emptyList()
    }
    
    /**
     * Imports a song from global catalog to church catalog.
     */
    fun importFromGlobal(globalSongId: UUID, churchId: UUID): UUID {
        // TODO: Fetch global song and create local copy
        val localSong = Song(
            title = "Imported Song",
            artist = "Artist",
            key = "C",
            churchId = churchId
        )
        
        // TODO: Persist through repository
        return localSong.id
    }
}