package com.worshiphub.domain.catalog.repository

import com.worshiphub.domain.catalog.Song
import java.util.*

interface SongRepository {
    fun save(song: Song): Song
    fun findById(id: UUID): Song?
    fun findByChurchId(churchId: UUID, page: Int, size: Int): List<Song>
    fun searchByTitleOrArtist(query: String, churchId: UUID, page: Int, size: Int): List<Song>
    fun filterByCategory(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song>
    fun delete(song: Song)
    fun existsByTitleAndArtistAndChurchId(title: String, artist: String?, churchId: UUID): Boolean
}
