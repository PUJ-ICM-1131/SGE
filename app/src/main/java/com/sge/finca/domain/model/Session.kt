package com.sge.finca.domain.model

/**
 * Estado de la sesión: quién está conectado, en qué finca y con qué rol.
 *
 * - [loading] = true mientras se lee el perfil y la membresía (al abrir la app o al iniciar
 *   sesión). Mientras tanto las pantallas no deben decidir nada sobre permisos.
 * - [role] es null si no hay sesión, si el usuario aún no tiene finca o si su membresía está
 *   inactiva.
 */
data class Session(
    val uid: String? = null,
    val farmId: String? = null,
    val member: FarmMember? = null,
    val loading: Boolean = true,
) {
    val role: FarmRole?
        get() = member?.takeIf { it.isActive }?.role

    val isOwner: Boolean get() = role == FarmRole.OWNER

    fun can(permission: Permission): Boolean = role.can(permission, member?.grants.orEmpty())

    companion object {
        val Loading = Session(loading = true)
        val SignedOut = Session(loading = false)
    }
}
