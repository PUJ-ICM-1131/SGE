package com.sge.finca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.RoutePointEntity

@Database(
    entities = [
        InspectionRouteEntity::class,
        RoutePointEntity::class,
        AnimalEntity::class,
        CategoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun animalDao(): AnimalDao
    abstract fun categoryDao(): CategoryDao
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
                    "PRIMARY KEY(`id`))"
        )
    }
}

/** v2 -> v3: apariencia (variante) de cada vaca. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `animals` ADD COLUMN `variant` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/** v3 -> v4: vincula los animales nuevos con su usuario. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `animals` ADD COLUMN `ownerUid` TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_animals_ownerUid` " +
                    "ON `animals` (`ownerUid`)"
        )
    }
}

/** v4 -> v5: agrega las categorías económicas sin borrar datos anteriores. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` TEXT NOT NULL, " +
                    "`farmId` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`normalizedName` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL, " +
                    "`syncStatus` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_categories_farmId_type_normalizedName` " +
                    "ON `categories` (`farmId`, `type`, `normalizedName`)"
        )
    }
}