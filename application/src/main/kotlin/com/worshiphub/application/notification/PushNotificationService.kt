package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.*
import com.worshiphub.domain.collaboration.repository.DeviceTokenRepository
import com.worshiphub.domain.collaboration.repository.NotificationPreferenceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Servicio central que orquesta el envío de notificaciones push.
 *
 * Recibe un [PushEvent], filtra destinatarios por rol de usuario ([RoleNotificationFilter]),
 * guarda la notificación in-app (siempre), verifica preferencias del usuario,
 * obtiene tokens de dispositivos, y delega el envío al [PushGateway].
 *
 * Flujo por cada recipientUserId:
 * 1. Filtrar por rol usando [RoleNotificationFilter.filterByRole]
 * 2. Guardar notificación in-app (siempre, para usuarios que pasan filtro de rol)
 * 3. Verificar preferencias del usuario
 * 4. Obtener tokens y enviar push
 * 5. Limpiar tokens inválidos
 *
 * Incluye lógica de reintento con backoff exponencial para errores transitorios
 * (máx 3 reintentos: 1s, 2s, 4s).
 *
 * Validates: Requirements 11.2, 11.3, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 30.1, 30.2, 30.4
 */
@Service
open class PushNotificationService(
    private val pushGateway: PushGateway,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val notificationApplicationService: NotificationApplicationService,
    private val userRoleResolver: UserRoleResolver
) {

    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        private val DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    /**
     * Procesa un evento push: filtra por rol, guarda in-app, verifica preferencias,
     * envía push y limpia tokens inválidos.
     *
     * Se ejecuta de forma asíncrona en el thread pool `pushNotificationExecutor`.
     */
    @Async("pushNotificationExecutor")
    open fun processPushEvent(event: PushEvent) {
        try {
            // 1. Filtrar destinatarios por rol (Mapa_Notificaciones_Rol)
            val roleFilteredRecipients = RoleNotificationFilter.filterByRole(
                userIds = event.recipientUserIds,
                notificationType = event.notificationType,
                roleResolver = { userId -> userRoleResolver.resolveEffectiveRole(userId) }
            )

            logger.debug(
                "Processing push event {} for {} recipients (filtered from {})",
                event::class.simpleName,
                roleFilteredRecipients.size,
                event.recipientUserIds.size
            )

            for (userId in roleFilteredRecipients) {
                try {
                    processForUser(event, userId)
                } catch (e: Exception) {
                    logger.error("Error processing push event for user {}: {}", userId, e.message, e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing push event {}: {}", event::class.simpleName, e.message, e)
        }
    }

    private fun processForUser(event: PushEvent, userId: UUID) {
        val payload = event.toPayload(userId)

        // 2. Siempre guardar notificación in-app
        val entityInfo = extractEntityInfo(event)
        notificationApplicationService.sendNotification(
            userId = userId,
            title = payload.title,
            message = payload.body,
            type = event.notificationType,
            relatedEntityId = entityInfo.first,
            relatedEntityType = entityInfo.second
        )

        // 3. Verificar preferencias antes de enviar push
        val prefs = notificationPreferenceRepository.findByUserIdOrDefault(userId)
        if (!prefs.isEnabled(event.notificationType)) {
            logger.debug("Push disabled for user {} and type {}", userId, event.notificationType)
            return
        }

        // 4. Obtener tokens y enviar
        val tokens = deviceTokenRepository.findByUserId(userId)
        if (tokens.isEmpty()) {
            logger.debug("No device tokens found for user {}", userId)
            return
        }

        val results = sendWithRetry(tokens.map { it.token }, payload)

        // 5. Limpiar tokens inválidos
        results.filterIsInstance<PushResult.InvalidToken>()
            .forEach { result ->
                logger.info("Removing invalid token for user {}: {}", userId, result.token.take(20))
                deviceTokenRepository.deleteByToken(result.token)
            }
    }

    /**
     * Envía push a una lista de tokens con lógica de reintento con backoff exponencial
     * para errores transitorios. Máximo 3 reintentos: 1s, 2s, 4s.
     */
    private fun sendWithRetry(tokens: List<String>, payload: PushPayload): List<PushResult> {
        val finalResults = mutableMapOf<String, PushResult>()
        var tokensToRetry = tokens

        for (attempt in 0..MAX_RETRIES) {
            if (tokensToRetry.isEmpty()) break

            val results = pushGateway.sendToDevices(tokensToRetry, payload)

            val nextRetryTokens = mutableListOf<String>()

            results.forEachIndexed { index, result ->
                val token = tokensToRetry[index]
                when (result) {
                    is PushResult.TransientError -> {
                        if (attempt < MAX_RETRIES) {
                            nextRetryTokens.add(token)
                            logger.warn(
                                "Transient error for token {} (attempt {}/{}): {}",
                                token.take(20), attempt + 1, MAX_RETRIES, result.message
                            )
                        } else {
                            finalResults[token] = result
                            logger.error(
                                "Max retries reached for token {}: {}",
                                token.take(20), result.message
                            )
                        }
                    }
                    else -> finalResults[token] = result
                }
            }

            tokensToRetry = nextRetryTokens

            if (tokensToRetry.isNotEmpty() && attempt < MAX_RETRIES) {
                val backoffMs = INITIAL_BACKOFF_MS * (1L shl attempt) // 1s, 2s, 4s
                try {
                    Thread.sleep(backoffMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    logger.warn("Retry sleep interrupted, aborting retries")
                    tokensToRetry.forEach { token ->
                        finalResults[token] = PushResult.TransientError(token, "Retry interrupted")
                    }
                    break
                }
            }
        }

        return finalResults.values.toList()
    }

    /**
     * Extrae la información de entidad relacionada para deep linking.
     */
    private fun extractEntityInfo(event: PushEvent): Pair<UUID?, String?> = when (event) {
        is PushEvent.ServiceAssignment -> null to "SERVICE"
        is PushEvent.ChatMessage -> event.teamId to "TEAM"
        is PushEvent.SongComment -> event.songId to "SONG"
        is PushEvent.TeamMemberChange -> event.teamId to "TEAM"
        is PushEvent.InvitationResponse -> null to "SERVICE"
        is PushEvent.NewSong -> event.songId to "SONG"
        is PushEvent.ChurchInvitation -> null to null
        is PushEvent.ServiceReminder -> null to "SERVICE"
        is PushEvent.SetlistModified -> event.serviceId to "SERVICE"
        is PushEvent.ServiceCancelled -> null to null
        is PushEvent.RecurringServiceCreated -> null to "SERVICE"
        is PushEvent.RecurrenceRuleUpdated -> null to "SERVICE"
        is PushEvent.RecurringServiceDeleted -> null to null
        is PushEvent.SongUpdated -> event.songId to "SONG"
        is PushEvent.SongDeleted -> null to null
        is PushEvent.AttachmentAdded -> event.songId to "SONG"
        is PushEvent.InvitationAccepted -> null to null
        is PushEvent.MemberUnavailable -> null to "AVAILABILITY"
        is PushEvent.MemberAvailableAgain -> null to "AVAILABILITY"
    }

    /**
     * Genera un [PushPayload] a partir de un [PushEvent] para un usuario específico.
     *
     * Incluye título y cuerpo descriptivos para cada tipo de evento,
     * data map con type y entityId para deep linking,
     * channelId para Android y category para iOS.
     *
     * Mapeo de channelId:
     * - "services" para eventos de servicio
     * - "chat" para eventos de chat
     * - "team" para eventos de equipo
     * - "songs" para eventos de canciones
     *
     * Category iOS: solo "SERVICE_ASSIGNMENT" para ServiceAssignment.
     */
    private fun PushEvent.toPayload(userId: UUID): PushPayload = when (this) {
        is PushEvent.ServiceAssignment -> {
            val roleName = roles[userId] ?: "miembro"
            PushPayload(
                title = "Asignación a servicio",
                body = "Has sido asignado como $roleName en \"$serviceName\" el ${scheduledDate.format(DATE_FORMATTER)}",
                data = buildDataMap("SERVICE_INVITATION", null),
                channelId = "services",
                category = "SERVICE_ASSIGNMENT"
            )
        }

        is PushEvent.ChatMessage -> PushPayload(
            title = "$senderName en $teamName",
            body = messageExcerpt,
            data = buildDataMap("CHAT_MESSAGE", teamId) + mapOf(
                "silent" to "true",           // Signals frontend to refresh chat silently if screen is active
                "senderName" to senderName,
                "teamName" to teamName
            ),
            channelId = "chat"
        )

        is PushEvent.SongComment -> PushPayload(
            title = "Nuevo comentario en \"$songTitle\"",
            body = "$commenterName: $commentExcerpt",
            data = buildDataMap("NEW_COMMENT", songId),
            channelId = "songs"
        )

        is PushEvent.TeamMemberChange -> PushPayload(
            title = "Cambio en equipo \"$teamName\"",
            body = changeDescription,
            data = buildDataMap(notificationType.name, teamId),
            channelId = "team"
        )

        is PushEvent.InvitationResponse -> {
            val status = if (accepted) "aceptó" else "declinó"
            PushPayload(
                title = "Respuesta a asignación",
                body = "$memberName $status la asignación para \"$serviceName\" el ${scheduledDate.format(DATE_FORMATTER)}",
                data = buildDataMap("INVITATION_RESPONSE", null),
                channelId = "services"
            )
        }

        is PushEvent.NewSong -> {
            val artistInfo = if (artist != null) " de $artist" else ""
            PushPayload(
                title = "Nueva canción agregada",
                body = "$addedByName agregó \"$songTitle\"$artistInfo al catálogo",
                data = buildDataMap("NEW_SONG", songId),
                channelId = "songs"
            )
        }

        is PushEvent.ChurchInvitation -> PushPayload(
            title = "Invitación a iglesia",
            body = "Has sido invitado a unirte a \"$churchName\" como $offeredRole",
            data = buildDataMap("CHURCH_INVITATION", null),
            channelId = "team"
        )

        is PushEvent.ServiceReminder -> {
            val setlistInfo = if (setlistName != null) " con setlist \"$setlistName\"" else ""
            PushPayload(
                title = "Recordatorio de servicio",
                body = "\"$serviceName\" comienza en ${hoursUntil}h$setlistInfo",
                data = buildDataMap("SERVICE_SCHEDULED", null),
                channelId = "services"
            )
        }

        is PushEvent.SetlistModified -> PushPayload(
            title = "Setlist modificado",
            body = "El setlist de \"$serviceName\" (${scheduledDate.format(DATE_FORMATTER)}) fue actualizado: $changeSummary",
            data = buildDataMap("SETLIST_MODIFIED", serviceId),
            channelId = "services"
        )

        is PushEvent.ServiceCancelled -> {
            val reasonInfo = if (reason != null) ": $reason" else ""
            PushPayload(
                title = "Servicio cancelado",
                body = "\"$serviceName\" del ${originalDate.format(DATE_FORMATTER)} ha sido cancelado$reasonInfo",
                data = buildDataMap("SERVICE_CANCELLED", null),
                channelId = "services"
            )
        }

        is PushEvent.RecurringServiceCreated -> {
            val roleName = roles[userId] ?: "miembro"
            val patternText = when (recurrencePattern) {
                "WEEKLY" -> "semanal"
                "BIWEEKLY" -> "quincenal"
                "MONTHLY" -> "mensual"
                else -> recurrencePattern.lowercase()
            }
            PushPayload(
                title = "Servicio recurrente creado",
                body = "\"$serviceName\" ($patternText) — asignado como $roleName con ${scheduledDates.size} instancias",
                data = buildDataMap("RECURRING_SERVICE", null),
                channelId = "services"
            )
        }

        is PushEvent.RecurrenceRuleUpdated -> {
            val patternText = when (newRecurrencePattern) {
                "WEEKLY" -> "semanal"
                "BIWEEKLY" -> "quincenal"
                "MONTHLY" -> "mensual"
                else -> newRecurrencePattern.lowercase()
            }
            PushPayload(
                title = "Recurrencia actualizada",
                body = "\"$parentServiceName\" cambió a $patternText — ${affectedDates.size} instancias afectadas",
                data = buildDataMap("RECURRING_SERVICE", null),
                channelId = "services"
            )
        }

        is PushEvent.RecurringServiceDeleted -> {
            val reasonInfo = if (reason != null) ": $reason" else ""
            PushPayload(
                title = "Servicio recurrente eliminado",
                body = "\"$serviceName\" fue eliminado (${affectedDates.size} instancias canceladas)$reasonInfo",
                data = buildDataMap("RECURRING_SERVICE", null),
                channelId = "services"
            )
        }

        is PushEvent.SongUpdated -> {
            val fieldsText = changedFields.joinToString(", ")
            PushPayload(
                title = "Canción actualizada",
                body = "$updatedByName actualizó \"$songTitle\" ($fieldsText)",
                data = buildDataMap("SONG_UPDATED", songId),
                channelId = "songs"
            )
        }

        is PushEvent.SongDeleted -> {
            val setlistsInfo = if (affectedSetlists.isNotEmpty()) {
                " — afecta setlists: ${affectedSetlists.joinToString(", ")}"
            } else ""
            PushPayload(
                title = "Canción eliminada",
                body = "$deletedByName eliminó \"$songTitle\"$setlistsInfo",
                data = buildDataMap("SONG_DELETED", null),
                channelId = "songs"
            )
        }

        is PushEvent.AttachmentAdded -> PushPayload(
            title = "Nuevo adjunto en canción",
            body = "$addedByName agregó un $attachmentType a \"$songTitle\"",
            data = buildDataMap("SONG_ATTACHMENT", songId),
            channelId = "songs"
        )

        is PushEvent.InvitationAccepted -> PushPayload(
            title = "Invitación aceptada",
            body = "$newMemberName ($newMemberEmail) aceptó la invitación como $acceptedRole",
            data = buildDataMap("INVITATION_ACCEPTED", null),
            channelId = "team"
        )

        is PushEvent.MemberUnavailable -> {
            val reasonInfo = if (reason != null) " — Motivo: $reason" else ""
            PushPayload(
                title = "Miembro no disponible",
                body = "$memberName no estará disponible el ${unavailableDate.format(DATE_ONLY_FORMATTER)}$reasonInfo",
                data = buildDataMap("AVAILABILITY_CHANGE", null),
                channelId = "team"
            )
        }

        is PushEvent.MemberAvailableAgain -> PushPayload(
            title = "Miembro disponible nuevamente",
            body = "$memberName volvió a estar disponible para el ${previouslyUnavailableDate.format(DATE_ONLY_FORMATTER)}",
            data = buildDataMap("AVAILABILITY_CHANGE", null),
            channelId = "team"
        )
    }

    /**
     * Construye el data map para deep linking con type y entityId opcionales.
     */
    private fun buildDataMap(type: String, entityId: UUID?): Map<String, String> {
        val map = mutableMapOf("type" to type)
        if (entityId != null) {
            map["entityId"] = entityId.toString()
        }
        return map
    }
}
