package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.domain.model.RoutePoint

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class RoutePointEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val order: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = 0L,
) {
    fun toDomain() = RoutePoint(
        id = id,
        routeId = routeId,
        order = order,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = timestamp,
    )

    companion object {
        fun fromDomain(point: RoutePoint) = RoutePointEntity(
            id = point.id,
            routeId = point.routeId,
            order = point.order,
            latitude = point.latitude,
            longitude = point.longitude,
            accuracy = point.accuracy,
            timestamp = point.timestamp,
        )
    }
}
