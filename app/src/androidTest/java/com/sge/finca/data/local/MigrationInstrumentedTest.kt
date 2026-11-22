package com.sge.finca.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sge.finca.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica que las migraciones 5 -> 6 -> 7 -> 8 no borran datos y que Room acepta el esquema resultante.
 *
 * No usa MigrationTestHelper (necesita room-testing y exportSchema = true). En su lugar crea a
 * mano una base de datos con el esquema v5, tal como la dejan las migraciones 1..5, y la abre con
 * Room: si el esquema migrado no coincide con las entidades, Room lanza IllegalStateException
 * ("Migration didn't properly handle...") y el test falla.
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrateFrom5KeepsDataAndMatchesEntities() = runBlocking {
        createVersion5Database()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            // Los datos viejos siguen ahí, con valores por defecto en las columnas nuevas.
            // v8 (#62): el animal viejo no tiene finca hasta que su creador abre una; ahí pasa a
            // esa finca y queda pendiente de subir. Los de otro usuario no se tocan.
            assertNull(db.animalDao().getById("finca-1", "animal-1"))
            assertEquals(0, db.animalDao().claimUnassigned("finca-1", "otro-uid", 2000L))
            assertEquals(1, db.animalDao().claimUnassigned("finca-1", "uid-1", 2000L))
            val animal = db.animalDao().getById("finca-1", "animal-1")!!
            assertEquals("Lola", animal.name)
            assertEquals("finca-1", animal.farmId)
            assertEquals(2000L, animal.updatedAt)
            assertEquals(SyncStatus.PENDING, animal.syncStatus)

            val route = db.inspectionDao().observeRoutes().first().single()
            assertEquals(SyncStatus.PENDING, route.syncStatus)
            assertEquals(1, db.inspectionDao().countPoints("route-1"))

            val category = db.categoryDao().getById("finca-1", "cat-1")!!
            assertEquals(TransactionType.INCOME, category.type)
            assertEquals(SyncStatus.SYNCED, category.syncStatus)

            // Las tablas nuevas existen y están vacías.
            assertEquals(0, db.transactionDao().observeByFarm("finca-1").first().size)
            assertEquals(0, db.farmContactDao().observeByFarm("finca-1").first().size)
            assertEquals(0, db.reminderDao().observeByFarm("finca-1").first().size)
            // v7 (#32): tabla de borrados pendientes.
            assertEquals(0, db.pendingDeleteDao().getRetryable().size)
        } finally {
            db.close()
        }
    }

    /** Esquema v5 = lo que generaba Room con las entidades de esa versión. */
    private fun createVersion5Database() {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE `inspection_routes` (`id` TEXT NOT NULL, `farmId` TEXT NOT NULL, " +
                    "`userId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, " +
                    "`status` TEXT NOT NULL, `totalDistanceMeters` REAL NOT NULL, " +
                    "`pointCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE `route_points` (`id` TEXT NOT NULL, `routeId` TEXT NOT NULL, " +
                    "`order` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, " +
                    "`accuracy` REAL NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`routeId`) REFERENCES `inspection_routes`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX `index_route_points_routeId` ON `route_points` (`routeId`)")
            db.execSQL(
                "CREATE TABLE `animals` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`notes` TEXT NOT NULL, `photoUri` TEXT, `latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, `locationAccuracy` REAL, `createdAt` INTEGER NOT NULL, " +
                    "`variant` INTEGER NOT NULL DEFAULT 0, `ownerUid` TEXT, PRIMARY KEY(`id`))"
            )
            db.execSQL("CREATE INDEX `index_animals_ownerUid` ON `animals` (`ownerUid`)")
            db.execSQL(
                "CREATE TABLE `categories` (`id` TEXT NOT NULL, `farmId` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`isDefault` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX `index_categories_farmId_type_normalizedName` " +
                    "ON `categories` (`farmId`, `type`, `normalizedName`)"
            )

            db.execSQL(
                "INSERT INTO animals VALUES " +
                    "('animal-1', 'Lola', '', NULL, 5.0, -75.5, NULL, 1000, 2, 'uid-1')"
            )
            db.execSQL(
                "INSERT INTO inspection_routes VALUES " +
                    "('route-1', '', 'uid-1', 1000, 2000, 'COMPLETED', 120.5, 1)"
            )
            db.execSQL(
                "INSERT INTO route_points VALUES ('p-1', 'route-1', 0, 5.0, -75.5, 3.0, 1500)"
            )
            db.execSQL(
                "INSERT INTO categories VALUES " +
                    "('cat-1', 'finca-1', 'Venta de leche', 'venta de leche', 'INCOME', 1, 'SYNCED')"
            )
            db.version = 5
        }
    }
}
