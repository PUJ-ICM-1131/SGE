package com.sge.finca.data.repository

import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.repository.AnimalRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAnimalRepository @Inject constructor(
    private val dao: AnimalDao,
    private val auth: FirebaseAuth,
) : AnimalRepository {

    override fun getAnimals(): Flow<List<Animal>> =
        callbackFlow<String?> {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else dao.observeAnimals(uid).map { entities -> entities.map { it.toDomain() } }
        }

    override suspend fun saveAnimal(animal: Animal) {
        dao.insertAnimal(AnimalEntity.fromDomain(animal, requireUserUid()))
    }

    override suspend fun getAnimal(id: String): Animal? =
        auth.currentUser?.uid?.let { dao.getById(id, it)?.toDomain() }

    override suspend fun updateAnimal(animal: Animal) {
        val uid = requireUserUid()
        val changed = dao.updateAnimal(
            id = animal.id, ownerUid = uid, name = animal.name, notes = animal.notes,
            photoUri = animal.photoUri, latitude = animal.latitude, longitude = animal.longitude,
            locationAccuracy = animal.locationAccuracy, createdAt = animal.createdAt,
            variant = animal.variant,
        )
        require(changed == 1) { "El animal no pertenece a la cuenta activa." }
    }

    override suspend fun deleteAnimal(id: String) {
        dao.deleteById(id, requireUserUid())
    }

    private fun requireUserUid(): String =
        checkNotNull(auth.currentUser?.uid) { "Inicia sesión para gestionar animales." }
}
