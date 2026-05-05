package com.worshiphub.infrastructure.push

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushNotification
import com.worshiphub.domain.collaboration.push.PushGateway
import com.worshiphub.domain.collaboration.push.PushPayload
import com.worshiphub.domain.collaboration.push.PushResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Firebase Cloud Messaging implementation of [PushGateway].
 *
 * Constructs platform-specific FCM messages with:
 * - AndroidConfig: notification channel and sound
 * - ApnsConfig: alert (title/body), badge, sound, and category for iOS quick actions
 * - WebpushConfig: WorshipHub icon for browser notifications
 *
 * Maps FCM exceptions to [PushResult] subtypes:
 * - UNREGISTERED / INVALID_ARGUMENT → [PushResult.InvalidToken]
 * - UNAVAILABLE / INTERNAL → [PushResult.TransientError]
 * - Any other error → [PushResult.PermanentError]
 *
 * If [FirebaseMessaging] is null (credentials not configured), all sends return
 * [PushResult.PermanentError] indicating that FCM is not available.
 *
 * Validates: Requirements 13.1, 13.3, 13.4, 13.6, 13.7, 26.5
 */
@Component
class FirebasePushGateway(
    private val firebaseMessaging: FirebaseMessaging?
) : PushGateway {

    private val logger = LoggerFactory.getLogger(FirebasePushGateway::class.java)

    override fun sendToDevice(token: String, notification: PushPayload): PushResult {
        if (firebaseMessaging == null) {
            logger.warn("FCM is not configured — push notification not sent to token: {}", maskToken(token))
            return PushResult.PermanentError(token, "FCM is not configured. Set Firebase credentials to enable push notifications.")
        }

        val message = buildMessage(token, notification)

        return try {
            val messageId = firebaseMessaging.send(message)
            logger.debug("Push notification sent successfully to token {}: messageId={}", maskToken(token), messageId)
            PushResult.Success(messageId)
        } catch (e: FirebaseMessagingException) {
            handleMessagingException(token, e)
        } catch (e: Exception) {
            logger.error("Unexpected error sending push notification to token {}: {}", maskToken(token), e.message, e)
            PushResult.PermanentError(token, e.message ?: "Unexpected error")
        }
    }

    override fun sendToDevices(tokens: List<String>, notification: PushPayload): List<PushResult> {
        return tokens.map { token -> sendToDevice(token, notification) }
    }

    /**
     * Builds a platform-aware FCM [Message] with Android, APNs (iOS), and Web push configs.
     */
    private fun buildMessage(token: String, notification: PushPayload): Message {
        val builder = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(notification.title)
                    .setBody(notification.body)
                    .build()
            )
            .putAllData(notification.data)
            .setAndroidConfig(buildAndroidConfig(notification))
            .setApnsConfig(buildApnsConfig(notification))
            .setWebpushConfig(buildWebpushConfig())

        return builder.build()
    }

    /**
     * Android configuration: sets the notification channel and sound.
     */
    private fun buildAndroidConfig(notification: PushPayload): AndroidConfig {
        return AndroidConfig.builder()
            .setNotification(
                AndroidNotification.builder()
                    .setChannelId(notification.channelId ?: "default")
                    .setSound(notification.sound ?: "default")
                    .build()
            )
            .build()
    }

    /**
     * APNs (iOS) configuration: sets alert with title/body, badge count, sound,
     * and category for quick actions (e.g., SERVICE_ASSIGNMENT for Accept/Decline).
     */
    private fun buildApnsConfig(notification: PushPayload): ApnsConfig {
        val apsBuilder = Aps.builder()
            .setAlert(
                ApsAlert.builder()
                    .setTitle(notification.title)
                    .setBody(notification.body)
                    .build()
            )
            .setBadge(notification.badge ?: 1)
            .setSound(notification.sound ?: "default")

        if (notification.category != null) {
            apsBuilder.setCategory(notification.category)
        }

        return ApnsConfig.builder()
            .setAps(apsBuilder.build())
            .build()
    }

    /**
     * Web push configuration: sets the WorshipHub icon for browser notifications.
     */
    private fun buildWebpushConfig(): WebpushConfig {
        return WebpushConfig.builder()
            .setNotification(
                WebpushNotification.builder()
                    .setIcon("/icons/icon-192x192.png")
                    .build()
            )
            .build()
    }

    /**
     * Maps [FirebaseMessagingException] error codes to the appropriate [PushResult].
     *
     * - UNREGISTERED / INVALID_ARGUMENT → token is invalid and should be removed
     * - UNAVAILABLE / INTERNAL → transient error, eligible for retry with backoff
     * - All other codes → permanent error, should not be retried
     */
    private fun handleMessagingException(token: String, e: FirebaseMessagingException): PushResult {
        return when (e.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT -> {
                logger.info("Invalid/unregistered FCM token detected ({}): {}", maskToken(token), e.messagingErrorCode)
                PushResult.InvalidToken(token)
            }

            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL -> {
                logger.warn("Transient FCM error for token {} ({}): {}", maskToken(token), e.messagingErrorCode, e.message)
                PushResult.TransientError(token, e.message ?: "Transient FCM error")
            }

            else -> {
                logger.error("Permanent FCM error for token {} ({}): {}", maskToken(token), e.messagingErrorCode, e.message)
                PushResult.PermanentError(token, e.message ?: "Permanent FCM error")
            }
        }
    }

    /**
     * Masks a device token for safe logging, showing only the first 8 and last 4 characters.
     */
    private fun maskToken(token: String): String {
        if (token.length <= 12) return "***"
        return "${token.take(8)}...${token.takeLast(4)}"
    }
}
