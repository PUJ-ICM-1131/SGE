package com.sge.finca.data.remote.collar

import kotlinx.serialization.Serializable

/*
 * Contrato REST del proveedor de collares GPS (repo sge-collar-api, ver su README).
 * Los DTO solo viven en la capa de datos: el resto de la app usa los modelos de dominio.
 */

@Serializable
data class LatLngDto(
    val lat: Double,
    val lng: Double,
)

/** Collar tal como lo devuelve el proveedor. [status] es "ACTIVE" o "INACTIVE". */
@Serializable
data class CollarDto(
    val id: String,
    val serial: String,
    val farmId: String,
    val animalId: String? = null,
    val animalName: String? = null,
    val anchor: LatLngDto,
    val radiusMeters: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CollarListResponse(
    val items: List<CollarDto>,
    val total: Int,
)

/** Cuerpo de POST /v1/collars. Los campos en null toman el valor por defecto del proveedor. */
@Serializable
data class CreateCollarRequest(
    val serial: String,
    val farmId: String,
    val anchor: LatLngDto,
    val animalId: String? = null,
    val animalName: String? = null,
    val radiusMeters: Double? = null,
)

/** Cuerpo de PATCH /v1/collars/{id}: solo se envían los campos que no son null. */
@Serializable
data class UpdateCollarRequest(
    val animalId: String? = null,
    val animalName: String? = null,
    val anchor: LatLngDto? = null,
    val radiusMeters: Double? = null,
    val status: String? = null,
)

@Serializable
data class CollarPositionsResponse(
    val generatedAt: Long,
    val positions: List<CollarPositionDto>,
)

@Serializable
data class CollarPositionDto(
    val collarId: String,
    val serial: String,
    val animalId: String? = null,
    val lat: Double,
    val lng: Double,
    val speedMps: Float = 0f,
    val headingDeg: Float? = null,
    val batteryPercent: Int? = null,
    val timestamp: Long,
)

/** Cuerpo de error del proveedor: {"error": "...", "details": [...]}. */
@Serializable
data class CollarErrorDto(
    val error: String,
    val details: List<String> = emptyList(),
)
