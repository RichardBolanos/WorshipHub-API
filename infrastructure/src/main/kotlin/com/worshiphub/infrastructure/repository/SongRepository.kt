package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Song
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * JPA Repository for Song entities.
 */
// @Repository
// interface SongRepository : JpaRepository<Song, UUID> {
//     
//     @Query("SELECT s FROM Song s WHERE s.churchId = :churchId AND (s.title LIKE %:query% OR s.artist LIKE %:query%)")
//     fun searchByTitleOrArtist(query: String, churchId: UUID): List<Song>
//     
//     fun findByChurchId(churchId: UUID): List<Song>
// }