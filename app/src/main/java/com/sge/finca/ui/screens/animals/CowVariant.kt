package com.sge.finca.ui.screens.animals

import androidx.annotation.DrawableRes
import com.sge.finca.R

/**
 * Apariencias disponibles para representar a una vaca (en la lista, el registro y el mapa).
 * El [id] es lo que se guarda en Room: no cambiar los valores existentes.
 */
enum class CowVariant(val id: Int, val label: String, @DrawableRes val sheetRes: Int) {
    HOLSTEIN(0, "Holstein", R.drawable.cow_sheet),
    BROWN(1, "Manchas café", R.drawable.cow_sheet_brown),
    TAN(2, "Caramelo", R.drawable.cow_sheet_tan),
    BLACK(3, "Negra", R.drawable.cow_sheet_black);

    companion object {
        fun fromId(id: Int): CowVariant = entries.firstOrNull { it.id == id } ?: HOLSTEIN
    }
}
