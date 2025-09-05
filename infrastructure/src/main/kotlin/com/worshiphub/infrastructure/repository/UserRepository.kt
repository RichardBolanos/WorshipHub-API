package com.worshiphub.infrastructure.repository

import com.worshiphub.domain.organization.User
import com.worshiphub.domain.organization.repository.UserRepository
import com.worshiphub.infrastructure.persistence.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface JpaUserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByChurchIdAndIsActiveTrue(churchId: UUID): List<UserEntity>
    fun existsByEmail(email: String): Boolean
    fun findByEmailAndIsActiveTrue(email: String): UserEntity?
}

@Repository
open class UserRepositoryImpl(
    private val jpaRepository: JpaUserRepository
) : UserRepository {
    
    override fun save(user: User): User {
        val entity = UserEntity.fromDomain(user)
        return jpaRepository.save(entity).toDomain()
    }
    
    override fun findById(id: UUID): User? = 
        jpaRepository.findById(id).map { it.toDomain() }.orElse(null)
    
    override fun findByEmail(email: String): User? = 
        jpaRepository.findByEmail(email)?.toDomain()
    
    override fun findByEmailAndIsActiveTrue(email: String): User? = 
        jpaRepository.findByEmailAndIsActiveTrue(email)?.toDomain()
    
    override fun findByChurchIdAndIsActiveTrue(churchId: UUID): List<User> = 
        jpaRepository.findByChurchIdAndIsActiveTrue(churchId).map { it.toDomain() }
    
    override fun existsByEmail(email: String): Boolean = 
        jpaRepository.existsByEmail(email)
    
    override fun delete(user: User) {
        val entity = UserEntity.fromDomain(user)
        jpaRepository.delete(entity)
    }
}