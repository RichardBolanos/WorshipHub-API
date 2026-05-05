package com.worshiphub.application.notification

import com.worshiphub.domain.collaboration.push.UserRole
import com.worshiphub.domain.organization.canManageChurch
import com.worshiphub.domain.organization.repository.TeamRepository
import com.worshiphub.domain.organization.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.*

/**
 * Servicio auxiliar que resuelve el rol efectivo de un usuario para el sistema
 * de notificaciones push, aplicando la jerarquía Admin > Líder_Equipo > Miembro.
 *
 * Utiliza el rol global del usuario en la iglesia ([com.worshiphub.domain.organization.UserRole])
 * y su posición como líder de equipo ([com.worshiphub.domain.organization.Team.leaderId])
 * para determinar el [UserRole] de mayor jerarquía.
 *
 * _Requisitos: 30.5_
 */
@Service
class UserRoleResolver(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository
) {

    /**
     * Resuelve el rol efectivo (mayor jerarquía) de un usuario.
     *
     * - Si el usuario tiene rol `CHURCH_ADMIN` o `SUPER_ADMIN` → [UserRole.ADMIN]
     * - Si el usuario es líder de algún equipo o tiene rol `WORSHIP_LEADER` → [UserRole.TEAM_LEADER]
     * - En caso contrario → [UserRole.MEMBER]
     *
     * @param userId ID del usuario a resolver
     * @return El [UserRole] de mayor jerarquía del usuario
     */
    fun resolveEffectiveRole(userId: UUID): UserRole {
        val user = userRepository.findById(userId)
            ?: return UserRole.MEMBER

        // Si el usuario es admin de la iglesia → ADMIN
        if (user.role.canManageChurch()) {
            return UserRole.ADMIN
        }

        // Si el usuario es líder de algún equipo → TEAM_LEADER
        if (user.role == com.worshiphub.domain.organization.UserRole.WORSHIP_LEADER) {
            return UserRole.TEAM_LEADER
        }

        val teamsAsLeader = teamRepository.findByLeaderId(userId)
        if (teamsAsLeader.isNotEmpty()) {
            return UserRole.TEAM_LEADER
        }

        return UserRole.MEMBER
    }
}
