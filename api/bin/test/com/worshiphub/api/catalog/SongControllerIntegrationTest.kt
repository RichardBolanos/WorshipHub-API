package com.worshiphub.api.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import com.worshiphub.application.catalog.CatalogApplicationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

/**
 * Integration tests for SongController.
 */
@WebMvcTest(SongController::class)
class SongControllerIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var catalogApplicationService: CatalogApplicationService
    
    @Test
    fun `should create song successfully`() {
        val songId = UUID.randomUUID()
        val request = CreateSongRequest(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            bpm = 80,
            chords = "[C]Amazing [F]Grace"
        )
        
        whenever(catalogApplicationService.createSong(any())).thenReturn(songId)
        
        mockMvc.perform(
            post("/api/v1/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.songId").value(songId.toString()))
    }
    
    @Test
    fun `should return bad request for invalid song data`() {
        val request = CreateSongRequest(
            title = "", // Invalid empty title
            artist = "John Newton",
            key = "C",
            bpm = 80,
            chords = "[C]Amazing [F]Grace"
        )
        
        mockMvc.perform(
            post("/api/v1/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Church-Id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `should search songs with pagination`() {
        val songs = listOf(
            mapOf("id" to UUID.randomUUID().toString(), "title" to "Amazing Grace", "artist" to "John Newton"),
            mapOf("id" to UUID.randomUUID().toString(), "title" to "How Great Thou Art", "artist" to "Carl Boberg")
        )
        
        whenever(catalogApplicationService.searchSongs(any(), any())).thenReturn(songs)
        
        mockMvc.perform(
            get("/api/v1/songs/search")
                .param("query", "grace")
                .param("page", "0")
                .param("size", "20")
                .header("Church-Id", UUID.randomUUID().toString())
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.content").isArray)
    }
    
    @Test
    fun `should transpose song chords`() {
        val songId = UUID.randomUUID()
        val transposedChords = "[D]Amazing [G]Grace [A]how [D]sweet"
        
        whenever(catalogApplicationService.transposeSong(songId, "D")).thenReturn(transposedChords)
        
        mockMvc.perform(
            post("/api/v1/songs/{songId}/transpose", songId)
                .param("toKey", "D")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.chords").value(transposedChords))
    }
    
    @Test
    fun `should filter songs by category and tags`() {
        val songs = listOf(
            mapOf("id" to UUID.randomUUID().toString(), "title" to "Worship Song", "category" to "Worship")
        )
        
        whenever(catalogApplicationService.filterSongs(any(), any(), any())).thenReturn(songs)
        
        mockMvc.perform(
            get("/api/v1/songs/filter")
                .param("categoryId", UUID.randomUUID().toString())
                .header("Church-Id", UUID.randomUUID().toString())
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.content").isArray)
    }
}