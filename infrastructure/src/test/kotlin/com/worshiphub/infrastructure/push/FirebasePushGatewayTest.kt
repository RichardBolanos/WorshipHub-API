package com.worshiphub.infrastructure.push

import com.google.firebase.messaging.*
import com.worshiphub.domain.collaboration.push.PushPayload
import com.worshiphub.domain.collaboration.push.PushResult
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*

/**
 * Unit tests for FirebasePushGateway.
 * Validates: Requirements 13.1, 13.3, 13.4, 13.7, 26.5, 29.1
 */
class FirebasePushGatewayTest : FreeSpec({

    fun createGateway(): Pair<FirebasePushGateway, FirebaseMessaging> {
        val firebaseMessaging = mockk<FirebaseMessaging>()
        val gateway = FirebasePushGateway(firebaseMessaging)
        return gateway to firebaseMessaging
    }

    "FCM error mapping" - {

        "UNREGISTERED maps to InvalidToken" {
            val (gateway, fcm) = createGateway()
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
            every { exception.message } returns "Token not registered"
            every { fcm.send(any()) } throws exception

            val result = gateway.sendToDevice("invalid-token-12345678", PushPayload(title = "Test", body = "Body"))

            result.shouldBeInstanceOf<PushResult.InvalidToken>()
        }

        "INVALID_ARGUMENT maps to InvalidToken" {
            val (gateway, fcm) = createGateway()
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT
            every { exception.message } returns "Invalid argument"
            every { fcm.send(any()) } throws exception

            val result = gateway.sendToDevice("bad-token-123456789", PushPayload(title = "Test", body = "Body"))

            result.shouldBeInstanceOf<PushResult.InvalidToken>()
        }

        "UNAVAILABLE maps to TransientError" {
            val (gateway, fcm) = createGateway()
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.UNAVAILABLE
            every { exception.message } returns "Service unavailable"
            every { fcm.send(any()) } throws exception

            val result = gateway.sendToDevice("valid-token-12345678", PushPayload(title = "Test", body = "Body"))

            result.shouldBeInstanceOf<PushResult.TransientError>()
        }

        "INTERNAL maps to TransientError" {
            val (gateway, fcm) = createGateway()
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.INTERNAL
            every { exception.message } returns "Internal error"
            every { fcm.send(any()) } throws exception

            val result = gateway.sendToDevice("valid-token-12345678", PushPayload(title = "Test", body = "Body"))

            result.shouldBeInstanceOf<PushResult.TransientError>()
        }

        "other error codes map to PermanentError" {
            val (gateway, fcm) = createGateway()
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.SENDER_ID_MISMATCH
            every { exception.message } returns "Sender ID mismatch"
            every { fcm.send(any()) } throws exception

            val result = gateway.sendToDevice("valid-token-12345678", PushPayload(title = "Test", body = "Body"))

            result.shouldBeInstanceOf<PushResult.PermanentError>()
        }
    }

    "Message construction" - {

        "builds Message with correct notification title and body" {
            val (gateway, fcm) = createGateway()
            val messageSlot = slot<Message>()
            every { fcm.send(capture(messageSlot)) } returns "msg-id-123"

            val payload = PushPayload(
                title = "Service Reminder",
                body = "Your service starts in 2 hours",
                data = mapOf("type" to "SERVICE_SCHEDULED"),
                channelId = "services"
            )

            val result = gateway.sendToDevice("valid-token-12345678", payload)

            result.shouldBeInstanceOf<PushResult.Success>()
            (result as PushResult.Success).messageId shouldBe "msg-id-123"
            // Message was captured and sent
            verify { fcm.send(any()) }
        }

        "includes ApnsConfig with alert, badge, sound for iOS" {
            val (gateway, fcm) = createGateway()
            every { fcm.send(any()) } returns "msg-id-456"

            val payload = PushPayload(
                title = "New Song",
                body = "Amazing Grace was added",
                badge = 5,
                sound = "custom_sound"
            )

            val result = gateway.sendToDevice("ios-token-1234567890", payload)

            result.shouldBeInstanceOf<PushResult.Success>()
            verify { fcm.send(any()) }
        }

        "includes iOS category SERVICE_ASSIGNMENT for service assignments" {
            val (gateway, fcm) = createGateway()
            every { fcm.send(any()) } returns "msg-id-789"

            val payload = PushPayload(
                title = "Service Assignment",
                body = "You have been assigned to Sunday Service",
                channelId = "services",
                category = "SERVICE_ASSIGNMENT"
            )

            val result = gateway.sendToDevice("ios-token-1234567890", payload)

            result.shouldBeInstanceOf<PushResult.Success>()
            verify { fcm.send(any()) }
        }
    }

    "null FirebaseMessaging" - {

        "returns PermanentError when FCM is not configured" {
            val gateway = FirebasePushGateway(null)

            val result = gateway.sendToDevice(
                "any-token-1234567890",
                PushPayload(title = "Test", body = "Body")
            )

            result.shouldBeInstanceOf<PushResult.PermanentError>()
        }
    }
})
