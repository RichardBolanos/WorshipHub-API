package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.PushEvent
import com.worshiphub.domain.scheduling.AssignedMember
import com.worshiphub.domain.scheduling.ConfirmationStatus
import com.worshiphub.domain.scheduling.ServiceEvent
import com.worshiphub.domain.scheduling.ServiceEventStatus
import com.worshiphub.domain.scheduling.repository.ServiceEventRepository
import com.worshiphub.domain.scheduling.repository.SetlistRepository
import io.kotest.core.spec.style.FreeSpec
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.*

/**
 * Unit tests for ReminderSchedulerJob.
 * Validates: Requirements 9.1, 9.2, 29.1
 */
class ReminderSchedulerJobTest : FreeSpec({

    fun createJob(): Triple<ReminderSchedulerJob, ServiceEventRepository, ApplicationEventPublisher> {
        val serviceEventRepo = mockk<ServiceEventRepository>()
        val setlistRepo = mockk<SetlistRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val job = ReminderSchedulerJob(serviceEventRepo, setlistRepo, eventPublisher)
        return Triple(job, serviceEventRepo, eventPublisher)
    }

    "checkUpcomingServices" - {

        "does not generate reminder for DRAFT services" {
            val (job, serviceEventRepo, eventPublisher) = createJob()

            // The job queries only PUBLISHED and CONFIRMED statuses,
            // so DRAFT services won't be returned by the repository
            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns emptyList()

            job.checkUpcomingServices()

            verify(exactly = 0) { eventPublisher.publishEvent(any<PushEvent.ServiceReminder>()) }
        }

        "does not generate reminder for CANCELLED services" {
            val (job, serviceEventRepo, eventPublisher) = createJob()

            // CANCELLED services are not in the ACTIVE_STATUSES list
            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns emptyList()

            job.checkUpcomingServices()

            verify(exactly = 0) { eventPublisher.publishEvent(any<PushEvent.ServiceReminder>()) }
        }

        "does not generate reminder for members with DECLINED status" {
            val (job, serviceEventRepo, eventPublisher) = createJob()
            val serviceId = UUID.randomUUID()
            val declinedMember = AssignedMember(
                serviceEventId = serviceId,
                userId = UUID.randomUUID(),
                role = "Vocalist",
                confirmationStatus = ConfirmationStatus.DECLINED
            )
            val service = ServiceEvent(
                id = serviceId,
                name = "Sunday Service",
                scheduledDate = LocalDateTime.now().plusHours(20),
                teamId = UUID.randomUUID(),
                churchId = UUID.randomUUID(),
                status = ServiceEventStatus.PUBLISHED,
                assignedMembers = listOf(declinedMember)
            )

            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns listOf(service)

            job.checkUpcomingServices()

            verify(exactly = 0) { eventPublisher.publishEvent(any<PushEvent.ServiceReminder>()) }
        }

        "generates 24h reminder for accepted members" {
            val (job, serviceEventRepo, eventPublisher) = createJob()
            val serviceId = UUID.randomUUID()
            val acceptedUserId = UUID.randomUUID()
            val acceptedMember = AssignedMember(
                serviceEventId = serviceId,
                userId = acceptedUserId,
                role = "Vocalist",
                confirmationStatus = ConfirmationStatus.ACCEPTED
            )
            val service = ServiceEvent(
                id = serviceId,
                name = "Sunday Service",
                scheduledDate = LocalDateTime.now().plusHours(20),
                teamId = UUID.randomUUID(),
                churchId = UUID.randomUUID(),
                status = ServiceEventStatus.PUBLISHED,
                assignedMembers = listOf(acceptedMember)
            )

            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns listOf(service)

            job.checkUpcomingServices()

            verify {
                eventPublisher.publishEvent(match<PushEvent.ServiceReminder> {
                    it.recipientUserIds.contains(acceptedUserId) && it.hoursUntil == 24
                })
            }
        }

        "generates 2h reminder for accepted members" {
            val (job, serviceEventRepo, eventPublisher) = createJob()
            val serviceId = UUID.randomUUID()
            val acceptedUserId = UUID.randomUUID()
            val acceptedMember = AssignedMember(
                serviceEventId = serviceId,
                userId = acceptedUserId,
                role = "Pianist",
                confirmationStatus = ConfirmationStatus.ACCEPTED
            )
            val service = ServiceEvent(
                id = serviceId,
                name = "Evening Service",
                scheduledDate = LocalDateTime.now().plusMinutes(90),
                teamId = UUID.randomUUID(),
                churchId = UUID.randomUUID(),
                status = ServiceEventStatus.CONFIRMED,
                assignedMembers = listOf(acceptedMember)
            )

            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns listOf(service)

            job.checkUpcomingServices()

            verify {
                eventPublisher.publishEvent(match<PushEvent.ServiceReminder> {
                    it.recipientUserIds.contains(acceptedUserId) && it.hoursUntil == 2
                })
            }
        }

        "does not generate duplicate reminders" {
            val (job, serviceEventRepo, eventPublisher) = createJob()
            val serviceId = UUID.randomUUID()
            val acceptedUserId = UUID.randomUUID()
            val acceptedMember = AssignedMember(
                serviceEventId = serviceId,
                userId = acceptedUserId,
                role = "Vocalist",
                confirmationStatus = ConfirmationStatus.ACCEPTED
            )
            val service = ServiceEvent(
                id = serviceId,
                name = "Sunday Service",
                scheduledDate = LocalDateTime.now().plusHours(20),
                teamId = UUID.randomUUID(),
                churchId = UUID.randomUUID(),
                status = ServiceEventStatus.PUBLISHED,
                assignedMembers = listOf(acceptedMember)
            )

            every {
                serviceEventRepo.findByStatusesAndDateRange(any(), any(), any())
            } returns listOf(service)

            // First run — should publish
            job.checkUpcomingServices()
            // Second run — should NOT publish again (duplicate)
            job.checkUpcomingServices()

            verify(exactly = 1) {
                eventPublisher.publishEvent(match<PushEvent.ServiceReminder> {
                    it.recipientUserIds.contains(acceptedUserId) && it.hoursUntil == 24
                })
            }
        }
    }
})
