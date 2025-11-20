package com.worshiphub.api.config

import com.worshiphub.domain.organization.*
// import com.worshiphub.infrastructure.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Initializes sample data for development and testing purposes.
 */
@Component
class DataInitializer(
    // private val userRepository: UserRepository,
    // private val churchRepository: ChurchRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            initializeSampleData()
        } catch (e: Exception) {
            // Log error properly instead of println
            // logger.warn("Sample data initialization failed", e)
            println("⚠️ Sample data initialization failed: ${e.message?.take(100)}")
        }
    }

    @Transactional
    private fun initializeSampleData() {
        // TODO: Uncomment when repositories are available
        /*
        // Check if data already exists
        if (churchRepository.count() > 0 || userRepository.count() > 0) {
            println("ℹ️ Sample data already exists, skipping initialization")
            return
        }

        // Create sample church
        val church = Church(
            name = "Grace Community Church",
            address = "123 Main Street, Springfield, IL 62701",
            email = "info@gracecommunity.org"
        )
        val savedChurch = churchRepository.save(church)

        // Create sample users
        val users = listOf(
            User(
                email = "admin@gracecommunity.org",
                firstName = "John",
                lastName = "Smith",
                passwordHash = passwordEncoder.encode("admin123"),
                churchId = savedChurch.id,
                role = UserRole.CHURCH_ADMIN
            ),
            User(
                email = "leader@gracecommunity.org",
                firstName = "Sarah",
                lastName = "Johnson",
                passwordHash = passwordEncoder.encode("leader123"),
                churchId = savedChurch.id,
                role = UserRole.WORSHIP_LEADER
            ),
            User(
                email = "member@gracecommunity.org",
                firstName = "Mike",
                lastName = "Davis",
                passwordHash = passwordEncoder.encode("member123"),
                churchId = savedChurch.id,
                role = UserRole.TEAM_MEMBER
            )
        )

        userRepository.saveAll(users)

        println("✅ Sample data initialized successfully:")
        println("   Church: ${savedChurch.name}")
        println("   Admin: admin@gracecommunity.org / admin123")
        println("   Leader: leader@gracecommunity.org / leader123")
        println("   Member: member@gracecommunity.org / member123")
        */
        println("ℹ️ Sample data initialization disabled - repositories not available")
    }
}