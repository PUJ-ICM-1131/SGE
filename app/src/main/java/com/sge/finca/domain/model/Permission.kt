package com.sge.finca.domain.model

/**
 * Acciones de la app que dependen del rol (RF-08, RF-09).
 *
 * Es la única tabla de permisos de la app: las pantallas la usan para mostrar u ocultar
 * opciones, y los repositorios para rechazar escrituras. Las Security Rules de Firestore
 * (firebase/firestore.rules) repiten estas reglas como última línea de defensa.
 *
 * Un permiso [grantable] es solo del propietario por defecto, pero él puede otorgárselo a un
 * trabajador en particular (issue #67).
 */
enum class Permission(val ownerOnly: Boolean, val grantable: Boolean = false) {
    // --- Propietario y trabajador ---
    CREATE_TRANSACTION(ownerOnly = false),
    VIEW_TRANSACTIONS(ownerOnly = false),
    REGISTER_ANIMAL(ownerOnly = false),
    START_INSPECTION(ownerOnly = false),
    MANAGE_CONTACTS(ownerOnly = false),
    MANAGE_REMINDERS(ownerOnly = false),

    // --- Solo propietario, salvo que se lo otorgue a un trabajador ---
    /** Balance y totales de ingresos/egresos (inicio, historial) y la pantalla de Reportes. */
    VIEW_BALANCE(ownerOnly = true, grantable = true),

    // --- Solo propietario ---
    EDIT_TRANSACTION(ownerOnly = true),
    DELETE_TRANSACTION(ownerOnly = true),
    MANAGE_CATEGORIES(ownerOnly = true),
    MANAGE_WORKERS(ownerOnly = true),
    VIEW_INVITE_CODE(ownerOnly = true),
    CONFIGURE_FARM(ownerOnly = true),
}

/**
 * ¿Este rol puede hacer [permission]? [grants] son los permisos que el propietario le otorgó al
 * miembro; solo cuentan los [Permission.grantable]. Un rol null (sin sesión, sin finca o miembro
 * inactivo) no puede hacer nada.
 */
fun FarmRole?.can(permission: Permission, grants: Set<Permission> = emptySet()): Boolean =
    when (this) {
        FarmRole.OWNER -> true
        FarmRole.WORKER -> !permission.ownerOnly || (permission.grantable && permission in grants)
        null -> false
    }
