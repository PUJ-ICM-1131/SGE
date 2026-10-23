package com.sge.finca.ui.screens.animals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.FilterQuality

/** Selector de la apariencia de la vaca: 4 tarjetas con la vaca quieta de cada variante. */
@Composable
fun CowVariantPicker(
    selected: CowVariant,
    onSelected: (CowVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Apariencia en el mapa", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CowVariant.entries.forEach { variant ->
                val isSelected = variant == selected
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelected(variant) },
                            role = Role.RadioButton,
                        ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CowPreview(variant, Modifier.fillMaxWidth().aspectRatio(256f / 192f))
                        Text(
                            text = variant.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

/** Dibuja el primer fotograma de la fila "quieta" (fila 1) de la hoja de sprites. */
@Composable
private fun CowPreview(variant: CowVariant, modifier: Modifier = Modifier) {
    val sheet: ImageBitmap = ImageBitmap.imageResource(variant.sheetRes)
    val cellW = sheet.width / 4
    val cellH = sheet.height / 2
    Canvas(modifier = modifier) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(0, cellH),
            srcSize = IntSize(cellW, cellH),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
