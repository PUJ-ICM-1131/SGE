package com.sge.finca.data.repository

import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RoutePoint
import com.sge.finca.domain.model.RouteStatus
import com.sge.finca.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryInspectionRepository @Inject constructor() : InspectionRepository {

    private val routes = MutableStateFlow<List<InspectionRoute>>(emptyList())
    private val points = MutableStateFlow<List<RoutePoint>>(emptyList())

    override fun getRoutes(): Flow<List<InspectionRoute>> = routes

    override fun getRoutePoints(routeId: String): Flow<List<RoutePoint>> =
        points.map { list -> list.filter { it.routeId == routeId } }

    override suspend fun createRoute(route: InspectionRoute): String {
        val id = route.id.ifEmpty { System.currentTimeMillis().toString() }
        val newRoute = route.copy(id = id)
        routes.update { it + newRoute }
        return id
    }

    override suspend fun addPoint(point: RoutePoint) {
        points.update { it + point }
    }

    override suspend fun finishRoute(routeId: String, totalDistance: Double) {
        routes.update { list ->
            list.map { route ->
                if (route.id == routeId) {
                    route.copy(
                        status = RouteStatus.COMPLETED,
                        endTime = System.currentTimeMillis(),
                        totalDistanceMeters = totalDistance,
                        pointCount = points.value.count { it.routeId == routeId },
                    )
                } else route
            }
        }
    }
}
