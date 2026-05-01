package com.worshiphub.domain.common

import java.time.LocalDateTime
import java.util.*

interface DomainEvent {
    val eventId: UUID
    val occurredAt: LocalDateTime
    val aggregateId: UUID
}

sealed class SongEvent : DomainEvent {
    data class SongCreated(
        override val eventId: UUID = UUID.randomUUID(),
        override val occurredAt: LocalDateTime = LocalDateTime.now(),
        override val aggregateId: UUID,
        val title: String,
        val artist: String,
        val churchId: UUID,
        val createdBy: UUID  // User who created the song
    ) : SongEvent()
}

sealed class ServiceEvent : DomainEvent {
    data class ServiceScheduled(
        override val eventId: UUID = UUID.randomUUID(),
        override val occurredAt: LocalDateTime = LocalDateTime.now(),
        override val aggregateId: UUID,
        val teamMembers: List<UUID>,
        val scheduledDate: LocalDateTime
    ) : ServiceEvent()
}