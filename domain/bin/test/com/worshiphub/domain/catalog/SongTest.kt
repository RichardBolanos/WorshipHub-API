package com.worshiphub.domain.catalog

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SongTest {
    
    @Test
    fun `should transpose song to different key`() {
        val song = Song(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            chords = "[C]Amazing [F]Grace [G]how [C]sweet",
            churchId = UUID.randomUUID()
        )
        
        val transposed = song.transpose("D")
        
        assertEquals("D", transposed.key)
        assert(transposed.chords?.contains("D") == true)
        assert(transposed.chords?.contains("G") == true)
        assert(transposed.chords?.contains("A") == true)
    }
    
    @Test
    fun `should maintain same song when transposing to same key`() {
        val song = Song(
            title = "Amazing Grace",
            artist = "John Newton",
            key = "C",
            chords = "[C]Amazing [F]Grace",
            churchId = UUID.randomUUID()
        )
        
        val transposed = song.transpose("C")
        
        assertEquals(song.key, transposed.key)
        assertEquals(song.chords, transposed.chords)
    }
    
    @Test
    fun `should handle song equality by id`() {
        val id = UUID.randomUUID()
        val song1 = Song(
            id = id,
            title = "Song 1",
            artist = "Artist 1",
            key = "C",
            churchId = UUID.randomUUID()
        )
        val song2 = Song(
            id = id,
            title = "Song 2",
            artist = "Artist 2",
            key = "D",
            churchId = UUID.randomUUID()
        )
        
        assertEquals(song1, song2)
        assertEquals(song1.hashCode(), song2.hashCode())
    }
    
    @Test
    fun `should differentiate songs with different ids`() {
        val song1 = Song(
            title = "Same Song",
            artist = "Same Artist",
            key = "C",
            churchId = UUID.randomUUID()
        )
        val song2 = Song(
            title = "Same Song",
            artist = "Same Artist",
            key = "C",
            churchId = UUID.randomUUID()
        )
        
        assertNotEquals(song1, song2)
    }
}