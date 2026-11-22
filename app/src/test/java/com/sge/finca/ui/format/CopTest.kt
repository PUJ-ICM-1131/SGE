package com.sge.finca.ui.format

import com.sge.finca.ui.components.ThousandsSeparatorTransformation
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopTest {

    @Test
    fun `formatea pesos colombianos sin decimales y con punto de miles`() {
        assertEquals("$0", Cop.format(0))
        assertEquals("$500", Cop.format(50_000))
        assertEquals("$50.000", Cop.format(5_000_000))
        assertEquals("$1.250.000", Cop.format(125_000_000))
        assertEquals("-$30.000", Cop.format(-3_000_000))
    }

    @Test
    fun `limpia lo que escribe o pega el usuario`() {
        assertEquals("50000", Cop.sanitizeInput("50000"))
        assertEquals("50000", Cop.sanitizeInput("$ 50.000"))
        assertEquals("50000", Cop.sanitizeInput("50.000,99")) // los centavos no se escriben
        assertEquals("1200", Cop.sanitizeInput("001200"))
        assertEquals("", Cop.sanitizeInput("abc"))
        assertEquals(Cop.MAX_INPUT_DIGITS, Cop.sanitizeInput("9".repeat(20)).length)
    }

    @Test
    fun `convierte entre pesos escritos y centavos guardados`() {
        assertEquals(5_000_000L, Cop.inputToCents("50000"))
        assertNull(Cop.inputToCents(""))
        assertEquals("50000", Cop.centsToInput(5_000_000))
        assertEquals("", Cop.centsToInput(0))
    }

    @Test
    fun `agrupa los digitos de a tres`() {
        assertEquals("", Cop.groupDigits(""))
        assertEquals("999", Cop.groupDigits("999"))
        assertEquals("1.000", Cop.groupDigits("1000"))
        assertEquals("99.999.999.999", Cop.groupDigits("99999999999"))
    }

    @Test
    fun `el cursor queda en su sitio con los puntos de miles`() {
        for (length in 0..Cop.MAX_INPUT_DIGITS) {
            val digits = "1234567890123".take(length)
            val transformed = ThousandsSeparatorTransformation.filter(AnnotatedString(digits))
            val mapping = transformed.offsetMapping
            for (offset in 0..length) {
                val t = mapping.originalToTransformed(offset)
                assertEquals("ida y vuelta en '$digits' posición $offset", offset, mapping.transformedToOriginal(t))
                assertEquals(digits.take(offset), transformed.text.text.take(t).replace(".", ""))
            }
        }
    }

    @Test
    fun `fechas en espanol sin punto en el mes`() {
        val oct20 = com.sge.finca.domain.model.BusinessDate.of(2026, 10, 20)
        assertEquals("20 oct 2026", BusinessDateFormat.format(oct20))
        assertEquals(oct20, BusinessDateFormat.fromUtcMillis(BusinessDateFormat.toUtcMillis(oct20)))
    }
}
