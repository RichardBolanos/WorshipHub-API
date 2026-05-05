package com.worshiphub.application.notification.e2e

import com.worshiphub.application.notification.DeviceTokenService
import com.worshiphub.domain.collaboration.push.DevicePlatform
import com.worshiphub.domain.collaboration.push.DeviceToken
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.*

class TokenRegistrationE2ETest : FreeSpec({

    "18.22 E2E: token registration and unregistration" - {
        "registers Android token successfully" {
            val deviceTokenRepo = mockk<DeviceTokenRepository>()
            val userId = UUID.randomUUID()
            val tokenId = UUID.randomUUID()
            val token = "android-fcm-token-abcdef123456"

            every { deviceTokenRepo.findByToken(token) } returns null
            every { deviceTokenRepo.save(any()) } answers {
                firstArg<DeviceToken>().copy(id = tokenId)
            }

            val service = DeviceTokenService(deviceTokenRepo)
            val result = service.registerToken(userId, token, "ANDROID")

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe tokenId

            verify {
                deviceTokenRepo.save(match {
                    it.userId == userId && it.token == token && it.platform == DevicePlatform.ANDROID
                })
            }
        }

        "registers iOS token successfully" {
            val deviceTokenRepo = mockk<DeviceTokenRepository>()
            val userId = UUID.randomUUID()
            val tokenId = UUID.randomUUID()
            val token = "ios-apns-token-abcdef123456"

            every { deviceTokenRepo.findByToken(token) } returns null
            every { deviceTokenRepo.save(any()) } answers {
                firstArg<DeviceToken>().copy(id = tokenId)
            }

            val service = DeviceTokenService(deviceTokenRepo)
            val result = service.registerToken(userId, token, "IOS")

            result.isSuccess shouldBe true

            verify {
                deviceTokenRepo.save(match {
                    it.platform == DevicePlatform.IOS
                })
            }
        }

        "unregisters token on logout" {
            val deviceTokenRepo = mockk<DeviceTokenRepository>()
            val userId = UUID.randomUUID()
            val token = "fcm-token-to-remove"

            every { deviceTokenRepo.deleteByUserIdAndToken(userId, token) } just Runs

            val service = DeviceTokenService(deviceTokenRepo)
            val result = service.unregisterToken(userId, token)

            result.isSuccess shouldBe true
            verify { deviceTokenRepo.deleteByUserIdAndToken(userId, token) }
        }
    }
})
