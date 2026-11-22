package com.sge.finca.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sge.finca.data.local.entity.PendingDeleteEntity
import com.sge.finca.data.local.entity.TransactionEntity
import com.sge.finca.domain.model.SyncState
import com.sge.finca.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Consultas de la cola de sincronización (issue #32). */
@RunWith(AndroidJUnit4::class)
class SyncQueueInstrumentedTest {

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
    fun offlineTransactionIsPendingUntilMarkedSynced() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(tx("t1"))

        assertEquals(listOf("t1"), dao.getPendingUpload().map { it.id })
        assertEquals(1, dao.observeCountByStatus(FARM, SyncStatus.PENDING.name).first())
        assertEquals(SyncState.PENDING, dao.getById(FARM, "t1")!!.toDomain().syncState)

        dao.markSynced(FARM, "t1", 1_000L)

        assertTrue(dao.getPendingUpload().isEmpty())
        assertEquals(0, dao.observeCountByStatus(FARM, SyncStatus.PENDING.name).first())
        assertEquals(SyncState.SYNCED, dao.getById(FARM, "t1")!!.toDomain().syncState)
    }

    @Test
    fun rejectedTransactionIsNotRetriedUntilReset() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(tx("t1"))
        dao.markFailed(FARM, "t1", 1_000L)

        // FAILED no vuelve a la cola sola: no hay reintentos infinitos.
        assertTrue(dao.getPendingUpload().isEmpty())
        assertEquals(1, dao.observeCountByStatus(FARM, SyncStatus.FAILED.name).first())

        dao.resetFailed(FARM)
        assertEquals(listOf("t1"), dao.getPendingUpload().map { it.id })
    }

    @Test
    fun confirmationOfOldVersionDoesNotHideNewEdit() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(tx("t1"))
        dao.update(tx("t1").copy(amountCents = 999L, updatedAt = 2_000L))

        dao.markFailed(FARM, "t1", 1_000L)
        dao.markSynced(FARM, "t1", 1_000L)

        assertEquals(SyncStatus.PENDING, dao.getById(FARM, "t1")!!.syncStatus)
    }

    @Test
    fun pendingDeletesAreQueuedAndFailedOnesSkipped() = runBlocking {
        val dao = db.pendingDeleteDao()
        dao.insert(delete("t1", createdAt = 1))
        dao.insert(delete("t2", createdAt = 2))

        assertTrue(dao.exists(FARM, "transactions", "t1"))
        assertEquals(listOf("t1", "t2"), dao.getRetryable().map { it.docId })
        assertEquals(2, dao.observeCount(FARM, failed = false).first())

        dao.markFailed("farms/$FARM/transactions/t2")
        assertEquals(listOf("t1"), dao.getRetryable().map { it.docId })
        assertEquals(1, dao.observeCount(FARM, failed = true).first())

        dao.remove("farms/$FARM/transactions/t1")
        assertFalse(dao.exists(FARM, "transactions", "t1"))

        dao.resetFailed(FARM)
        assertEquals(listOf("t2"), dao.getRetryable().map { it.docId })
    }

    @Test
    fun syncedIdsOnlyListConfirmedRows() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(tx("t1"))
        dao.upsertAll(listOf(tx("t2").copy(syncStatus = SyncStatus.SYNCED)))

        assertEquals(listOf("t2"), dao.getSyncedIds(FARM))
    }

    private fun tx(id: String) = TransactionEntity(
        id = id,
        farmId = FARM,
        type = TransactionType.EXPENSE,
        amountCents = 100L,
        categoryId = "c1",
        categoryName = "Sanidad",
        dateEpochDay = 20_778L,
        note = null,
        paymentMethod = null,
        photoUri = null,
        remotePhotoUrl = null,
        createdBy = "uid-1",
        createdByName = null,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun delete(id: String, createdAt: Long) = PendingDeleteEntity(
        path = "farms/$FARM/transactions/$id",
        farmId = FARM,
        collection = "transactions",
        docId = id,
        createdAt = createdAt,
    )

    private companion object {
        const val FARM = "finca-1"
    }
}
