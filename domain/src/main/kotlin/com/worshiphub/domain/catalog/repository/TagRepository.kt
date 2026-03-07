package com.worshiphub.domain.catalog.repository

import com.worshiphub.domain.catalog.Tag
import java.util.*

interface TagRepository {
    fun save(tag: Tag): Tag
    fun findById(id: UUID): Tag?
    fun findByChurchId(churchId: UUID): List<Tag>
    fun findBySongId(songId: UUID): List<Tag>
    fun delete(tag: Tag)
}