package com.worshiphub.api.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FirebaseConfigTest {

    @Test
    fun `firebaseApp returns null when no credentials are available`() {
        val config = FirebaseConfig()
        val app = config.firebaseApp()
        // Without FIREBASE_CREDENTIALS or GOOGLE_APPLICATION_CREDENTIALS env vars,
        // the config should gracefully return null
        assertNull(app, "FirebaseApp should be null when no credentials are configured")
    }

    @Test
    fun `firebaseMessaging returns null when firebaseApp is null`() {
        val config = FirebaseConfig()
        val messaging = config.firebaseMessaging(null)
        assertNull(messaging, "FirebaseMessaging should be null when FirebaseApp is not available")
    }

    @Test
    fun `firebaseApp initialization does not throw when credentials are missing`() {
        val config = FirebaseConfig()
        assertDoesNotThrow {
            config.firebaseApp()
        }
    }

    @Test
    fun `firebaseMessaging does not throw when firebaseApp is null`() {
        val config = FirebaseConfig()
        assertDoesNotThrow {
            config.firebaseMessaging(null)
        }
    }

    @Test
    fun `config class is annotated with Configuration`() {
        val annotation = FirebaseConfig::class.java.getAnnotation(
            org.springframework.context.annotation.Configuration::class.java
        )
        assertNotNull(annotation, "FirebaseConfig should be annotated with @Configuration")
    }
}
