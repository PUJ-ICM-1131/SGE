package com.sge.finca.data.repository

import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.model.AnimalLocation
import com.sge.finca.domain.repository.AnimalLocationSource
import com.sge.finca.domain.repository.AnimalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * SOLO PARA PRUEBAS: simula la API de rastreo. Cada animal camina al azar cerca del sitio
 * donde se registró y a veces se detiene, para ver el marcador deslizarse y "correr".
 * Para volver a animales quietos, cambiar el @Binds en AnimalModule a StationaryAnimalLocationSource.
 */
@Singleton
class SimulatedAnimalLocationSource @Inject constructor(
    private val animalRepository: AnimalRepository,
) : AnimalLocationSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLocations(): Flow<Map<String, AnimalLocation>> =
        animalRepository.getAnimals().flatMapLatest { animals -> simulate(animals) }

    private data class Walker(val animal: Animal, var lat: Double, var lng: Double, var heading: Double)

    private fun simulate(animals: List<Animal>): Flow<Map<String, AnimalLocation>> = flow {
        val walkers = animals.map { Walker(it, it.latitude, it.longitude, Random.nextDouble(0.0, 2 * PI)) }
        while (true) {
            delay(TICK_MILLIS)
            val result = HashMap<String, AnimalLocation>()
            for (w in walkers) {
                val moving = Random.nextFloat() > PAUSE_CHANCE
                var speed = 0f
                if (moving) {
                    w.heading += Random.nextDouble(-0.6, 0.6)
                    // Si se aleja demasiado del punto de registro, vuelve hacia él
                    val dNorth = (w.animal.latitude - w.lat) * METERS_PER_DEGREE
                    val dEast = (w.animal.longitude - w.lng) * METERS_PER_DEGREE * cos(Math.toRadians(w.lat))
                    if (sqrt(dNorth * dNorth + dEast * dEast) > MAX_RADIUS_METERS) {
                        w.heading = atan2(dEast, dNorth)
                    }
                    // La mayoría del tiempo camina; a veces sale corriendo
                    val step = if (Random.nextFloat() < RUN_CHANCE) RUN_STEP_METERS else WALK_STEP_METERS
                    w.lat += step * cos(w.heading) / METERS_PER_DEGREE
                    w.lng += step * sin(w.heading) / (METERS_PER_DEGREE * cos(Math.toRadians(w.lat)))
                    speed = (step / (TICK_MILLIS / 1000.0)).toFloat()
                }
                result[w.animal.id] = AnimalLocation(
                    animalId = w.animal.id,
                    latitude = w.lat,
                    longitude = w.lng,
                    speedMetersPerSecond = speed,
                    timestamp = System.currentTimeMillis(),
                )
            }
            emit(result)
        }
    }

    private companion object {
        const val TICK_MILLIS = 2000L
        const val WALK_STEP_METERS = 2.0   // 1.5 m/s
        const val RUN_STEP_METERS = 5.0    // 3.5 m/s
        const val RUN_CHANCE = 0.1f
        const val MAX_RADIUS_METERS = 40.0
        const val PAUSE_CHANCE = 0.50f
        const val METERS_PER_DEGREE = 111_320.0
    }
}
