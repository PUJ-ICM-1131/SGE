package com.sge.finca.domain.usecase

import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.domain.repository.AnimalLocationSource
import com.sge.finca.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Une los animales registrados con su posición en vivo (si existe).
 *  - Sin posición en vivo: se usa la del registro y el animal está quieto.
 *  - Con posición en vivo: se usa esa y se marca "en movimiento" si supera el umbral de velocidad.
 */
class ObserveTrackedAnimalsUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val locationSource: AnimalLocationSource,
) {

    operator fun invoke(): Flow<List<TrackedAnimal>> =
        combine(
            animalRepository.getAnimals(),
            locationSource.observeLocations(),
        ) { animals, live ->
            animals.map { animal ->
                val location = live[animal.id]
                val speed = location?.speedMetersPerSecond ?: 0f
                TrackedAnimal(
                    animal = animal,
                    latitude = location?.latitude ?: animal.latitude,
                    longitude = location?.longitude ?: animal.longitude,
                    isMoving = speed > MOVING_SPEED_THRESHOLD,
                    speedMetersPerSecond = speed,
                )
            }
        }

    private companion object {
        /** Por debajo de esta velocidad (m/s) se considera que el animal está quieto. */
        const val MOVING_SPEED_THRESHOLD = 0.5f
    }
}
