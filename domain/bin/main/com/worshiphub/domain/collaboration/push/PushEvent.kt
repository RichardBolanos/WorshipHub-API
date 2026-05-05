package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.NotificationType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Sealed class que representa todos los eventos de dominio que generan notificaciones push.
 * Cada subtipo contiene los datos específicos del evento y mapea a un [NotificationType].
 */
sealed class PushEvent {
    abstract val recipientUserIds: List<UUID>
    abstract val notificationType: NotificationType

    /**
     * Evento: un líder asigna miembros a un servicio de adoración.
     * Validates: Requirements 2.1
     */
    data class ServiceAssignment(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val scheduledDate: LocalDateTime,
        val roles: Map<UUID, String> // userId -> role
    ) : PushEvent() {
        override val notificationType = NotificationType.SERVICE_INVITATION
    }

    /**
     * Evento: un usuario envía un mensaje en el chat de un equipo.
     * Validates: Requirements 3.1
     */
    data class ChatMessage(
        override val recipientUserIds: List<UUID>,
        val senderName: String,
        val teamName: String,
        val messageExcerpt: String,
        val teamId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.TEAM_ASSIGNMENT
    }

    /**
     * Evento: un usuario agrega un comentario a una canción.
     * Validates: Requirements 4.1
     */
    data class SongComment(
        override val recipientUserIds: List<UUID>,
        val commenterName: String,
        val songTitle: String,
        val commentExcerpt: String,
        val songId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.NEW_COMMENT
    }

    /**
     * Evento: cambio en la composición de un equipo (miembro agregado, removido, cambio de líder o rol).
     * El [notificationType] varía según el tipo de cambio específico.
     * Validates: Requirements 5.1
     */
    data class TeamMemberChange(
        override val recipientUserIds: List<UUID>,
        val teamName: String,
        val changeDescription: String,
        val teamId: UUID,
        override val notificationType: NotificationType
    ) : PushEvent()

    /**
     * Evento: un miembro acepta o declina su asignación a un servicio.
     * Validates: Requirements 6.1
     */
    data class InvitationResponse(
        override val recipientUserIds: List<UUID>,
        val memberName: String,
        val serviceName: String,
        val scheduledDate: LocalDateTime,
        val accepted: Boolean
    ) : PushEvent() {
        override val notificationType = NotificationType.SERVICE_INVITATION
    }

    /**
     * Evento: se agrega una nueva canción al catálogo de la iglesia.
     * Validates: Requirements 7.1
     */
    data class NewSong(
        override val recipientUserIds: List<UUID>,
        val songTitle: String,
        val artist: String?,
        val addedByName: String,
        val songId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.NEW_SONG
    }

    /**
     * Evento: un administrador envía una invitación a un usuario existente.
     * Validates: Requirements 8.1
     */
    data class ChurchInvitation(
        override val recipientUserIds: List<UUID>,
        val churchName: String,
        val offeredRole: String
    ) : PushEvent() {
        override val notificationType = NotificationType.TEAM_ASSIGNMENT
    }

    /**
     * Evento: recordatorio de servicio próximo (24h o 2h antes).
     * Validates: Requirements 9.1
     */
    data class ServiceReminder(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val scheduledDate: LocalDateTime,
        val setlistName: String?,
        val hoursUntil: Int
    ) : PushEvent() {
        override val notificationType = NotificationType.SERVICE_SCHEDULED
    }

    /**
     * Evento: un líder modifica el setlist de un servicio futuro.
     * Validates: Requirements 10.1
     */
    data class SetlistModified(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val scheduledDate: LocalDateTime,
        val changeSummary: String,
        val serviceId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.SERVICE_SCHEDULED
    }

    /**
     * Evento: un líder cancela un servicio de adoración.
     * Validates: Requirements 16.1
     */
    data class ServiceCancelled(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val originalDate: LocalDateTime,
        val reason: String?
    ) : PushEvent() {
        override val notificationType = NotificationType.SERVICE_SCHEDULED
    }

    /**
     * Evento: se crea un servicio recurrente con instancias y miembros asignados.
     * Validates: Requirements 17.1
     */
    data class RecurringServiceCreated(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val scheduledDates: List<LocalDateTime>,
        val recurrencePattern: String, // "WEEKLY", "BIWEEKLY", "MONTHLY"
        val roles: Map<UUID, String> // userId -> role
    ) : PushEvent() {
        override val notificationType = NotificationType.RECURRING_SERVICE
    }

    /**
     * Evento: se actualiza la regla de recurrencia de un servicio recurrente.
     * Validates: Requirements 18.1
     */
    data class RecurrenceRuleUpdated(
        override val recipientUserIds: List<UUID>,
        val parentServiceName: String,
        val newRecurrencePattern: String,
        val affectedDates: List<LocalDateTime>,
        val removedDates: List<LocalDateTime> = emptyList()
    ) : PushEvent() {
        override val notificationType = NotificationType.RECURRING_SERVICE
    }

    /**
     * Evento: se elimina un servicio recurrente y todas sus instancias futuras.
     * Validates: Requirements 19.1
     */
    data class RecurringServiceDeleted(
        override val recipientUserIds: List<UUID>,
        val serviceName: String,
        val affectedDates: List<LocalDateTime>,
        val reason: String?
    ) : PushEvent() {
        override val notificationType = NotificationType.RECURRING_SERVICE
    }

    /**
     * Evento: se actualizan los detalles de una canción (tonalidad, BPM, letra, acordes).
     * Validates: Requirements 20.1
     */
    data class SongUpdated(
        override val recipientUserIds: List<UUID>,
        val songTitle: String,
        val changedFields: List<String>, // ["key", "bpm", "lyrics", "chords"]
        val updatedByName: String,
        val songId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.SONG_UPDATED
    }

    /**
     * Evento: se elimina una canción del catálogo.
     * Validates: Requirements 21.1
     */
    data class SongDeleted(
        override val recipientUserIds: List<UUID>,
        val songTitle: String,
        val deletedByName: String,
        val affectedSetlists: List<String>
    ) : PushEvent() {
        override val notificationType = NotificationType.SONG_DELETED
    }

    /**
     * Evento: se agrega un attachment a una canción (YouTube, PDF, Spotify, etc.).
     * Validates: Requirements 22.1
     */
    data class AttachmentAdded(
        override val recipientUserIds: List<UUID>,
        val songTitle: String,
        val attachmentType: String, // "YOUTUBE_LINK", "PDF_SHEET", etc.
        val addedByName: String,
        val songId: UUID
    ) : PushEvent() {
        override val notificationType = NotificationType.SONG_ATTACHMENT
    }

    /**
     * Evento: un usuario acepta una invitación a unirse a la iglesia.
     * Validates: Requirements 23.1
     */
    data class InvitationAccepted(
        override val recipientUserIds: List<UUID>,
        val newMemberName: String,
        val newMemberEmail: String,
        val acceptedRole: String
    ) : PushEvent() {
        override val notificationType = NotificationType.INVITATION_ACCEPTED
    }

    /**
     * Evento: un miembro marca una fecha como no disponible.
     * Validates: Requirements 24.1
     */
    data class MemberUnavailable(
        override val recipientUserIds: List<UUID>,
        val memberName: String,
        val unavailableDate: LocalDate,
        val reason: String?
    ) : PushEvent() {
        override val notificationType = NotificationType.AVAILABILITY_CHANGE
    }

    /**
     * Evento: un miembro elimina su registro de indisponibilidad.
     * Validates: Requirements 25.1
     */
    data class MemberAvailableAgain(
        override val recipientUserIds: List<UUID>,
        val memberName: String,
        val previouslyUnavailableDate: LocalDate
    ) : PushEvent() {
        override val notificationType = NotificationType.AVAILABILITY_CHANGE
    }
}
