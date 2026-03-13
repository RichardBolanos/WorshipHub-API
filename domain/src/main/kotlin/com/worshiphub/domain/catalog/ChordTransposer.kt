package com.worshiphub.domain.catalog

/**
 * Utility object for transposing musical chords.
 */
object ChordTransposer {
    
    private val chromaticScale = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val chromaticScaleFlats = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    
    // Keys that typically use flats
    private val flatKeys = setOf("F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb", "Dm", "Gm", "Cm", "Fm", "Bbm", "Ebm")
    
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
        
        val useFlats = flatKeys.contains(toKey)
        
        // Handle ChordPro format [chord]text
        return Regex("\\[([A-G][#b]?)([^\\]]*)\\]").replace(chords) { matchResult ->
            val root = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[2]
            val transposedRoot = transposeNote(root, semitones, useFlats)
            "[$transposedRoot$suffix]"
        }
    }
    
    private fun calculateSemitones(fromKey: String, toKey: String): Int {
        // Extract root note from key (remove minor/major suffix)
        val fromRoot = extractRootNote(fromKey)
        val toRoot = extractRootNote(toKey)
        
        val fromIndex = chromaticScale.indexOf(normalizeKey(fromRoot))
        val toIndex = chromaticScale.indexOf(normalizeKey(toRoot))
        if (fromIndex == -1 || toIndex == -1) {
            throw IllegalArgumentException("Invalid key: $fromKey or $toKey")
        }
        return (toIndex - fromIndex + 12) % 12
    }
    
    private fun extractRootNote(key: String): String {
        // Remove 'm', 'min', 'maj', 'major', 'minor' suffixes
        return key.replace(Regex("(m|min|maj|major|minor)$", RegexOption.IGNORE_CASE), "")
    }
    
    private fun transposeNote(note: String, semitones: Int, useFlats: Boolean): String {
        val normalizedNote = normalizeKey(note)
        val currentIndex = chromaticScale.indexOf(normalizedNote)
        return if (currentIndex != -1) {
            val newIndex = (currentIndex + semitones) % 12
            if (useFlats) chromaticScaleFlats[newIndex] else chromaticScale[newIndex]
        } else note
    }
    
    private fun normalizeKey(key: String): String = key.replace("b", "#")
        .let { if (it == "Db") "C#" else it }
        .let { if (it == "Eb") "D#" else it }
        .let { if (it == "Gb") "F#" else it }
        .let { if (it == "Ab") "G#" else it }
        .let { if (it == "Bb") "A#" else it }
}