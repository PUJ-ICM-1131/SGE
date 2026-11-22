package com.sge.finca.data.repository

import android.util.Log
import com.sge.finca.data.remote.collar.CollarApi
import com.sge.finca.data.remote.collar.toLocationsByAnimalId
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.model.AnimalLocation
import com.sge.finca.domain.repository.AnimalLocationSource
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.domain.repository.CollarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posiciones en vivo desde el proveedor externo de collares GPS (REST, carpeta collar-api/).
 *
 * - Antes de consultar, se asegura de que cada animal tenga su collar (lo crea si falta y
 *   actualiza el anclaje si el animal se editó). Así funciona también con animales que ya
 *   estaban registrados antes de existir el proveedor.
 * - Consulta cada [POLL_INTERVAL_MS] mientras alguien observe el Flow (mapa o lista abiertos).
 * - Si la lista de animales cambia, reinicia con los datos nuevos.
 * - Si falla la red o el servidor, conserva la última posición conocida y reintenta con
 *   espera exponencial (hasta [MAX_BACKOFF_MS]); la pantalla nunca se rompe por un error.
 *   Los animales sin posición en vivo se muestran donde se registraron.
 */
@Singleton
class RemoteAnimalLocationSource @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val collarRepository: CollarRepository,
    private val collarApi: CollarApi,
) : AnimalLocationSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLocations(): Flow<Map<String, AnimalLocation>> =
        animalRepository.getAnimals()
            .map { animals -> animals.sortedBy { it.id } }
            // Solo reiniciar si cambia algo que le importa al collar (no la foto, por ejemplo).
            .distinctUntilChanged { old, new -> old.map { it.collarKey() } == new.map { it.collarKey() } }
            .flatMapLatest { animals ->
                if (animals.isEmpty()) flowOf(emptyMap()) else poll(animals)
            }

    private fun poll(animals: List<Animal>): Flow<Map<String, AnimalLocation>> = flow {
        // Emitir de inmediato para que la lista y el mapa no esperen la primera respuesta.
        emit(emptyMap())
        val animalIds = animals.map { it.id }
        var collarsReady = false
        var consecutiveFailures = 0
        while (currentCoroutineContext().isActive) {
            // emit() va fuera de los try: un error del colector no debe confundirse con uno de red.
            if (!collarsReady) {
                collarsReady = callOrNull("preparar los collares") { collarRepository.ensureCollars(animals) } != null
            }
            val locations = if (collarsReady) callOrNull("consultar posiciones") { fetch(animalIds) } else null
            if (locations != null) {
                consecutiveFailures = 0
                emit(locations)
                delay(POLL_INTERVAL_MS)
            } else {
                consecutiveFailures++
                delay(backoffMillis(consecutiveFailures))
            }
        }
    }

    /** Se consulta en lotes de [CollarApi.MAX_ANIMALS_PER_REQUEST] ids para no generar URLs gigantes. */
    private suspend fun fetch(animalIds: List<String>): Map<String, AnimalLocation> =
        animalIds.chunked(CollarApi.MAX_ANIMALS_PER_REQUEST)
            .map { chunk -> collarApi.getPositions(chunk.joinToString(",")).toLocationsByAnimalId() }
            .fold(emptyMap()) { acc, part -> acc + part }

    /** Ejecuta una llamada al proveedor; devuelve null si falla la red, el servidor o el formato. */
    private suspend fun <T> callOrNull(action: String, block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Sin conexión con el proveedor de collares al $action", e)
            null
        } catch (e: HttpException) {
            Log.w(TAG, "El proveedor de collares respondió ${e.code()} al $action", e)
            null
        } catch (e: SerializationException) {
            Log.w(TAG, "Respuesta inesperada del proveedor de collares al $action", e)
            null
        }

    private fun Animal.collarKey() = listOf(id, name, latitude, longitude)

    companion object {
        private const val TAG = "CollarTracking"
        const val POLL_INTERVAL_MS = 3_000L
        const val MAX_BACKOFF_MS = 30_000L

        /** 3 s, 6 s, 12 s, 24 s, 30 s, 30 s… */
        fun backoffMillis(consecutiveFailures: Int): Long {
            val exponent = (consecutiveFailures - 1).coerceIn(0, 10)
            return (POLL_INTERVAL_MS shl exponent).coerceAtMost(MAX_BACKOFF_MS)
        }
    }
}
