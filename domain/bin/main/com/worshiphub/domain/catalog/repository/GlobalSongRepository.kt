package com.worshiphub.domain.catalog.repository

import com.worshiphub.domain.catalog.GlobalSong
import java.util.*

interface GlobalSongRepository {
    fun save(globalSong: GlobalSong): GlobalSong
    fun findById(id: UUID): GlobalSong?
    fun searchByTitleOrArtist(query: String): List<GlobalSong>
}
