package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.remote.collar.CollarApi
import com.sge.finca.data.remote.collar.CollarDto
import com.sge.finca.data.remote.collar.CreateCollarRequest
import com.sge.finca.data.remote.collar.LatLngDto
import com.sge.finca.data.remote.collar.UpdateCollarRequest
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.repository.CollarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Sincroniza los animales de la cuenta con los collares del proveedor (CRUD por REST).
 * En el proveedor, la "finca" del collar es el uid del dueño, porque los animales son por usuario.
 */
@Singleton
class RemoteCollarRepository @Inject constructor(
    private val collarApi: CollarApi,
    private val auth: FirebaseAuth,
    @ApplicationScope private val scope: CoroutineScope,
) : CollarRepository {

    override suspend fun ensureCollars(animals: List<Animal>) {
        val ownerId = auth.currentUser?.uid ?: return
        if (animals.isEmpty()) return

        val existing: Map<String, CollarDto> = animals
            .map { it.id }
            .chunked(CollarApi.MAX_ANIMALS_PER_REQUEST)
            .flatMap { ids -> collarApi.listCollars(animalIds = ids.joinToString(",")).items }
            .filter { it.animalId != null }
            .associateBy { it.animalId!! }

        for (animal in animals) {
            val collar = existing[animal.id]
            when {
                collar == null -> create(ownerId, animal)
                collar.needsUpdate(animal) -> collarApi.updateCollar(
                    collar.id,
                    UpdateCollarRequest(animalName = animal.name, anchor = animal.anchor()),
                )
            }
        }
    }

    override fun releaseCollar(animalId: String) {
        scope.launch {
            try {
                collarApi.listCollars(animalIds = animalId).items
                    .forEach { collarApi.deleteCollar(it.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "No se pudo liberar el collar del animal $animalId", e)
            } catch (e: HttpException) {
                Log.w(TAG, "El proveedor rechazó liberar el collar del animal $animalId", e)
            }
        }
    }

    private suspend fun create(ownerId: String, animal: Animal) {
        try {
            collarApi.createCollar(
                CreateCollarRequest(
                    serial = serialFor(animal.id),
                    farmId = ownerId,
                    anchor = animal.anchor(),
                    animalId = animal.id,
                    animalName = animal.name,
                ),
            )
        } catch (e: HttpException) {
            // 409: otro dispositivo de la misma cuenta lo creó al mismo tiempo. No es un error.
            if (e.code() != HTTP_CONFLICT) throw e
        }
    }

    private fun Animal.anchor() = LatLngDto(lat = latitude, lng = longitude)

    /** Cambió el nombre o el animal se movió de potrero (más de ~1 m). */
    private fun CollarDto.needsUpdate(animal: Animal): Boolean =
        animalName != animal.name ||
            abs(anchor.lat - animal.latitude) > ANCHOR_TOLERANCE_DEGREES ||
            abs(anchor.lng - animal.longitude) > ANCHOR_TOLERANCE_DEGREES

    companion object {
        private const val TAG = "CollarTracking"
        private const val HTTP_CONFLICT = 409
        private const val ANCHOR_TOLERANCE_DEGREES = 0.00001 // ≈ 1 m

        /** Serial estable derivado del id del animal: "SGE-" + id sin guiones (máx. 40 caracteres). */
        fun serialFor(animalId: String): String =
            "SGE-" + animalId.replace(Regex("[^A-Za-z0-9]"), "").take(36).uppercase()
    }
}
