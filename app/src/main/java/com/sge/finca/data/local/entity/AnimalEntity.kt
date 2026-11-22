package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.Animal

@Entity(tableName = "animals", indices = [Index("ownerUid"), Index("farmId")])
data class AnimalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val photoUri: String?,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float?,
    val createdAt: Long,
    val variant: Int = 0,
    /** Usuario que registró el animal (creador). */
    val ownerUid: String? = null,
    /** Finca a la que pertenece. Null en los animales creados antes de la v6. */
    val farmId: String? = null,
    /** URL de la foto en Firebase Storage (cuando se suba). [photoUri] es la copia local. */
    val remotePhotoUrl: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    /** Última edición (epoch millis): resuelve conflictos entre celulares (#32, #62). */
    val updatedAt: Long = 0L,
) {
    fun toDomain() = Animal(
        id = id,
        name = name,
        notes = notes,
        photoUri = photoUri,
        latitude = latitude,
        longitude = longitude,
        locationAccuracy = locationAccuracy,
        createdAt = createdAt,
        variant = variant,
    )

    companion object {
        fun fromDomain(animal: Animal, ownerUid: String, farmId: String, updatedAt: Long) = AnimalEntity(
            id = animal.id,
            name = animal.name,
            notes = animal.notes,
            photoUri = animal.photoUri,
            latitude = animal.latitude,
            longitude = animal.longitude,
            locationAccuracy = animal.locationAccuracy,
            createdAt = animal.createdAt,
            variant = animal.variant,
            ownerUid = ownerUid,
            farmId = farmId,
            updatedAt = updatedAt,
        )
    }
}
