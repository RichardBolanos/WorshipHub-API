package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.repository.SongRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

interface JpaSongRepository : JpaRepository<Song, UUID> {
    fun findByChurchId(churchId: UUID): List<Song>
    
    @Query("SELECT s FROM Song s WHERE s.churchId = :churchId AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchByTitleOrArtist(query: String, churchId: UUID): List<Song>
    
    fun existsByTitleAndArtistAndChurchId(title: String, artist: String, churchId: UUID): Boolean
}

@Repository
open class SongRepositoryImpl(
    private val jpaRepository: JpaSongRepository
) : SongRepository {
    
    override fun save(song: Song): Song = jpaRepository.save(song)
    
    override fun findById(id: UUID): Song? = jpaRepository.findById(id).orElse(null)
    
    override fun findByChurchId(churchId: UUID): List<Song> = jpaRepository.findByChurchId(churchId)
    
    override fun searchByTitleOrArtist(query: String, churchId: UUID): List<Song> = 
        jpaRepository.searchByTitleOrArtist(query, churchId)
    
    override fun filterByCategory(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song> {
        // TODO: Implement category/tag filtering with proper joins
        return findByChurchId(churchId)
    }
    
    override fun delete(song: Song) = jpaRepository.delete(song)
    
    override fun existsByTitleAndArtistAndChurchId(title: String, artist: String, churchId: UUID): Boolean = 
        jpaRepository.existsByTitleAndArtistAndChurchId(title, artist, churchId)
}