package com.sge.finca.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Pesos colombianos (COP).
 *
 * - En la base de datos los montos van en **centavos** (Long) para no perder precisión.
 * - En pantalla se muestran **sin decimales** y con punto de miles: 5_000_000 centavos → "$50.000".
 * - Al escribir, el campo guarda solo los dígitos de los pesos ("50000"); el punto de miles lo
 *   pinta [com.sge.finca.ui.components.ThousandsSeparatorTransformation].
 */
object Cop {
    private const val CENTS_PER_PESO = 100L

    /** Hasta 99.999.999.999 pesos al escribir; el repositorio aplica su propio tope de negocio. */
    const val MAX_INPUT_DIGITS = 11

    private val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /** 5_000_000 → "$50.000"; -5_000_000 → "-$50.000". */
    fun format(cents: Long): String {
        val pesos = DecimalFormat("#,##0", symbols).format(abs(cents / CENTS_PER_PESO))
        return if (cents < 0) "-\$$pesos" else "\$$pesos"
    }

    /**
     * Limpia lo que escribe o pega el usuario: solo dígitos de la parte entera, sin ceros a la
     * izquierda y con tope de longitud. "$ 50.000,50" → "50000".
     */
    fun sanitizeInput(raw: String): String =
        raw.substringBefore(',')
            .filter { it in '0'..'9' }
            .trimStart('0')
            .take(MAX_INPUT_DIGITS)

    /** "50000" → 5_000_000 centavos; null si el campo está vacío. */
    fun inputToCents(digits: String): Long? = digits.toLongOrNull()?.times(CENTS_PER_PESO)

    /** 5_000_000 centavos → "50000", para cargar un monto en el campo al editar. */
    fun centsToInput(cents: Long): String = (cents / CENTS_PER_PESO).takeIf { it > 0 }?.toString() ?: ""

    /** "50000" → "50.000". */
    fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val out = StringBuilder(digits.length + digits.length / 3)
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
            out.append(c)
        }
        return out.toString()
    }
}
