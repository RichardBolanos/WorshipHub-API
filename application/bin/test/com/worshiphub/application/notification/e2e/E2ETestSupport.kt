package com.worshiphub.application.notification.e2e

import com.worshiphub.application.notification.*
import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import io.mockk.*
import java.util.*

/**
 * Shared test infrastructure for push notification E2E tests.
 *
 * Provides [buildContext], [registerToken], [enableAllPreferences], and [disablePreference]
 * helpers so each individual test file stays focused on its scenario.
 */

/**
 * Test context holding all mocks and the service under test for E2E scenarios.
 */
data class E2ETestContext(
    val service: PushNotificationService,
    val gateway: MockPushGateway,
    val deviceTokenRepo: DeviceTokenRepository,
    val prefRepo: NotificationPreferenceRepository,
    val notificationAppService: NotificationApplicationService,
    val userRoleResolver: UserRoleResolver
)

/**
 * Builds a fresh [E2ETestContext] with all mocks wired up.
 * By default every user resolves to [UserRole.ADMIN] (receives everything).
 */
fun buildContext(
    roleOverride: ((UUID) -> UserRole)? = null
): E2ETestContext {
    val mockGateway = MockPushGateway()
    val deviceTokenRepo = mockk<DeviceTokenRepository>()
    val prefRepo = mockk<NotificationPreferenceRepository>()
    val notificationAppService = mockk<NotificationApplicationService>(relaxed = true)
    val userRoleResolver = mockk<UserRoleResolver>()

    if (roleOverride != null) {
        every { userRoleResolver.resolveEffectiveRole(any()) } answers { roleOverride(firstArg()) }
    } else {
        every { userRoleResolver.resolveEffectiveRole(any()) } returns UserRole.ADMIN
    }

    val service = PushNotificationService(
        mockGateway, deviceTokenRepo, prefRepo, notificationAppService, userRoleResolver
    )

    return E2ETestContext(
        service = service,
        gateway = mockGateway,
        deviceTokenRepo = deviceTokenRepo,
        prefRepo = prefRepo,
        notificationAppService = notificationAppService,
        userRoleResolver = userRoleResolver
    )
}

/** Register a device token for a user in the mock repo. */
fun E2ETestContext.registerToken(
    userId: UUID,
    token: String = "fcm-token-${userId.toString().take(8)}",
    platform: DevicePlatform = DevicePlatform.ANDROID
): DeviceToken {
    val deviceToken = DeviceToken(userId = userId, token = token, platform = platform)
    every { deviceTokenRepo.findByUserId(userId) } returns listOf(deviceToken)
    every { deviceTokenRepo.deleteByToken(any()) } just Runs
    return deviceToken
}

/** Set default preferences (all enabled) for a user. */
fun E2ETestContext.enableAllPreferences(userId: UUID): NotificationPreference {
    val prefs = NotificationPreference(userId = userId)
    every { prefRepo.findByUserIdOrDefault(userId) } returns prefs
    return prefs
}

/** Set preferences with specific types disabled. */
fun E2ETestContext.disablePreference(
    userId: UUID,
    chatMessages: Boolean = true,
    serviceAssignments: Boolean = true,
    songComments: Boolean = true,
    teamChanges: Boolean = true,
    newSongs: Boolean = true,
    serviceReminders: Boolean = true,
    invitationResponses: Boolean = true,
    setlistChanges: Boolean = true,
    serviceCancellations: Boolean = true,
    recurringServices: Boolean = true,
    songUpdates: Boolean = true,
    songDeletions: Boolean = true,
    songAttachments: Boolean = true,
    invitationAccepted: Boolean = true,
    availabilityChanges: Boolean = true
): NotificationPreference {
    val prefs = NotificationPreference(
        userId = userId,
        chatMessages = chatMessages,
        serviceAssignments = serviceAssignments,
        songComments = songComments,
        teamChanges = teamChanges,
        newSongs = newSongs,
        serviceReminders = serviceReminders,
        invitationResponses = invitationResponses,
        setlistChanges = setlistChanges,
        serviceCancellations = serviceCancellations,
        recurringServices = recurringServices,
        songUpdates = songUpdates,
        songDeletions = songDeletions,
        songAttachments = songAttachments,
        invitationAccepted = invitationAccepted,
        availabilityChanges = availabilityChanges
    )
    every { prefRepo.findByUserIdOrDefault(userId) } returns prefs
    return prefs
}
