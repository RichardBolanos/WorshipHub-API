package com.worshiphub.domain.scheduling.service

import com.worshiphub.domain.catalog.Song
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetlistGenerationServiceTest {
    
    private val setlistService = SetlistGenerationService()
    
    @Test
    fun `should calculate duration from BPM`() {
        val songs = listOf(
            Song(title = "Slow Song", artist = "Artist", key = "C", bpm = 70, churchId = UUID.randomUUID()),
            Song(title = "Medium Song", artist = "Artist", key = "G", bpm = 120, churchId = UUID.randomUUID()),
            Song(title = "Fast Song", artist = "Artist", key = "D", bpm = 150, churchId = UUID.randomUUID()),
            Song(title = "No BPM Song", artist = "Artist", key = "F", bpm = null, churchId = UUID.randomUUID())
        )
        
        val duration = setlistService.calculateDuration(songs)
        
        assertEquals(16, duration)
    }
    
    @Test
    fun `should handle empty song list`() {
        val duration = setlistService.calculateDuration(emptyList())
        
        assertEquals(0, duration)
    }
}