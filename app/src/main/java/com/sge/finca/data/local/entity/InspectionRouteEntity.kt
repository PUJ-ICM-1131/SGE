package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RouteStatus

@Entity(tableName = "inspection_routes")
data class InspectionRouteEntity(
    @PrimaryKey val id: String,
    val farmId: String = "",
    val userId: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val status: String = RouteStatus.ACTIVE.name,
    val totalDistanceMeters: Double = 0.0,
    val pointCount: Int = 0,
) {
    fun toDomain() = InspectionRoute(
        id = id,
        farmId = farmId,
        userId = userId,
        startTime = startTime,
        endTime = endTime,
        status = RouteStatus.valueOf(status),
        totalDistanceMeters = totalDistanceMeters,
        pointCount = pointCount,
    )

    companion object {
        fun fromDomain(route: InspectionRoute) = InspectionRouteEntity(
            id = route.id,
            farmId = route.farmId,
            userId = route.userId,
            startTime = route.startTime,
            endTime = route.endTime,
            status = route.status.name,
            totalDistanceMeters = route.totalDistanceMeters,
            pointCount = route.pointCount,
        )
    }
}
