package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.TransactionInput
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.format.Cop

/**
 * Campos del formulario de transacción, compartidos por crear y editar.
 * [amountDigits] son solo los dígitos de los pesos ("50000"); se muestra como "$50.000".
 */
data class TransactionFormState(
    val type: TransactionType = TransactionType.INCOME,
    val amountDigits: String = "",
    val categoryId: String? = null,
    val dateEpochDay: Long = BusinessDate.today(),
    val note: String = "",
    /** Todas las categorías de la finca; el formulario muestra solo las del tipo elegido. */
    val categories: List<Category> = emptyList(),
) {
    val categoriesForType: List<Category>
        get() = categories.filter { it.type == type }

    val selectedCategory: Category?
        get() = categoriesForType.firstOrNull { it.id == categoryId }

    /** Al cambiar de ingreso a egreso, la categoría elegida deja de servir. */
    fun withType(newType: TransactionType): TransactionFormState =
        if (newType == type) this else copy(type = newType, categoryId = null)

    /**
     * Valida en el dispositivo antes de guardar (el repositorio vuelve a validar como respaldo).
     * Los mensajes están pensados para mostrarse tal cual al usuario.
     */
    fun validate(today: Long = BusinessDate.today()): TransactionFormValidation {
        val amountCents = Cop.inputToCents(amountDigits)
        return when {
            amountCents == null || amountCents <= 0 ->
                TransactionFormValidation.Invalid("Escribe un monto mayor que cero")
            amountCents > MAX_AMOUNT_CENTS ->
                TransactionFormValidation.Invalid("El monto no puede superar ${Cop.format(MAX_AMOUNT_CENTS)}")
            selectedCategory == null ->
                TransactionFormValidation.Invalid("Selecciona una categoría")
            dateEpochDay > today ->
                TransactionFormValidation.Invalid("La fecha no puede ser futura")
            note.length > MAX_NOTE_LENGTH ->
                TransactionFormValidation.Invalid("La nota no puede pasar de $MAX_NOTE_LENGTH caracteres")
            else -> TransactionFormValidation.Valid(
                TransactionInput(
                    type = type,
                    amountCents = amountCents,
                    categoryId = selectedCategory!!.id,
                    dateEpochDay = dateEpochDay,
                    note = note.trim().ifEmpty { null },
                ),
            )
        }
    }

    companion object {
        /** Igual al tope del repositorio: 10.000 millones de pesos. */
        const val MAX_AMOUNT_CENTS = 1_000_000_000_000L
        const val MAX_NOTE_LENGTH = 500
    }
}

sealed interface TransactionFormValidation {
    data class Valid(val input: TransactionInput) : TransactionFormValidation
    data class Invalid(val message: String) : TransactionFormValidation
}
