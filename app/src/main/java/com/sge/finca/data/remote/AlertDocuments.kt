package com.sge.finca.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.sge.finca.domain.model.AlertStatus
import com.sge.finca.domain.model.AnimalAlert
import com.sge.finca.domain.model.Coordinate

/*
 * Documento farms/{farmId}/alerts/{id} (#73), escrito por sge-notifier:
 *   animalId, animalName  vaca con collar que salió de los potreros
 *   lat, lng              dónde estaba al detectarse
 *   status                "OPEN" | "RESOLVED"
 *   createdAt, resolvedAt Long (ms)
 *   forwardedAt, forwardedBy  los escribe el propietario al reenviar
 */

/** null si el documento no existe o le faltan datos. */
fun DocumentSnapshot.toAnimalAlert(farmId: String): AnimalAlert? {
    if (!exists()) return null
    return AnimalAlert(
        id = id,
        farmId = farmId,
        animalId = getString("animalId") ?: return null,
        animalName = getString("animalName")?.takeIf { it.isNotBlank() } ?: "Una vaca",
        position = Coordinate(getDouble("lat") ?: return null, getDouble("lng") ?: return null),
        status = if (getString("status") == "RESOLVED") AlertStatus.RESOLVED else AlertStatus.OPEN,
        createdAt = getLong("createdAt") ?: 0L,
        forwardedAt = getLong("forwardedAt"),
        resolvedAt = getLong("resolvedAt"),
    )
}
