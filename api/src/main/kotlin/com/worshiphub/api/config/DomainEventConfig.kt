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
class DomainEventHandler {
    
    @EventListener
    fun handleSongCreated(event: com.worshiphub.domain.common.SongEvent.SongCreated) {
        // TODO: Send notification to team members
        println("Song created: ${event.title} by ${event.artist}")
    }
    
    @EventListener
    fun handleServiceScheduled(event: com.worshiphub.domain.common.ServiceEvent.ServiceScheduled) {
        // TODO: Send notifications to team members
        println("Service scheduled for ${event.scheduledDate} with ${event.teamMembers.size} members")
    }
}