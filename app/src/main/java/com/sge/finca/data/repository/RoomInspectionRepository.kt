package com.sge.finca.data.repository

import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.RoutePointEntity
import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RoutePoint
import com.sge.finca.domain.model.RouteStatus
import com.sge.finca.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomInspectionRepository @Inject constructor(
    private val dao: InspectionDao,
) : InspectionRepository {

    override fun getRoutes(): Flow<List<InspectionRoute>> =
        dao.observeRoutes().map { entities -> entities.map { it.toDomain() } }

    override fun getRoutePoints(routeId: String): Flow<List<RoutePoint>> =
        dao.observePoints(routeId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createRoute(route: InspectionRoute): String {
        dao.insertRoute(InspectionRouteEntity.fromDomain(route))
        return route.id
    }

    override suspend fun addPoint(point: RoutePoint) {
        dao.insertPoint(RoutePointEntity.fromDomain(point))
    }

    override suspend fun finishRoute(routeId: String, totalDistance: Double) {
        dao.finishRoute(
            routeId = routeId,
            status = RouteStatus.COMPLETED.name,
            endTime = System.currentTimeMillis(),
            distance = totalDistance,
            pointCount = dao.countPoints(routeId),
        )
    }
}
