package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFormStateTest {

    private val today = BusinessDate.of(2026, 11, 22)
    private val leche = Category("c1", "f1", "Venta de leche", TransactionType.INCOME, isDefault = true)
    private val sanidad = Category("c2", "f1", "Sanidad", TransactionType.EXPENSE, isDefault = true)

    private val valid = TransactionFormState(
        type = TransactionType.INCOME,
        amountDigits = "50000",
        categoryId = leche.id,
        dateEpochDay = today,
        note = "  Leche de la semana  ",
        categories = listOf(leche, sanidad),
    )

    private fun errorOf(form: TransactionFormState) =
        (form.validate(today) as TransactionFormValidation.Invalid).message

    @Test
    fun `un formulario valido produce el input en centavos`() {
        val input = (valid.validate(today) as TransactionFormValidation.Valid).input
        assertEquals(5_000_000L, input.amountCents)
        assertEquals("c1", input.categoryId)
        assertEquals(TransactionType.INCOME, input.type)
        assertEquals("Leche de la semana", input.note)
    }

    @Test
    fun `nota vacia se guarda como null`() {
        val input = (valid.copy(note = "   ").validate(today) as TransactionFormValidation.Valid).input
        assertNull(input.note)
    }

    @Test
    fun `el monto es obligatorio y mayor que cero`() {
        assertEquals("Escribe un monto mayor que cero", errorOf(valid.copy(amountDigits = "")))
        assertEquals("Escribe un monto mayor que cero", errorOf(valid.copy(amountDigits = "0")))
    }

    @Test
    fun `el monto tiene tope de 10 mil millones`() {
        assertTrue(errorOf(valid.copy(amountDigits = "10000000001")).startsWith("El monto no puede superar"))
    }

    @Test
    fun `la categoria es obligatoria y debe ser del tipo elegido`() {
        assertEquals("Selecciona una categoría", errorOf(valid.copy(categoryId = null)))
        // "Sanidad" es de egreso: no sirve para un ingreso.
        assertEquals("Selecciona una categoría", errorOf(valid.copy(categoryId = sanidad.id)))
    }

    @Test
    fun `cambiar el tipo limpia la categoria`() {
        val expense = valid.withType(TransactionType.EXPENSE)
        assertNull(expense.categoryId)
        assertEquals(listOf(sanidad), expense.categoriesForType)
        assertEquals(valid, valid.withType(TransactionType.INCOME))
    }

    @Test
    fun `no se permiten fechas futuras`() {
        assertEquals("La fecha no puede ser futura", errorOf(valid.copy(dateEpochDay = today + 1)))
    }
}
