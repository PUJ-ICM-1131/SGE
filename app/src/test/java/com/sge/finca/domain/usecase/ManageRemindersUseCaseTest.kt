package com.sge.finca.domain.usecase

import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Ciclo de vida de los avisos: cada cambio de datos debe programar, reprogramar o cancelar. */
class ManageRemindersUseCaseTest {

    private val now = System.currentTimeMillis()
    private val inOneHour = now + 3_600_000
    private val inTwoHours = now + 7_200_000

    private val repository = FakeReminderRepository()
    private val scheduler = RecordingScheduler()
    private val useCase = ManageRemindersUseCase(repository, scheduler)

    @Test
    fun `crear programa el aviso`() = runBlocking {
        val id = useCase.create("f1", "Vacunar terneros", null, inOneHour, now)
        assertEquals(listOf("schedule:$id@$inOneHour"), scheduler.calls)
    }

    @Test
    fun `editar la fecha reprograma con el mismo id`() = runBlocking {
        val id = useCase.create("f1", "Vacunar", null, inOneHour, now)
        val saved = repository.getReminder("f1", id)!!
        useCase.update(saved.copy(dueAt = inTwoHours), now)
        // Mismo id: WorkManager reemplaza el trabajo anterior, no queda duplicado.
        assertEquals("schedule:$id@$inTwoHours", scheduler.calls.last())
    }

    @Test
    fun `completar cancela y reabrir vuelve a programar`() = runBlocking {
        val id = useCase.create("f1", "Pagar luz", null, inOneHour, now)
        useCase.setCompleted("f1", id, completed = true)
        assertEquals("cancel:$id", scheduler.calls.last())
        useCase.setCompleted("f1", id, completed = false)
        assertEquals("schedule:$id@$inOneHour", scheduler.calls.last())
    }

    @Test
    fun `eliminar cancela el aviso y borra el dato`() = runBlocking {
        val id = useCase.create("f1", "Comprar sal", null, inOneHour, now)
        useCase.delete("f1", id)
        assertEquals("cancel:$id", scheduler.calls.last())
        assertNull(repository.getReminder("f1", id))
    }

    @Test
    fun `no se crea un recordatorio en el pasado ni sin titulo`() = runBlocking {
        expectError("Elige una fecha y hora futura") { useCase.create("f1", "Tarde", null, now - 1, now) }
        expectError("Escribe un título") { useCase.create("f1", "  ", null, inOneHour, now) }
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun `editar solo el titulo de uno vencido no exige fecha futura`() = runBlocking {
        val id = useCase.create("f1", "Revisar cerca", null, inOneHour, now)
        val later = inOneHour + 60_000 // ya venció
        useCase.update(repository.getReminder("f1", id)!!.copy(title = "Revisar cerca norte"), later)
        assertEquals("Revisar cerca norte", repository.getReminder("f1", id)!!.title)
    }

    @Test
    fun `validacion de longitudes`() {
        assertEquals(
            "El título no puede pasar de 80 caracteres",
            ReminderValidation.errorFor("x".repeat(81), null, inOneHour, now, requireFuture = true),
        )
        assertNull(ReminderValidation.errorFor("Vacunar", "ok", inOneHour, now, requireFuture = true))
    }

    private suspend fun expectError(message: String, block: suspend () -> Unit) {
        try {
            block()
            fail("Se esperaba: $message")
        } catch (e: IllegalArgumentException) {
            assertEquals(message, e.message)
        }
    }

    private class RecordingScheduler : ReminderScheduler {
        val calls = mutableListOf<String>()
        override fun schedule(reminder: Reminder) {
            calls += "schedule:${reminder.id}@${reminder.dueAt}"
        }
        override fun cancel(reminderId: String) {
            calls += "cancel:$reminderId"
        }
    }

    private class FakeReminderRepository : ReminderRepository {
        private val items = MutableStateFlow<Map<String, Reminder>>(emptyMap())
        private var nextId = 1

        override fun observeReminders(farmId: String): Flow<List<Reminder>> =
            items.map { all -> all.values.filter { it.farmId == farmId } }

        override fun observePending(farmId: String): Flow<List<Reminder>> =
            observeReminders(farmId).map { list -> list.filter { !it.isCompleted } }

        override suspend fun getReminder(farmId: String, id: String): Reminder? =
            items.value[id]?.takeIf { it.farmId == farmId }

        override suspend fun create(farmId: String, title: String, description: String?, dueAt: Long): String {
            val id = "r${nextId++}"
            items.value += id to Reminder(id, farmId, title.trim(), description, dueAt, false, "u1", 0, 0)
            return id
        }

        override suspend fun update(reminder: Reminder) {
            items.value += reminder.id to reminder
        }

        override suspend fun setCompleted(farmId: String, id: String, completed: Boolean) {
            items.value[id]?.let { items.value += id to it.copy(isCompleted = completed) }
        }

        override suspend fun delete(farmId: String, id: String) {
            items.value -= id
        }
    }
}
