package com.sge.finca.domain.repository

import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RoutePoint
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun getRoutes(): Flow<List<InspectionRoute>>
    fun getRoutePoints(routeId: String): Flow<List<RoutePoint>>
    suspend fun createRoute(route: InspectionRoute): String
    suspend fun addPoint(point: RoutePoint)
    suspend fun finishRoute(routeId: String, totalDistance: Double)
}
