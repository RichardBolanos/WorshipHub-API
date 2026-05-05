package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.DevicePlatform
import com.worshiphub.domain.collaboration.push.DeviceToken
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for DeviceTokenService.
 * Validates: Requirements 1.2, 1.3, 1.5, 1.7, 29.1
 */
class DeviceTokenServiceTest : FreeSpec({

    fun createService(): Pair<DeviceTokenService, DeviceTokenRepository> {
        val repo = mockk<DeviceTokenRepository>()
        val service = DeviceTokenService(repo)
        return service to repo
    }

    "registerToken" - {

        "successful registration creates token in DB" {
            val (service, repo) = createService()
            val userId = UUID.randomUUID()
            val tokenStr = "fcm-token-android-123"
            val savedToken = DeviceToken(
                userId = userId,
                token = tokenStr,
                platform = DevicePlatform.ANDROID
            )

            every { repo.findByToken(tokenStr) } returns null
            every { repo.save(any()) } returns savedToken

            val result = service.registerToken(userId, tokenStr, "ANDROID")

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe savedToken.id
            verify { repo.save(match { it.token == tokenStr && it.platform == DevicePlatform.ANDROID }) }
        }

        "duplicate token updates lastUsedAt instead of creating new" {
            val (service, repo) = createService()
            val userId = UUID.randomUUID()
            val tokenStr = "fcm-token-existing"
            val existingToken = DeviceToken(
                userId = userId,
                token = tokenStr,
                platform = DevicePlatform.ANDROID,
                createdAt = LocalDateTime.now().minusDays(5),
                lastUsedAt = LocalDateTime.now().minusDays(1)
            )

            every { repo.findByToken(tokenStr) } returns existingToken
            every { repo.save(any()) } answers { firstArg() }

            val result = service.registerToken(userId, tokenStr, "ANDROID")

            result.isSuccess shouldBe true
            verify {
                repo.save(match {
                    it.token == tokenStr && it.lastUsedAt.isAfter(existingToken.lastUsedAt)
                })
            }
        }

        "registers iOS token with IOS platform" {
            val (service, repo) = createService()
            val userId = UUID.randomUUID()
            val tokenStr = "apns-token-ios-456"
            val savedToken = DeviceToken(
                userId = userId,
                token = tokenStr,
                platform = DevicePlatform.IOS
            )

            every { repo.findByToken(tokenStr) } returns null
            every { repo.save(any()) } returns savedToken

            val result = service.registerToken(userId, tokenStr, "IOS")

            result.isSuccess shouldBe true
            verify { repo.save(match { it.platform == DevicePlatform.IOS }) }
        }
    }

    "unregisterToken" - {

        "removes token for the user" {
            val (service, repo) = createService()
            val userId = UUID.randomUUID()
            val tokenStr = "fcm-token-to-remove"

            every { repo.deleteByUserIdAndToken(userId, tokenStr) } just Runs

            val result = service.unregisterToken(userId, tokenStr)

            result.isSuccess shouldBe true
            verify { repo.deleteByUserIdAndToken(userId, tokenStr) }
        }
    }

    "unregisterAllTokens" - {

        "removes all tokens for the user" {
            val (service, repo) = createService()
            val userId = UUID.randomUUID()

            every { repo.deleteAllByUserId(userId) } just Runs

            val result = service.unregisterAllTokens(userId)

            result.isSuccess shouldBe true
            verify { repo.deleteAllByUserId(userId) }
        }
    }
})
