package com.sge.finca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.FarmContactDao
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.dao.ReminderDao
import com.sge.finca.data.local.dao.TransactionDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.FarmContactEntity
import com.sge.finca.data.local.entity.InspectionRouteEntity
import com.sge.finca.data.local.entity.ReminderEntity
import com.sge.finca.data.local.entity.RoutePointEntity
import com.sge.finca.data.local.entity.TransactionEntity

@Database(
    entities = [
        InspectionRouteEntity::class,
        RoutePointEntity::class,
        AnimalEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        FarmContactEntity::class,
        ReminderEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun animalDao(): AnimalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun farmContactDao(): FarmContactDao
    abstract fun reminderDao(): ReminderDao
}

/** Todas las migraciones, en orden. Usar esta lista al construir la base de datos. */
val ALL_MIGRATIONS: Array<Migration>
    get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

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

/**
 * v5 -> v6 (issue #23): modelo de datos completo.
 * - Tablas nuevas: transactions, farm_contacts, reminders.
 * - syncStatus en las tablas existentes (las filas viejas quedan PENDING para que el
 *   issue #32 las suba a Firestore).
 * - animals: farmId y remotePhotoUrl (null en los registros anteriores).
 * - inspection_routes: índice por farmId.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- Tablas existentes ---
        db.execSQL("ALTER TABLE `animals` ADD COLUMN `farmId` TEXT")
        db.execSQL("ALTER TABLE `animals` ADD COLUMN `remotePhotoUrl` TEXT")
        db.execSQL(
            "ALTER TABLE `animals` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_animals_farmId` ON `animals` (`farmId`)"
        )

        db.execSQL(
            "ALTER TABLE `inspection_routes` " +
                    "ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inspection_routes_farmId` " +
                    "ON `inspection_routes` (`farmId`)"
        )

        db.execSQL(
            "ALTER TABLE `route_points` " +
                    "ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'"
        )

        // --- Tablas nuevas ---
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions` (" +
                    "`id` TEXT NOT NULL, " +
                    "`farmId` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`amountCents` INTEGER NOT NULL, " +
                    "`categoryId` TEXT NOT NULL, " +
                    "`categoryName` TEXT NOT NULL, " +
                    "`dateEpochDay` INTEGER NOT NULL, " +
                    "`note` TEXT, " +
                    "`paymentMethod` TEXT, " +
                    "`photoUri` TEXT, " +
                    "`remotePhotoUrl` TEXT, " +
                    "`createdBy` TEXT NOT NULL, " +
                    "`createdByName` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`syncStatus` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_farmId_dateEpochDay` " +
                    "ON `transactions` (`farmId`, `dateEpochDay`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_farmId_categoryId` " +
                    "ON `transactions` (`farmId`, `categoryId`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `farm_contacts` (" +
                    "`id` TEXT NOT NULL, " +
                    "`farmId` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`phone` TEXT, " +
                    "`email` TEXT, " +
                    "`notes` TEXT, " +
                    "`createdBy` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`syncStatus` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_farm_contacts_farmId` " +
                    "ON `farm_contacts` (`farmId`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (" +
                    "`id` TEXT NOT NULL, " +
                    "`farmId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`description` TEXT, " +
                    "`dueAt` INTEGER NOT NULL, " +
                    "`isCompleted` INTEGER NOT NULL, " +
                    "`createdBy` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`syncStatus` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminders_farmId_dueAt` " +
                    "ON `reminders` (`farmId`, `dueAt`)"
        )
    }
}
