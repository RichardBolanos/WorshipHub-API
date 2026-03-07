package com.worshiphub.domain.collaboration.repository

import com.worshiphub.domain.collaboration.SongComment
import java.util.*

interface SongCommentRepository {
    fun save(comment: SongComment): SongComment
    fun findById(id: UUID): SongComment?
    fun findBySongId(songId: UUID): List<SongComment>
    fun findByUserId(userId: UUID): List<SongComment>
    fun delete(comment: SongComment)
}