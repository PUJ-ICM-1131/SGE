package com.sge.finca.domain.model

enum class UserRole { OWNER, WORKER }

/** Finca: raíz de todos los datos compartidos. Vive en Firestore en farms/{id}. */
data class Farm(
    val id: String,
    val name: String,
    val ownerUid: String,
    val inviteCode: String? = null,
    val createdAt: Long,
)

/** Perfil del usuario en users/{uid}. [farmId] es la finca a la que pertenece (null si aún no tiene). */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val farmId: String? = null,
    val role: UserRole? = null,
    val createdAt: Long,
)
