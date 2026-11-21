package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Query("SELECT * FROM inspection_routes ORDER BY startTime DESC")
    fun observeRoutes(): Flow<List<InspectionRouteEntity>>

    @Query("SELECT * FROM inspection_routes WHERE farmId = :farmId ORDER BY startTime DESC")
    fun observeRoutesByFarm(farmId: String): Flow<List<InspectionRouteEntity>>

    /** Recorrido en curso del usuario (si hay uno sin terminar). */
    @Query(
        "SELECT * FROM inspection_routes WHERE userId = :userId AND status = 'ACTIVE' " +
            "ORDER BY startTime DESC LIMIT 1",
    )
    suspend fun getActiveRoute(userId: String): InspectionRouteEntity?

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY `order` ASC")
    fun observePoints(routeId: String): Flow<List<RoutePointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: InspectionRouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: RoutePointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Query("SELECT COUNT(*) FROM route_points WHERE routeId = :routeId")
    suspend fun countPoints(routeId: String): Int

    @Query(
        "UPDATE inspection_routes SET status = :status, endTime = :endTime, " +
            "totalDistanceMeters = :distance, pointCount = :pointCount, syncStatus = 'PENDING' " +
            "WHERE id = :routeId",
    )
    suspend fun finishRoute(
        routeId: String,
        status: String,
        endTime: Long,
        distance: Double,
        pointCount: Int,
    )
}
