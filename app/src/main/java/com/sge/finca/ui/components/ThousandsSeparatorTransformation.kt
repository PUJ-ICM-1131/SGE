package com.sge.finca.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sge.finca.ui.format.Cop

/**
 * Muestra "50000" como "50.000" mientras se escribe, sin cambiar el valor del campo
 * (que sigue siendo solo dígitos). El cursor se mantiene en el lugar correcto.
 */
object ThousandsSeparatorTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = Cop.groupDigits(digits)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val n = digits.length
                // Puntos que quedan antes de la posición: uno por cada grupo de 3 cerrado a su izquierda.
                val separators = (1 until offset.coerceIn(0, n)).count { i -> (n - i) % 3 == 0 }
                return offset + separators
            }

            override fun transformedToOriginal(offset: Int): Int =
                grouped.take(offset.coerceIn(0, grouped.length)).count { it != '.' }
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }
}
