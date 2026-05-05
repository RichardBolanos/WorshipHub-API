package com.worshiphub.api.config

import com.worshiphub.domain.common.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}

@Service
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {
    
    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}

@Component
class DomainEventHandler(
    private val notificationApplicationService: com.worshiphub.application.notification.NotificationApplicationService
) {
    
    @EventListener
    fun handleSongCreated(event: com.worshiphub.domain.common.SongEvent.SongCreated) {
        // Notify the user who created the song that it was successfully added
        notificationApplicationService.sendNotification(
            userId = event.createdBy,
            title = "Nueva canción agregada",
            message = "${event.title} por ${event.artist} ha sido agregada al catálogo",
            type = com.worshiphub.domain.collaboration.NotificationType.SONG_ADDED
        )
    }
    
    @EventListener
    fun handleServiceScheduled(event: com.worshiphub.domain.common.ServiceEvent.ServiceScheduled) {
        // Notify all team members assigned to the service
        event.teamMembers.forEach { memberId ->
            notificationApplicationService.sendNotification(
                userId = memberId,
                title = "Servicio programado",
                message = "Has sido asignado al servicio del ${event.scheduledDate}",
                type = com.worshiphub.domain.collaboration.NotificationType.SERVICE_SCHEDULED
            )
        }
    }
}