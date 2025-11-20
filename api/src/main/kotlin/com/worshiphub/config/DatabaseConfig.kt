package com.worshiphub.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn

/**
 * Database configuration to handle Flyway and JPA initialization order
 */
@Configuration
class DatabaseConfig {

    @Bean
    @ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true")
    @DependsOn("flyway")
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            // Flyway will run migrations before JPA initialization
            flyway.migrate()
        }
    }
}