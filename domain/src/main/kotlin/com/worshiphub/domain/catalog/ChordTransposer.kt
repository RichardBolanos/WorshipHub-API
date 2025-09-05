package com.worshiphub.domain.catalog

/**
 * Utility object for transposing musical chords.
 */
object ChordTransposer {
    
    private val chromaticScale = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val chordPattern = Regex("""([A-G][#b]?)([^A-G\s]*)""")
    
    /**
     * Transposes chords in ChordPro format from one key to another.
     * 
     * @param chords Original chords in ChordPro format
     * @param fromKey Current key of the song
     * @param toKey Target key for transposition
     * @return Transposed chords in ChordPro format
     */
    fun transpose(chords: String, fromKey: String, toKey: String): String {
        val semitones = calculateSemitones(fromKey, toKey)
        if (semitones == 0) return chords
        
        // Handle ChordPro format [chord]text
        return Regex("\\[([A-G][#b]?)([^\\]]*)\\]").replace(chords) { matchResult ->
            val root = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[2]
            val transposedRoot = transposeNote(root, semitones)
            "[$transposedRoot$suffix]"
        }
    }
    
    private fun calculateSemitones(fromKey: String, toKey: String): Int {
        val fromIndex = chromaticScale.indexOf(normalizeKey(fromKey))
        val toIndex = chromaticScale.indexOf(normalizeKey(toKey))
        if (fromIndex == -1 || toIndex == -1) {
            throw IllegalArgumentException("Invalid key: $fromKey or $toKey")
        }
        return (toIndex - fromIndex + 12) % 12
    }
    
    private fun transposeNote(note: String, semitones: Int): String {
        val normalizedNote = normalizeKey(note)
        val currentIndex = chromaticScale.indexOf(normalizedNote)
        return if (currentIndex != -1) {
            chromaticScale[(currentIndex + semitones) % 12]
        } else note
    }
    
    private fun normalizeKey(key: String): String = key.replace("b", "#")
        .let { if (it == "Db") "C#" else it }
        .let { if (it == "Eb") "D#" else it }
        .let { if (it == "Gb") "F#" else it }
        .let { if (it == "Ab") "G#" else it }
        .let { if (it == "Bb") "A#" else it }
}