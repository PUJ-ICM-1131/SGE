package com.sge.finca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.RoutePointEntity

@Database(
    entities = [InspectionRouteEntity::class, RoutePointEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
}
