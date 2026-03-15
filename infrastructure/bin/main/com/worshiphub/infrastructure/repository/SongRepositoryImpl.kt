package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.Song
import com.worshiphub.domain.catalog.repository.SongRepository
import java.util.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface JpaSongRepository : JpaRepository<Song, UUID> {
    fun findByChurchId(churchId: UUID, pageable: Pageable): Page<Song>

    @Query(
            "SELECT s FROM Song s WHERE s.churchId = :churchId AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :query, '%')))"
    )
    fun searchByTitleOrArtist(query: String, churchId: UUID, pageable: Pageable): Page<Song>

    @Query("SELECT DISTINCT s FROM Song s LEFT JOIN FETCH s.categories LEFT JOIN FETCH s.tags WHERE s.id IN :ids")
    fun findAllWithCollections(ids: List<UUID>): List<Song>

    fun existsByTitleAndArtistAndChurchId(title: String, artist: String?, churchId: UUID): Boolean

    @Query(
            "SELECT DISTINCT s.id FROM Song s JOIN s.categories c WHERE s.churchId = :churchId AND c.id = :categoryId"
    )
    fun findIdsByCategoryAndChurchId(categoryId: UUID, churchId: UUID): List<UUID>

    @Query(
            "SELECT DISTINCT s.id FROM Song s JOIN s.tags t WHERE s.churchId = :churchId AND t.id IN :tagIds"
    )
    fun findIdsByTagsAndChurchId(tagIds: List<UUID>, churchId: UUID): List<UUID>

    @Query(
            "SELECT DISTINCT s.id FROM Song s JOIN s.categories c JOIN s.tags t WHERE s.churchId = :churchId AND c.id = :categoryId AND t.id IN :tagIds"
    )
    fun findIdsByCategoryAndTagsAndChurchId(categoryId: UUID, tagIds: List<UUID>, churchId: UUID): List<UUID>
}

@Repository
open class SongRepositoryImpl(private val jpaRepository: JpaSongRepository) : SongRepository {

    override fun save(song: Song): Song = jpaRepository.save(song)

    override fun findById(id: UUID): Song? = jpaRepository.findById(id).orElse(null)

    override fun findByChurchId(churchId: UUID, page: Int, size: Int): List<Song> =
            jpaRepository.findByChurchId(churchId, PageRequest.of(page, size)).content

    override fun searchByTitleOrArtist(
            query: String,
            churchId: UUID,
            page: Int,
            size: Int
    ): List<Song> =
            jpaRepository.searchByTitleOrArtist(query, churchId, PageRequest.of(page, size)).content

    override fun filterByCategory(
            categoryId: UUID?,
            tagIds: List<UUID>,
            churchId: UUID
    ): List<Song> {
        val ids: List<UUID> = when {
            categoryId != null && tagIds.isNotEmpty() ->
                jpaRepository.findIdsByCategoryAndTagsAndChurchId(categoryId, tagIds, churchId)
            categoryId != null ->
                jpaRepository.findIdsByCategoryAndChurchId(categoryId, churchId)
            tagIds.isNotEmpty() ->
                jpaRepository.findIdsByTagsAndChurchId(tagIds, churchId)
            else ->
                return findByChurchId(churchId, 0, 100)
        }
        if (ids.isEmpty()) return emptyList()
        return jpaRepository.findAllWithCollections(ids)
    }

    override fun delete(song: Song) = jpaRepository.delete(song)

    override fun existsByTitleAndArtistAndChurchId(
            title: String,
            artist: String?,
            churchId: UUID
    ): Boolean = jpaRepository.existsByTitleAndArtistAndChurchId(title, artist, churchId)
}
