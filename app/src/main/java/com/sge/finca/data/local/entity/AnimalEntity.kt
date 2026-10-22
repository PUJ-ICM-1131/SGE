package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sge.finca.domain.model.Animal

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val photoUri: String?,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float?,
    val createdAt: Long,
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
    )

    companion object {
        fun fromDomain(animal: Animal) = AnimalEntity(
            id = animal.id,
            name = animal.name,
            notes = animal.notes,
            photoUri = animal.photoUri,
            latitude = animal.latitude,
            longitude = animal.longitude,
            locationAccuracy = animal.locationAccuracy,
            createdAt = animal.createdAt,
        )
    }
}
