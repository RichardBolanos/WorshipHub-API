package com.worshiphub.domain.organization.repository

import com.worshiphub.domain.organization.User
import java.util.*

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findByEmailAndIsActiveTrue(email: String): User?
    fun findByChurchIdAndIsActiveTrue(churchId: UUID): List<User>
    fun existsByEmail(email: String): Boolean
    fun delete(user: User)
}