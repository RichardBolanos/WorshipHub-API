package com.worshiphub.domain.catalog.repository

import com.worshiphub.domain.catalog.Attachment
import java.util.*

interface AttachmentRepository {
    fun save(attachment: Attachment): Attachment
    fun findById(id: UUID): Attachment?
    fun findBySongId(songId: UUID): List<Attachment>
    fun delete(attachment: Attachment)
}