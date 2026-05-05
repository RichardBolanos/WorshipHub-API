package com.worshiphub.domain.collaboration.push

/**
 * Roles de usuario en la iglesia que determinan los tipos de notificación aplicables.
 *
 * Los valores están ordenados de mayor a menor jerarquía para que
 * [resolveHighest] funcione correctamente con `minByOrNull { it.ordinal }`:
 * ADMIN (0) > TEAM_LEADER (1) > MEMBER (2).
 */
enum class UserRole {
    ADMIN,
    TEAM_LEADER,
    MEMBER;

    companion object {
        /**
         * Resuelve el rol de mayor jerarquía de una lista de roles.
         * Admin > Líder_Equipo > Miembro.
         *
         * @param roles Lista de roles del usuario
         * @return El rol con mayor jerarquía, o [MEMBER] si la lista está vacía
         */
        fun resolveHighest(roles: List<UserRole>): UserRole =
            roles.minByOrNull { it.ordinal } ?: MEMBER
    }
}
