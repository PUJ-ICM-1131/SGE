package com.sge.finca.ui.screens.animals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Poses de la vaca. Cada una es una fila de la hoja de sprites res/drawable-nodpi/cow_sheet.png
 * (4 columnas x 2 filas, la vaca mira hacia la DERECHA).
 *  fila 0: caminando (4 fotogramas) -> también se usa, más rápido, para correr
 *  fila 1: quieta pastando (4 fotogramas, animación lenta)
 */
enum class CowPose(val row: Int, val frames: Int, val frameMillis: Long) {
    IDLE(row = 1, frames = 4, frameMillis = 500L),
    WALK(row = 0, frames = 4, frameMillis = 140L),
    RUN(row = 0, frames = 4, frameMillis = 75L),
}

/**
 * Corta la hoja de sprites en fotogramas ya listos para usar como ícono de marcador
 * (uno mirando al este y otro espejado mirando al oeste).
 * Debe crearse cuando el mapa ya está inicializado (dentro del contenido de GoogleMap).
 */
class CowSprites private constructor(
    private val east: Map<CowPose, List<BitmapDescriptor>>,
    private val west: Map<CowPose, List<BitmapDescriptor>>,
) {

    fun frame(pose: CowPose, index: Int, facingEast: Boolean): BitmapDescriptor {
        val frames = (if (facingEast) east else west).getValue(pose)
        return frames[index % frames.size]
    }

    companion object {
        private const val COLUMNS = 4
        private const val ROWS = 2

        fun load(context: Context, widthPx: Int, variant: CowVariant = CowVariant.HOLSTEIN): CowSprites {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, variant.sheetRes, options)
            val cellW = sheet.width / COLUMNS
            val cellH = sheet.height / ROWS
            val heightPx = widthPx * cellH / cellW
            val mirror = Matrix().apply { postScale(-1f, 1f) }

            val east = HashMap<CowPose, List<BitmapDescriptor>>()
            val west = HashMap<CowPose, List<BitmapDescriptor>>()
            for (pose in CowPose.entries) {
                val eastFrames = ArrayList<BitmapDescriptor>()
                val westFrames = ArrayList<BitmapDescriptor>()
                for (i in 0 until pose.frames) {
                    val cell = Bitmap.createBitmap(sheet, i * cellW, pose.row * cellH, cellW, cellH)
                    val scaled = Bitmap.createScaledBitmap(cell, widthPx, heightPx, true)
                    eastFrames.add(BitmapDescriptorFactory.fromBitmap(scaled))
                    val flipped = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, mirror, true)
                    westFrames.add(BitmapDescriptorFactory.fromBitmap(flipped))
                }
                east[pose] = eastFrames
                west[pose] = westFrames
            }
            return CowSprites(east, west)
        }
    }
}
