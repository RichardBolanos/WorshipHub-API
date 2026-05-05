package com.worshiphub.application.notification.e2e

import com.worshiphub.domain.collaboration.push.PushGateway
import com.worshiphub.domain.collaboration.push.PushPayload
import com.worshiphub.domain.collaboration.push.PushResult

/**
 * Mock implementation of [PushGateway] for E2E integration tests.
 *
 * Records all sent messages in [sentMessages] for later verification.
 * By default returns [PushResult.Success] for every send.
 *
 * Validates: Requirements 29.7, 29.8
 */
class MockPushGateway : PushGateway {

    /** All payloads sent via [sendToDevice] or [sendToDevices], paired with their token. */
    val sentMessages: MutableList<SentMessage> = mutableListOf()

    /** Optional override to control the result returned for each token. */
    var resultProvider: (String, PushPayload) -> PushResult = { _, _ ->
        PushResult.Success("mock-message-${System.nanoTime()}")
    }

    override fun sendToDevice(token: String, notification: PushPayload): PushResult {
        val result = resultProvider(token, notification)
        sentMessages.add(SentMessage(token, notification, result))
        return result
    }

    override fun sendToDevices(tokens: List<String>, notification: PushPayload): List<PushResult> {
        return tokens.map { token -> sendToDevice(token, notification) }
    }

    /**
     * Filters sent messages by the `type` key in the payload's data map.
     */
    fun findByType(type: String): List<SentMessage> =
        sentMessages.filter { it.payload.data["type"] == type }

    /**
     * Filters sent messages by the notification channel ID.
     */
    fun findByChannel(channelId: String): List<SentMessage> =
        sentMessages.filter { it.payload.channelId == channelId }

    /**
     * Filters sent messages sent to a specific token.
     */
    fun findByToken(token: String): List<SentMessage> =
        sentMessages.filter { it.token == token }

    /**
     * Clears all recorded messages. Call between tests.
     */
    fun reset() {
        sentMessages.clear()
        resultProvider = { _, _ -> PushResult.Success("mock-message-${System.nanoTime()}") }
    }

    /**
     * Represents a single push notification send attempt.
     */
    data class SentMessage(
        val token: String,
        val payload: PushPayload,
        val result: PushResult
    )
}
