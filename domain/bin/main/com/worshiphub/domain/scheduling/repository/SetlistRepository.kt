package com.worshiphub.domain.scheduling.repository

import com.worshiphub.domain.scheduling.Setlist
import java.util.*

/**
 * Repository interface for Setlist entity.
 */
interface SetlistRepository {
    
    fun save(setlist: Setlist): Setlist
    fun findById(id: UUID): Setlist?
    fun findByChurchId(churchId: UUID): List<Setlist>
    fun delete(setlist: Setlist)
}