package com.sge.finca.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.FarmContactEntity
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.ReminderEntity
import com.sge.finca.data.local.entity.RoutePointEntity
import com.sge.finca.data.local.entity.TransactionEntity
import com.sge.finca.domain.model.TransactionType
import java.util.Locale

/*
 * Conversión entidad de Room <-> documento de Firestore.
 *
 * Reglas:
 * - syncStatus NO se sube: es un dato local del celular.
 * - farmId no va dentro del documento: ya está en la ruta farms/{farmId}/...
 * - El id del documento es el mismo UUID que la fila de Room (se genera en el celular).
 * - Lo que llega de Firestore se guarda como SYNCED.
 * - fromFirestore devuelve null si al documento le falta un campo obligatorio, para no
 *   romper la app con un documento corrupto o de una versión vieja.
 */

// ---------- Transacciones ----------

fun TransactionEntity.toFirestore(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "amountCents" to amountCents,
    "categoryId" to categoryId,
    "categoryName" to categoryName,
    "dateEpochDay" to dateEpochDay,
    "note" to note,
    "paymentMethod" to paymentMethod,
    "remotePhotoUrl" to remotePhotoUrl,
    "createdBy" to createdBy,
    "createdByName" to createdByName,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toTransactionEntity(farmId: String): TransactionEntity? {
    return TransactionEntity(
        id = id,
        farmId = farmId,
        type = getEnum<TransactionType>("type") ?: return null,
        amountCents = getLong("amountCents") ?: return null,
        categoryId = getString("categoryId") ?: return null,
        categoryName = getString("categoryName") ?: "",
        dateEpochDay = getLong("dateEpochDay") ?: return null,
        note = getString("note"),
        paymentMethod = getString("paymentMethod"),
        // La foto local solo existe en el celular que la tomó.
        photoUri = null,
        remotePhotoUrl = getString("remotePhotoUrl"),
        createdBy = getString("createdBy") ?: return null,
        createdByName = getString("createdByName"),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        syncStatus = SyncStatus.SYNCED,
    )
}

// ---------- Categorías ----------

fun CategoryEntity.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "isDefault" to isDefault,
)

fun DocumentSnapshot.toCategoryEntity(farmId: String): CategoryEntity? {
    val name = getString("name") ?: return null
    return CategoryEntity(
        id = id,
        farmId = farmId,
        name = name,
        normalizedName = name.trim().lowercase(Locale.ROOT),
        type = getEnum<TransactionType>("type") ?: return null,
        isDefault = getBoolean("isDefault") ?: false,
        syncStatus = SyncStatus.SYNCED,
    )
}

// ---------- Contactos ----------

fun FarmContactEntity.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name,
    "phone" to phone,
    "email" to email,
    "notes" to notes,
    "createdBy" to createdBy,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toFarmContactEntity(farmId: String): FarmContactEntity? {
    return FarmContactEntity(
        id = id,
        farmId = farmId,
        name = getString("name") ?: return null,
        phone = getString("phone"),
        email = getString("email"),
        notes = getString("notes"),
        createdBy = getString("createdBy") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        syncStatus = SyncStatus.SYNCED,
    )
}

// ---------- Recordatorios ----------

fun ReminderEntity.toFirestore(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "dueAt" to dueAt,
    "isCompleted" to isCompleted,
    "createdBy" to createdBy,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toReminderEntity(farmId: String): ReminderEntity? {
    return ReminderEntity(
        id = id,
        farmId = farmId,
        title = getString("title") ?: return null,
        description = getString("description"),
        dueAt = getLong("dueAt") ?: return null,
        isCompleted = getBoolean("isCompleted") ?: false,
        createdBy = getString("createdBy") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        syncStatus = SyncStatus.SYNCED,
    )
}

// ---------- Animales ----------

fun AnimalEntity.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name,
    "notes" to notes,
    "remotePhotoUrl" to remotePhotoUrl,
    "latitude" to latitude,
    "longitude" to longitude,
    "locationAccuracy" to locationAccuracy?.toDouble(),
    "variant" to variant,
    "createdBy" to ownerUid,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toAnimalEntity(farmId: String): AnimalEntity? {
    return AnimalEntity(
        id = id,
        name = getString("name") ?: return null,
        notes = getString("notes") ?: "",
        photoUri = null,
        latitude = getDouble("latitude") ?: 0.0,
        longitude = getDouble("longitude") ?: 0.0,
        locationAccuracy = getDouble("locationAccuracy")?.toFloat(),
        createdAt = getLong("createdAt") ?: 0L,
        variant = getLong("variant")?.toInt() ?: 0,
        ownerUid = getString("createdBy"),
        farmId = farmId,
        remotePhotoUrl = getString("remotePhotoUrl"),
        syncStatus = SyncStatus.SYNCED,
        // Documentos anteriores a #62 no traen updatedAt.
        updatedAt = getLong("updatedAt") ?: getLong("createdAt") ?: 0L,
    )
}

// ---------- Inspecciones ----------

fun InspectionRouteEntity.toFirestore(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "startTime" to startTime,
    "endTime" to endTime,
    "status" to status,
    "totalDistanceMeters" to totalDistanceMeters,
    "pointCount" to pointCount,
)

fun DocumentSnapshot.toInspectionRouteEntity(farmId: String): InspectionRouteEntity? {
    return InspectionRouteEntity(
        id = id,
        farmId = farmId,
        userId = getString("userId") ?: return null,
        startTime = getLong("startTime") ?: 0L,
        endTime = getLong("endTime"),
        status = getString("status") ?: return null,
        totalDistanceMeters = getDouble("totalDistanceMeters") ?: 0.0,
        pointCount = getLong("pointCount")?.toInt() ?: 0,
        syncStatus = SyncStatus.SYNCED,
    )
}

fun RoutePointEntity.toFirestore(): Map<String, Any?> = mapOf(
    "order" to order,
    "latitude" to latitude,
    "longitude" to longitude,
    "accuracy" to accuracy.toDouble(),
    "timestamp" to timestamp,
)

fun DocumentSnapshot.toRoutePointEntity(routeId: String): RoutePointEntity? {
    return RoutePointEntity(
        id = id,
        routeId = routeId,
        order = getLong("order")?.toInt() ?: return null,
        latitude = getDouble("latitude") ?: return null,
        longitude = getDouble("longitude") ?: return null,
        accuracy = getDouble("accuracy")?.toFloat() ?: 0f,
        timestamp = getLong("timestamp") ?: 0L,
        syncStatus = SyncStatus.SYNCED,
    )
}

// ---------- Utilidades ----------

/** Lee un enum guardado por nombre. Devuelve null si falta o si el valor no existe. */
private inline fun <reified T : Enum<T>> DocumentSnapshot.getEnum(field: String): T? {
    val raw = getString(field) ?: return null
    return enumValues<T>().firstOrNull { it.name == raw }
}
