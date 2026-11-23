package com.sge.finca.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Área de un potrero (#71): siempre en m²; desde 1 ha se agregan las hectáreas. */
object AreaText {
    private const val M2_PER_HECTARE = 10_000.0

    private val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /** 1250.4 → "1.250 m²"; 23_456.0 → "23.456 m² (2,35 ha)". */
    fun format(m2: Double): String {
        val squareMeters = DecimalFormat("#,##0", symbols).format(m2) + " m²"
        if (m2 < M2_PER_HECTARE) return squareMeters
        val hectares = DecimalFormat("#,##0.##", symbols).format(m2 / M2_PER_HECTARE)
        return "$squareMeters ($hectares ha)"
    }
}
