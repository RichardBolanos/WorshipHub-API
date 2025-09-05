package com.worshiphub.domain.organization.repository

import com.worshiphub.domain.organization.Church
import java.util.*

interface ChurchRepository {
    fun save(church: Church): Church
    fun findById(id: UUID): Church?
    fun findByEmail(email: String): Church?
    fun existsByEmail(email: String): Boolean
    fun delete(church: Church)
}