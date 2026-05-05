package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.catalog.GlobalSong
import com.worshiphub.domain.catalog.repository.GlobalSongRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaGlobalSongRepository : JpaRepository<GlobalSong, UUID> {
    @Query(
        "SELECT g FROM GlobalSong g WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(g.artist) LIKE LOWER(CONCAT('%', :query, '%'))"
    )
    fun searchByTitleOrArtist(query: String): List<GlobalSong>
}

@Repository
open class GlobalSongRepositoryImpl(
    private val jpaRepository: JpaGlobalSongRepository
) : GlobalSongRepository {

    override fun save(globalSong: GlobalSong): GlobalSong = jpaRepository.save(globalSong)

    override fun findById(id: UUID): GlobalSong? = jpaRepository.findById(id).orElse(null)

    override fun searchByTitleOrArtist(query: String): List<GlobalSong> =
        jpaRepository.searchByTitleOrArtist(query)
}
