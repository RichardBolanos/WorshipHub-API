package com.worshiphub.application.catalog

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.api.common.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import java.util.*
import kotlin.test.assertEquals

class CatalogApplicationServiceTest {
    
    private val songRepository = mock<SongRepository>()
    private val catalogService = CatalogApplicationService(songRepository)
    
    @Test
    fun `should create song with duplicate validation`() {
        val command = CreateSongCommand(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            bpm = 80,
            chords = "[C]Amazing [F]Grace",
            churchId = UUID.randomUUID()
        )
        
        whenever(songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any())).thenReturn(false)
        whenever(songRepository.save(any())).thenAnswer { it.arguments[0] as Song }
        
        val result = catalogService.createSong(command)
        
        verify(songRepository).save(any())
        assert(result != null)
    }
    
    @Test
    fun `should reject duplicate song creation`() {
        val command = CreateSongCommand(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            bpm = 80,
            chords = "[C]Amazing [F]Grace",
            churchId = UUID.randomUUID()
        )
        
        whenever(songRepository.existsByTitleAndArtistAndChurchId(any(), any(), any())).thenReturn(true)
        
        assertThrows<BusinessException.InvalidBusinessRule> {
            catalogService.createSong(command)
        }
    }
    
    @Test
    fun `should transpose song chords correctly`() {
        val songId = UUID.randomUUID()
        val song = Song(
            id = songId,
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            chords = "[C]Amazing [F]Grace [G]how [C]sweet",
            churchId = UUID.randomUUID()
        )
        
        whenever(songRepository.findById(songId)).thenReturn(song)
        
        val result = catalogService.transposeSong(songId, "D")
        
        assert(result.contains("D"))
    }
    
    @Test
    fun `should search songs by title and artist`() {
        val churchId = UUID.randomUUID()
        val songs = listOf(
            Song(title = "Amazing Grace", artist = "John Newton", key = "C", churchId = churchId),
            Song(title = "How Great Thou Art", artist = "Carl Boberg", key = "G", churchId = churchId)
        )
        
        whenever(songRepository.searchByTitleOrArtist("grace", churchId)).thenReturn(songs.take(1))
        
        val result = catalogService.searchSongs("grace", churchId)
        
        assertEquals(1, result.size)
        assertEquals("Amazing Grace", result[0].title)
    }
}