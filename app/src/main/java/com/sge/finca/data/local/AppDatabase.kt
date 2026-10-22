package com.sge.finca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.RoutePointEntity

@Database(
    entities = [InspectionRouteEntity::class, RoutePointEntity::class, AnimalEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun animalDao(): AnimalDao
}

/** v1 -> v2: agrega la tabla de animales sin borrar las inspecciones existentes. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `animals` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`notes` TEXT NOT NULL, " +
                    "`photoUri` TEXT, " +
                    "`latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, " +
                    "`locationAccuracy` REAL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
        )
    }
}
