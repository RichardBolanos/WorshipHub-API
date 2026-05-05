package com.worshiphub.domain.collaboration.push

import com.worshiphub.domain.collaboration.NotificationType
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica del Mapa_Notificaciones_Rol.
 *
 * Define qué tipos de notificación son aplicables a cada rol de usuario
 * y proporciona métodos para filtrar destinatarios y determinar tipos
 * visibles en preferencias.
 *
 * - Admin: Todos los tipos (R2–R10, R16–R25)
 * - Líder_Equipo: R2, R3, R4, R5, R6, R7, R9, R10, R16, R17–R19, R20, R21, R22, R24, R25
 * - Miembro: R2, R3, R4, R7, R9, R10, R16, R17–R19, R20, R21, R22
 */
object RoleNotificationFilter {

    /**
     * Mapa_Notificaciones_Rol: define qué tipos de notificación son aplicables a cada rol.
     */
    private val roleNotificationMap: Map<UserRole, Set<NotificationType>> = mapOf(
        UserRole.ADMIN to NotificationType.entries.toSet(),

        UserRole.TEAM_LEADER to setOf(
            NotificationType.SERVICE_INVITATION,       // R2
            NotificationType.CHAT_MESSAGE,             // R3
            NotificationType.NEW_COMMENT,              // R4
            NotificationType.TEAM_MEMBER_ADDED,        // R5
            NotificationType.TEAM_MEMBER_REMOVED,      // R5
            NotificationType.TEAM_LEADER_CHANGED,      // R5
            NotificationType.TEAM_ROLE_CHANGED,        // R5
            NotificationType.TEAM_ASSIGNMENT,          // R5/R6
            NotificationType.NEW_SONG,                 // R7
            NotificationType.SERVICE_SCHEDULED,        // R9/R10
            NotificationType.SERVICE_CANCELLED,        // R16
            NotificationType.RECURRING_SERVICE,        // R17–R19
            NotificationType.SONG_UPDATED,             // R20
            NotificationType.SONG_DELETED,             // R21
            NotificationType.SONG_ATTACHMENT,          // R22
            NotificationType.AVAILABILITY_CHANGE,      // R24, R25
        ),

        UserRole.MEMBER to setOf(
            NotificationType.SERVICE_INVITATION,       // R2
            NotificationType.CHAT_MESSAGE,             // R3
            NotificationType.NEW_COMMENT,              // R4
            NotificationType.NEW_SONG,                 // R7
            NotificationType.SERVICE_SCHEDULED,        // R9/R10
            NotificationType.SERVICE_CANCELLED,        // R16
            NotificationType.RECURRING_SERVICE,        // R17–R19
            NotificationType.SONG_UPDATED,             // R20
            NotificationType.SONG_DELETED,             // R21
            NotificationType.SONG_ATTACHMENT,          // R22
        )
    )

    /**
     * Determina si un tipo de notificación es aplicable para un rol dado.
     *
     * @param notificationType Tipo de notificación a verificar
     * @param role Rol del usuario
     * @return `true` si el tipo de notificación es aplicable para el rol
     */
    fun isApplicableForRole(notificationType: NotificationType, role: UserRole): Boolean =
        roleNotificationMap[role]?.contains(notificationType) ?: false

    /**
     * Retorna el conjunto de tipos de notificación aplicables para un rol dado.
     *
     * @param role Rol del usuario
     * @return Conjunto de tipos de notificación aplicables, o vacío si el rol no está mapeado
     */
    fun getApplicableTypes(role: UserRole): Set<NotificationType> =
        roleNotificationMap[role] ?: emptySet()

    /**
     * Filtra una lista de userIds, eliminando aquellos cuyo rol no permite
     * recibir el tipo de notificación dado.
     *
     * @param userIds Lista de IDs de usuarios candidatos
     * @param notificationType Tipo de notificación a enviar
     * @param roleResolver Función que resuelve el rol efectivo (mayor jerarquía) de un usuario
     * @return Lista filtrada de userIds cuyos roles permiten el tipo de notificación
     */
    fun filterByRole(
        userIds: List<UUID>,
        notificationType: NotificationType,
        roleResolver: (UUID) -> UserRole
    ): List<UUID> =
        userIds.filter { userId ->
            isApplicableForRole(notificationType, roleResolver(userId))
        }
}
