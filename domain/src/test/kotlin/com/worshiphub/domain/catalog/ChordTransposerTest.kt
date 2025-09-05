package com.worshiphub.domain.catalog

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChordTransposerTest {
    
    @Test
    fun `should transpose chords from C to D`() {
        val chords = "[C]Amazing [F]Grace [G]how [C]sweet"
        val result = ChordTransposer.transpose(chords, "C", "D")
        
        assertEquals("[D]Amazing [G]Grace [A]how [D]sweet", result)
    }
    
    @Test
    fun `should transpose chords from G to A`() {
        val chords = "[G]How [C]great [D]Thou [G]art"
        val result = ChordTransposer.transpose(chords, "G", "A")
        
        assertEquals("[A]How [D]great [E]Thou [A]art", result)
    }
    
    @Test
    fun `should handle minor chords`() {
        val chords = "[Am]Holy [F]Spirit [C]come [G]fill this [Am]place"
        val result = ChordTransposer.transpose(chords, "Am", "Bm")
        
        assert(result.contains("Bm"))
        assert(result.contains("G"))
        assert(result.contains("D"))
        assert(result.contains("A"))
    }
    
    @Test
    fun `should handle complex chord progressions`() {
        val chords = "[C]Verse [Am]line [F]with [G]progression [C]end"
        val result = ChordTransposer.transpose(chords, "C", "F")
        
        assertEquals("[F]Verse [Dm]line [Bb]with [C]progression [F]end", result)
    }
    
    @Test
    fun `should return original chords when transposing to same key`() {
        val chords = "[C]Amazing [F]Grace"
        val result = ChordTransposer.transpose(chords, "C", "C")
        
        assertEquals(chords, result)
    }
}