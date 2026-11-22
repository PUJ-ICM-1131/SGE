package com.sge.finca.domain.model

/** Rol de un usuario dentro de una finca (RF-08, RF-09). Ver [Permission]. */
enum class FarmRole { OWNER, WORKER }

/** Finca: raíz de todos los datos compartidos. Vive en Firestore en farms/{id}. */
data class Farm(
    val id: String,
    val name: String,
    val ownerUid: String,
    val inviteCode: String? = null,
    val createdAt: Long,
)

/**
 * Membresía de un usuario en una finca: farms/{farmId}/members/{uid}.
 * Es la fuente de verdad del rol; las Security Rules de Firestore leen este documento.
 * Un miembro con [isActive] = false no tiene ningún permiso (trabajador dado de baja).
 */
data class FarmMember(
    val uid: String,
    val farmId: String,
    val role: FarmRole,
    val isActive: Boolean = true,
    val joinedAt: Long,
    val displayName: String = "",
    val email: String = "",
    /** Permisos que el propietario le otorgó a este miembro (#67); solo cuentan los [Permission.grantable]. */
    val grants: Set<Permission> = emptySet(),
)

/**
 * Perfil del usuario en users/{uid}. [farmId] es la finca a la que pertenece (null si aún no tiene).
 * [role] es solo una copia informativa: para permisos se usa siempre [FarmMember.role].
 */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val farmId: String? = null,
    val role: FarmRole? = null,
    val createdAt: Long,
)
