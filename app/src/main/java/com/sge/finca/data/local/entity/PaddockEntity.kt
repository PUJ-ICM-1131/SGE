package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.Coordinate
import com.sge.finca.domain.model.Paddock

/** Potrero (#71). [points] va como texto "lat,lng;lat,lng;..." para no necesitar un TypeConverter. */
@Entity(tableName = "paddocks", indices = [Index("farmId")])
data class PaddockEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val points: String,
    val areaM2: Double,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) {
    fun toDomain() = Paddock(
        id = id,
        farmId = farmId,
        name = name,
        points = decodePoints(points),
        areaM2 = areaM2,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun encodePoints(points: List<Coordinate>): String =
            points.joinToString(";") { "${it.latitude},${it.longitude}" }

        fun decodePoints(encoded: String): List<Coordinate> =
            encoded.split(';').mapNotNull { pair ->
                val parts = pair.split(',')
                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                val lng = parts.getOrNull(1)?.toDoubleOrNull()
                if (parts.size == 2 && lat != null && lng != null) Coordinate(lat, lng) else null
            }
    }
}
