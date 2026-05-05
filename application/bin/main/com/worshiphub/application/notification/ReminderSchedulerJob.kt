package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.PushEvent
import com.worshiphub.domain.scheduling.ConfirmationStatus
import com.worshiphub.domain.scheduling.ServiceEvent
import com.worshiphub.domain.scheduling.ServiceEventStatus
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Job programado que busca servicios próximos y genera eventos de recordatorio push.
 *
 * Se ejecuta cada 15 minutos y busca servicios con estado PUBLISHED o CONFIRMED
 * en las próximas 24h y 2h. Genera [PushEvent.ServiceReminder] solo para miembros
 * con estado ACCEPTED. Trackea recordatorios ya enviados para evitar duplicados
 * usando un [ConcurrentHashMap] en memoria con limpieza periódica.
 *
 * Validates: Requirements 9.1, 9.2, 9.3
 */
@Component
class ReminderSchedulerJob(
    private val serviceEventRepository: ServiceEventRepository,
    private val setlistRepository: SetlistRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(ReminderSchedulerJob::class.java)

    /**
     * Tracks sent reminders to avoid duplicates.
     * Key format: "serviceId:userId:hoursUntil"
     * Value: timestamp when the reminder was sent.
     */
    private val sentReminders = ConcurrentHashMap<String, Long>()

    companion object {
        private val ACTIVE_STATUSES = listOf(ServiceEventStatus.PUBLISHED, ServiceEventStatus.CONFIRMED)
        private const val REMINDER_24H = 24
        private const val REMINDER_2H = 2
        /** Tolerance window in minutes for matching reminder thresholds. */
        private const val WINDOW_MINUTES = 15L
        /** Entries older than 48 hours are cleaned up. */
        private const val CLEANUP_THRESHOLD_MS = 48 * 60 * 60 * 1000L
    }

    /**
     * Checks for upcoming services every 15 minutes and publishes reminder events.
     *
     * For each service with status PUBLISHED or CONFIRMED scheduled within the next 24h:
     * - If within 24h window → sends 24h reminder
     * - If within 2h window → sends 2h reminder
     *
     * Only members with ACCEPTED confirmation status receive reminders.
     * Duplicate reminders are prevented via in-memory tracking.
     */
    @Scheduled(fixedRate = 900_000)
    fun checkUpcomingServices() {
        try {
            val now = LocalDateTime.now()
            // Query services in the next 24h + window buffer
            val endDate = now.plusHours(REMINDER_24H.toLong()).plusMinutes(WINDOW_MINUTES)
            val upcomingServices = serviceEventRepository.findByStatusesAndDateRange(
                ACTIVE_STATUSES, now, endDate
            )

            logger.debug("Found {} upcoming services to check for reminders", upcomingServices.size)

            for (service in upcomingServices) {
                processServiceReminders(service, now)
            }

            cleanupOldEntries()
        } catch (e: Exception) {
            logger.error("Error checking upcoming services for reminders: {}", e.message, e)
        }
    }

    private fun processServiceReminders(service: ServiceEvent, now: LocalDateTime) {
        val hoursUntil = java.time.Duration.between(now, service.scheduledDate).toMinutes() / 60.0

        // Check 24h reminder: service is between now and 24h+window from now
        if (hoursUntil <= REMINDER_24H && hoursUntil > REMINDER_2H + WINDOW_MINUTES / 60.0) {
            publishReminderIfNotSent(service, REMINDER_24H)
        }

        // Check 2h reminder: service is within 2h+window from now
        if (hoursUntil <= REMINDER_2H) {
            publishReminderIfNotSent(service, REMINDER_2H)
        }
    }

    private fun publishReminderIfNotSent(service: ServiceEvent, hoursUntil: Int) {
        // Get accepted members only
        val acceptedMembers = service.assignedMembers
            .filter { it.confirmationStatus == ConfirmationStatus.ACCEPTED }

        if (acceptedMembers.isEmpty()) {
            return
        }

        // Filter out members who already received this reminder
        val recipientUserIds = acceptedMembers
            .map { it.userId }
            .filter { userId ->
                val key = buildReminderKey(service.id, userId, hoursUntil)
                !sentReminders.containsKey(key)
            }

        if (recipientUserIds.isEmpty()) {
            return
        }

        // Resolve setlist name if available
        val setlistName = service.setlistId?.let { setlistId ->
            try {
                setlistRepository.findById(setlistId)?.name
            } catch (e: Exception) {
                logger.warn("Could not resolve setlist name for id {}: {}", setlistId, e.message)
                null
            }
        }

        val event = PushEvent.ServiceReminder(
            recipientUserIds = recipientUserIds,
            serviceName = service.name,
            scheduledDate = service.scheduledDate,
            setlistName = setlistName,
            hoursUntil = hoursUntil
        )

        applicationEventPublisher.publishEvent(event)

        // Mark reminders as sent
        val now = System.currentTimeMillis()
        for (userId in recipientUserIds) {
            val key = buildReminderKey(service.id, userId, hoursUntil)
            sentReminders[key] = now
        }

        logger.info(
            "Published {}h reminder for service '{}' (id={}) to {} members",
            hoursUntil, service.name, service.id, recipientUserIds.size
        )
    }

    private fun buildReminderKey(serviceId: UUID, userId: UUID, hoursUntil: Int): String =
        "$serviceId:$userId:$hoursUntil"

    /**
     * Removes entries older than 48 hours to prevent unbounded memory growth.
     */
    private fun cleanupOldEntries() {
        val cutoff = System.currentTimeMillis() - CLEANUP_THRESHOLD_MS
        val removedCount = sentReminders.entries.removeAll { it.value < cutoff }
        if (removedCount) {
            logger.debug("Cleaned up old reminder tracking entries")
        }
    }
}
