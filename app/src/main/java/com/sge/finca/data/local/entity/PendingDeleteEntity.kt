package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Borrado hecho en el celular que falta aplicar en Firestore (issue #32).
 *
 * La fila original ya se borró de Room, así que aquí queda la ruta del documento remoto.
 * SyncWorker la elimina en Firestore cuando hay red; mientras tanto, el listener ignora ese
 * documento para que no "reviva" en el celular.
 */
@Entity(tableName = "pending_deletes", indices = [Index("farmId")])
data class PendingDeleteEntity(
    /** Ruta completa en Firestore, p. ej. farms/{farmId}/transactions/{id}. */
    @PrimaryKey val path: String,
    val farmId: String,
    /** Subcolección: "transactions", "categories", "contacts" o "reminders". */
    val collection: String,
    val docId: String,
    val createdAt: Long,
    /** true si Firestore lo rechazó de forma definitiva: ya no se reintenta solo. */
    val failed: Boolean = false,
)
