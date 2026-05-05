package com.worshiphub.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.FileInputStream

/**
 * Firebase Admin SDK configuration for push notifications via FCM.
 *
 * Supports two credential sources (checked in order):
 * 1. FIREBASE_CREDENTIALS env var — inline JSON string with service account credentials
 * 2. GOOGLE_APPLICATION_CREDENTIALS env var — file path to a service account JSON file
 *
 * If neither is available, Firebase initialization is skipped and push notifications
 * are disabled. The application continues to function normally without push support.
 */
@Configuration
class FirebaseConfig {

    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp? {
        if (FirebaseApp.getApps().isNotEmpty()) {
            logger.info("FirebaseApp already initialized, reusing existing instance")
            return FirebaseApp.getInstance()
        }

        val credentials = resolveCredentials()
        if (credentials == null) {
            logger.warn(
                "Firebase credentials not found. Push notifications are disabled. " +
                "Set FIREBASE_CREDENTIALS (inline JSON) or GOOGLE_APPLICATION_CREDENTIALS (file path) " +
                "to enable push notifications."
            )
            return null
        }

        return try {
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            val app = FirebaseApp.initializeApp(options)
            logger.info("FirebaseApp initialized successfully")
            app
        } catch (e: Exception) {
            logger.error("Failed to initialize FirebaseApp: ${e.message}", e)
            null
        }
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp?): FirebaseMessaging? {
        if (firebaseApp == null) {
            logger.warn("FirebaseMessaging bean not created — FirebaseApp is not available")
            return null
        }

        return try {
            val messaging = FirebaseMessaging.getInstance(firebaseApp)
            logger.info("FirebaseMessaging bean created successfully")
            messaging
        } catch (e: Exception) {
            logger.error("Failed to create FirebaseMessaging instance: ${e.message}", e)
            null
        }
    }

    private fun resolveCredentials(): GoogleCredentials? {
        // 1. Try inline JSON from FIREBASE_CREDENTIALS env var
        val inlineJson = System.getenv("FIREBASE_CREDENTIALS")
        if (!inlineJson.isNullOrBlank()) {
            return try {
                val stream = ByteArrayInputStream(inlineJson.toByteArray(Charsets.UTF_8))
                val credentials = GoogleCredentials.fromStream(stream)
                logger.info("Firebase credentials loaded from FIREBASE_CREDENTIALS environment variable")
                credentials
            } catch (e: Exception) {
                logger.error("Failed to parse FIREBASE_CREDENTIALS: ${e.message}", e)
                null
            }
        }

        // 2. Try file path from GOOGLE_APPLICATION_CREDENTIALS env var
        val filePath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if (!filePath.isNullOrBlank()) {
            return try {
                val stream = FileInputStream(filePath)
                val credentials = GoogleCredentials.fromStream(stream)
                logger.info("Firebase credentials loaded from file: {}", filePath)
                credentials
            } catch (e: Exception) {
                logger.error("Failed to load Firebase credentials from file '{}': {}", filePath, e.message, e)
                null
            }
        }

        return null
    }
}
