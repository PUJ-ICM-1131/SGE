package com.sge.finca.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.TransactionEntity
import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Criterio del issue #23: insertar una transacción en Room y leerla de vuelta. */
@RunWith(AndroidJUnit4::class)
class TransactionDaoInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertsAndReadsBackTransaction() = runBlocking {
        val dao = db.transactionDao()
        val tx = transaction(id = "t1", amountCents = 2_500_000L, day = BusinessDate.of(2026, 11, 20))

        dao.insert(tx)

        assertEquals(tx, dao.getById(FARM, "t1"))
        assertEquals(listOf(tx), dao.observeByFarm(FARM).first())
        // Otra finca no ve la transacción.
        assertNull(dao.getById("otra-finca", "t1"))
    }

    @Test
    fun filtersByDateRangeTypeAndCategory() = runBlocking {
        val dao = db.transactionDao()
        val nov1 = BusinessDate.of(2026, 11, 1)
        val nov15 = BusinessDate.of(2026, 11, 15)
        val dec1 = BusinessDate.of(2026, 12, 1)
        dao.insert(transaction("a", 100L, nov1, TransactionType.INCOME, "leche"))
        dao.insert(transaction("b", 200L, nov15, TransactionType.EXPENSE, "sanidad"))
        dao.insert(transaction("c", 300L, dec1, TransactionType.INCOME, "leche"))

        val november = dao.observeFiltered(FARM, nov1, BusinessDate.of(2026, 11, 30), null, null).first()
        assertEquals(listOf("b", "a"), november.map { it.id }) // más reciente primero

        val incomes = dao.observeFiltered(FARM, null, null, TransactionType.INCOME.name, null).first()
        assertEquals(listOf("c", "a"), incomes.map { it.id })

        val sanidad = dao.observeFiltered(FARM, null, null, null, "sanidad").first()
        assertEquals(listOf("b"), sanidad.map { it.id })
    }

    @Test
    fun markSyncedIgnoresOutdatedConfirmation() = runBlocking {
        val dao = db.transactionDao()
        val original = transaction("t1", 100L, BusinessDate.of(2026, 11, 20))
        dao.insert(original)
        val edited = original.copy(amountCents = 999L, updatedAt = original.updatedAt + 1)
        dao.update(edited)

        // Llega la confirmación de Firestore de la versión VIEJA: no debe marcar SYNCED.
        dao.markSynced(FARM, "t1", original.updatedAt)
        assertEquals(SyncStatus.PENDING, dao.getById(FARM, "t1")?.syncStatus)

        dao.markSynced(FARM, "t1", edited.updatedAt)
        assertEquals(SyncStatus.SYNCED, dao.getById(FARM, "t1")?.syncStatus)
    }

    @Test
    fun categoriesKeepEnumTypes() = runBlocking {
        val dao = db.categoryDao()
        dao.insert(
            CategoryEntity(
                id = "c1", farmId = FARM, name = "Venta de leche",
                normalizedName = "venta de leche", type = TransactionType.INCOME, isDefault = true,
            )
        )
        val incomes = dao.observeByFarmAndType(FARM, TransactionType.INCOME.name).first()
        assertEquals(TransactionType.INCOME, incomes.single().type)
        assertEquals(0, dao.observeByFarmAndType(FARM, TransactionType.EXPENSE.name).first().size)
    }

    private fun transaction(
        id: String,
        amountCents: Long,
        day: Long,
        type: TransactionType = TransactionType.INCOME,
        categoryId: String = "leche",
    ) = TransactionEntity(
        id = id,
        farmId = FARM,
        type = type,
        amountCents = amountCents,
        categoryId = categoryId,
        categoryName = categoryId,
        dateEpochDay = day,
        note = null,
        paymentMethod = null,
        photoUri = null,
        remotePhotoUrl = null,
        createdBy = "uid-1",
        createdByName = "Admin",
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private companion object {
        const val FARM = "finca-1"
    }
}
