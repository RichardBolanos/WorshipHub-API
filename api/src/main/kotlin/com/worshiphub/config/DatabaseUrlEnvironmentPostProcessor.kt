package com.worshiphub.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.net.URI

/**
 * Normalizes the DATABASE_URL environment variable so the application accepts ANY
 * standard PostgreSQL connection string format that hosting providers (Render, Neon,
 * Supabase, Heroku, Railway, etc.) typically expose.
 *
 * Accepted input formats for SPRING_DATASOURCE_URL / DATABASE_URL:
 *
 *   1. jdbc:postgresql://host[:port]/db?param=value   -> used as-is
 *   2. postgresql://user:pass@host[:port]/db?params   -> converted to jdbc form
 *   3. postgres://user:pass@host[:port]/db?params     -> converted to jdbc form
 *
 * When credentials are embedded in the URL (cases 2/3) they are extracted and
 * exposed as spring.datasource.username / password so HikariCP can use them.
 *
 * Runs BEFORE the datasource is initialized so Spring sees the canonical values.
 */
class DatabaseUrlEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 10

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        val rawUrl = environment.getProperty("DATABASE_URL")
            ?: environment.getProperty("spring.datasource.url")
            ?: return

        if (rawUrl.isBlank()) return

        // Already JDBC -> nothing to do
        if (rawUrl.startsWith("jdbc:")) return

        // Only handle postgres-style URLs (postgresql:// or postgres://)
        if (!rawUrl.startsWith("postgresql://") && !rawUrl.startsWith("postgres://")) return

        val converted = try {
            convertPostgresUrl(rawUrl)
        } catch (e: Exception) {
            // Don't block startup; let Spring fail with the original error if invalid.
            System.err.println(
                "[DatabaseUrlEnvironmentPostProcessor] Could not parse DATABASE_URL: ${e.message}"
            )
            return
        }

        val overrides = mutableMapOf<String, Any>(
            "spring.datasource.url" to converted.jdbcUrl,
            "spring.datasource.driver-class-name" to "org.postgresql.Driver"
        )
        converted.username?.let { overrides["spring.datasource.username"] = it }
        converted.password?.let { overrides["spring.datasource.password"] = it }

        environment.propertySources.addFirst(
            MapPropertySource("databaseUrlNormalization", overrides)
        )
    }

    private fun convertPostgresUrl(url: String): ParsedDbUrl {
        // URI requires a scheme it understands; postgresql:// works with java.net.URI.
        val uri = URI(url)

        val host = uri.host
            ?: throw IllegalArgumentException("Missing host in DATABASE_URL")
        val port = if (uri.port == -1) 5432 else uri.port
        val path = (uri.path ?: "").ifBlank { "/" }
        val query = uri.query?.let { "?$it" } ?: ""

        // userInfo is "user" or "user:password" (URL-encoded). Decode each component.
        val userInfo = uri.rawUserInfo
        val (user, pass) = if (userInfo.isNullOrBlank()) {
            null to null
        } else {
            val parts = userInfo.split(":", limit = 2)
            val u = decode(parts[0])
            val p = if (parts.size > 1) decode(parts[1]) else null
            u to p
        }

        val jdbc = "jdbc:postgresql://$host:$port$path$query"
        return ParsedDbUrl(jdbcUrl = jdbc, username = user, password = pass)
    }

    private fun decode(value: String): String =
        java.net.URLDecoder.decode(value, Charsets.UTF_8)

    private data class ParsedDbUrl(
        val jdbcUrl: String,
        val username: String?,
        val password: String?
    )
}
