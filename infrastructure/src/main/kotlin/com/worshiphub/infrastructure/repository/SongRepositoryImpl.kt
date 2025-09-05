package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.repository.SongRepository
import com.worshiphub.infrastructure.persistence.SongEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

interface JpaSongRepository : JpaRepository<SongEntity, UUID> {
    fun findByChurchId(churchId: UUID): List<SongEntity>
    
    @Query("SELECT s FROM SongEntity s WHERE s.churchId = :churchId AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchByTitleOrArtist(query: String, churchId: UUID): List<SongEntity>
    
    fun existsByTitleAndArtistAndChurchId(title: String, artist: String, churchId: UUID): Boolean
}

@Repository
open class SongRepositoryImpl(
    private val jpaRepository: JpaSongRepository
) : SongRepository {
    
    override fun save(song: Song): Song {
        val entity = SongEntity.fromDomain(song)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findById(id: UUID): Song? = 
        jpaRepository.findById(id).map { it.toDomain() }.orElse(null)
    
    override fun findByChurchId(churchId: UUID): List<Song> = 
        jpaRepository.findByChurchId(churchId).map { it.toDomain() }
    
    override fun searchByTitleOrArtist(query: String, churchId: UUID): List<Song> = 
        jpaRepository.searchByTitleOrArtist(query, churchId).map { it.toDomain() }
    
    override fun filterByCategory(categoryId: UUID?, tagIds: List<UUID>, churchId: UUID): List<Song> {
        // TODO: Implement category/tag filtering with proper joins
        return findByChurchId(churchId)
    }
    
    override fun delete(song: Song) {
        val entity = SongEntity.fromDomain(song)
        jpaRepository.delete(entity)
    }
    
    override fun existsByTitleAndArtistAndChurchId(title: String, artist: String, churchId: UUID): Boolean = 
        jpaRepository.existsByTitleAndArtistAndChurchId(title, artist, churchId)
}