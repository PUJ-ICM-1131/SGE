package com.sge.finca.domain.model

/**
 * Acciones de la app que dependen del rol (RF-08, RF-09).
 *
 * Es la única tabla de permisos de la app: las pantallas la usan para mostrar u ocultar
 * opciones, y los repositorios para rechazar escrituras. Las Security Rules de Firestore
 * (firebase/firestore.rules) repiten estas reglas como última línea de defensa.
 */
enum class Permission(val ownerOnly: Boolean) {
    // --- Propietario y trabajador ---
    CREATE_TRANSACTION(ownerOnly = false),
    VIEW_TRANSACTIONS(ownerOnly = false),
    VIEW_REPORTS(ownerOnly = false),
    REGISTER_ANIMAL(ownerOnly = false),
    START_INSPECTION(ownerOnly = false),
    MANAGE_CONTACTS(ownerOnly = false),
    MANAGE_REMINDERS(ownerOnly = false),

    // --- Solo propietario ---
    EDIT_TRANSACTION(ownerOnly = true),
    DELETE_TRANSACTION(ownerOnly = true),
    MANAGE_CATEGORIES(ownerOnly = true),
    MANAGE_WORKERS(ownerOnly = true),
    VIEW_INVITE_CODE(ownerOnly = true),
    CONFIGURE_FARM(ownerOnly = true),
}

/**
 * ¿Este rol puede hacer [permission]? Un rol null (sin sesión, sin finca o miembro inactivo)
 * no puede hacer nada.
 */
fun FarmRole?.can(permission: Permission): Boolean = when (this) {
    FarmRole.OWNER -> true
    FarmRole.WORKER -> !permission.ownerOnly
    null -> false
}
