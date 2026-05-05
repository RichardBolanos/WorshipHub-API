package com.worshiphub.domain.collaboration.push

/**
 * Gateway interface for sending push notifications to devices via FCM.
 * Implementations reside in the infrastructure layer (e.g., FirebasePushGateway).
 *
 * Validates: Requirements 13.1, 13.3, 13.4, 13.7, 26.5
 */
interface PushGateway {
    /**
     * Sends a push notification to a single device.
     *
     * @param token The FCM device token
     * @param notification The push notification payload
     * @return The result of the send operation
     */
    fun sendToDevice(token: String, notification: PushPayload): PushResult

    /**
     * Sends a push notification to multiple devices.
     *
     * @param tokens List of FCM device tokens
     * @param notification The push notification payload
     * @return List of results, one per token in the same order
     */
    fun sendToDevices(tokens: List<String>, notification: PushPayload): List<PushResult>
}

/**
 * Payload for a push notification message.
 *
 * @property title Notification title displayed to the user
 * @property body Notification body text
 * @property data Custom key-value data map for deep linking and metadata (e.g., type, entityId)
 * @property channelId Android notification channel identifier (e.g., "services", "chat", "team", "songs")
 * @property badge iOS badge count to display on the app icon
 * @property sound Notification sound name; defaults to "default" for system sound on iOS/Android
 * @property category iOS notification category for quick actions (e.g., "SERVICE_ASSIGNMENT" for Accept/Decline)
 */
data class PushPayload(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val channelId: String? = null,
    val badge: Int? = null,
    val sound: String? = "default",
    val category: String? = null
)

/**
 * Sealed class representing the result of a push notification send attempt.
 * Used to determine follow-up actions such as token cleanup or retry.
 */
sealed class PushResult {

    /**
     * The notification was delivered successfully.
     *
     * @property messageId The FCM message identifier
     */
    data class Success(val messageId: String) : PushResult()

    /**
     * The device token is invalid or unregistered.
     * The token should be removed from the database.
     *
     * @property token The invalid FCM token
     */
    data class InvalidToken(val token: String) : PushResult()

    /**
     * A transient error occurred (e.g., FCM UNAVAILABLE or INTERNAL).
     * The send operation may be retried with exponential backoff.
     *
     * @property token The FCM token that failed
     * @property message Description of the transient error
     */
    data class TransientError(val token: String, val message: String) : PushResult()

    /**
     * A permanent error occurred that should not be retried.
     *
     * @property token The FCM token that failed
     * @property message Description of the permanent error
     */
    data class PermanentError(val token: String, val message: String) : PushResult()
}
