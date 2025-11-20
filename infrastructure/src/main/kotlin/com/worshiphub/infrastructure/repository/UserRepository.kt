package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaUserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByChurchIdAndIsActiveTrue(churchId: UUID): List<User>
    fun existsByEmail(email: String): Boolean
    fun findByEmailAndIsActiveTrue(email: String): User?
}

@Repository
open class UserRepositoryImpl(
    private val jpaRepository: JpaUserRepository
) : UserRepository {
    
    override fun save(user: User): User = jpaRepository.save(user)
    
    override fun findById(id: UUID): User? = 
        jpaRepository.findById(id).orElse(null)
    
    override fun findByEmail(email: String): User? = 
        jpaRepository.findByEmail(email)
    
    override fun findByEmailAndIsActiveTrue(email: String): User? = 
        jpaRepository.findByEmailAndIsActiveTrue(email)
    
    override fun findByChurchIdAndIsActiveTrue(churchId: UUID): List<User> = 
        jpaRepository.findByChurchIdAndIsActiveTrue(churchId)
    
    override fun existsByEmail(email: String): Boolean = 
        jpaRepository.existsByEmail(email)
    
    override fun delete(user: User) {
        jpaRepository.deleteById(user.id)
    }
}