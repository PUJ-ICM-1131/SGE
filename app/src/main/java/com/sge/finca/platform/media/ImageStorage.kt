package com.sge.finca.platform.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maneja las fotos de los animales en el almacenamiento PRIVADO de la app
 * (filesDir/animal_photos). Nadie más puede leerlas sin pasar por FileProvider.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }

    private val authority: String
        get() = "${context.packageName}.fileprovider"

    /** Archivo nuevo y vacío donde CameraX va a escribir la foto. */
    fun createPhotoFile(): File = File(photosDir, "${UUID.randomUUID()}.jpg")

    /** URI segura (content://) para un archivo de la carpeta de fotos. */
    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, authority, file)

    /**
     * Copia la imagen elegida en la galería (Photo Picker) a nuestro almacenamiento,
     * porque el URI del picker deja de ser válido más adelante.
     * Devuelve null si no se pudo copiar.
     */
    suspend fun copyFromGallery(source: Uri): Uri? = withContext(Dispatchers.IO) {
        val target = createPhotoFile()
        try {
            val input = context.contentResolver.openInputStream(source) ?: return@withContext null
            input.use { stream ->
                target.outputStream().use { output -> stream.copyTo(output) }
            }
            uriFor(target)
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    /** Borra una foto previamente guardada (si pertenece a nuestra carpeta). */
    suspend fun delete(uri: String) = withContext(Dispatchers.IO) { deleteNow(uri) }

    /** Versión sin corrutina, para usar en onCleared() del ViewModel. */
    fun deleteNow(uri: String) {
        val name = Uri.parse(uri).lastPathSegment ?: return
        File(photosDir, name).takeIf { it.exists() }?.delete()
    }

    private companion object {
        const val PHOTOS_DIR = "animal_photos"
    }
}
