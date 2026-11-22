package com.sge.finca.data.remote.collar

import com.sge.finca.domain.model.AnimalLocation

fun CollarPositionDto.toDomain(animalId: String): AnimalLocation =
    AnimalLocation(
        animalId = animalId,
        latitude = lat,
        longitude = lng,
        speedMetersPerSecond = speedMps.coerceAtLeast(0f),
        timestamp = timestamp,
    )

/**
 * Indexa las lecturas por id de animal, que es lo que espera
 * [com.sge.finca.domain.repository.AnimalLocationSource]. Se ignoran los collares sin animal.
 */
fun CollarPositionsResponse.toLocationsByAnimalId(): Map<String, AnimalLocation> =
    positions
        .mapNotNull { position -> position.animalId?.let { id -> id to position.toDomain(id) } }
        .toMap()
