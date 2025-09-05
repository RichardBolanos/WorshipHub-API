package com.worshiphub.domain.scheduling.service

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.scheduling.Setlist
import org.springframework.stereotype.Service
import java.util.*

data class GenerationRule(
    val category: String,
    val count: Int,
    val position: Int
)

@Service
class SetlistGenerationService {
    
    fun generateSetlist(
        name: String,
        rules: List<GenerationRule>,
        availableSongs: List<Song>,
        churchId: UUID
    ): Setlist {
        val selectedSongs = mutableListOf<Song>()
        
        rules.sortedBy { it.position }.forEach { rule ->
            val songsForCategory = availableSongs
                .filter { matchesCategory(it, rule.category) }
                .shuffled()
                .take(rule.count)
            
            selectedSongs.addAll(songsForCategory)
        }
        
        return Setlist(
            name = name,
            songIds = selectedSongs.map { it.id },
            churchId = churchId
        )
    }
    
    private fun matchesCategory(song: Song, category: String): Boolean {
        return true
    }
    
    fun calculateDuration(songs: List<Song>): Int {
        return songs.fold(0) { acc, song ->
            acc + (song.bpm?.let { bpm ->
                when {
                    bpm < 80 -> 5
                    bpm > 140 -> 3
                    else -> 4
                }
            } ?: 4)
        }
    }
}