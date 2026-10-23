package com.sge.finca.platform.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PhotoFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun createTemporaryCameraFile(): Result<File> = storageOperation {
        createUniqueFile(
            directory = File(context.cacheDir, "camera"),
            extension = "jpg",
        )
    }

    suspend fun persistCameraPhoto(
        temporaryFile: File,
    ): Result<File> = storageOperation {
        val cameraDirectory = File(context.cacheDir, "camera").canonicalFile

        require(temporaryFile.canonicalFile.parentFile == cameraDirectory) {
            "El archivo debe pertenecer a la carpeta temporal de cámara."
        }

        if (!temporaryFile.isFile || temporaryFile.length() == 0L) {
            throw IOException("La cámara no generó una foto válida.")
        }

        copyToPhotos(extension = "jpg") {
            temporaryFile.inputStream()
        }
    }

    suspend fun copyGalleryPhoto(
        sourceUri: Uri,
    ): Result<File> = storageOperation {
        val mimeType = context.contentResolver
            .getType(sourceUri)
            ?.lowercase(Locale.ROOT)

        require(mimeType != null && mimeType.startsWith("image/")) {
            "El archivo seleccionado no tiene un tipo de imagen reconocido."
        }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: "img"

        copyToPhotos(extension) {
            context.contentResolver.openInputStream(sourceUri)
                ?: throw IOException("No se pudo abrir la imagen seleccionada.")
        }
    }

    suspend fun cleanupTemporaryCameraFiles(
        protectedFileNames: Set<String> = emptySet(),
    ): Result<Int> = storageOperation {
        val directory = File(context.cacheDir, "camera")

        if (!directory.exists()) {
            return@storageOperation 0
        }

        val files = directory.listFiles()
            ?: throw IOException("No se pudo revisar la carpeta temporal.")

        val expirationTime =
            System.currentTimeMillis() - 24L * 60 * 60 * 1000

        val canonicalDirectory = directory.canonicalFile
        var deletedCount = 0

        for (file in files) {
            // Solo revisamos archivos de cámara creados por este administrador.
            if (!file.isFile ||
                !file.name.startsWith("photo_") ||
                file.extension != "jpg"
            ) {
                continue
            }

            // Permite conservar archivos que todavía estén en uso.
            if (file.name in protectedFileNames) {
                continue
            }

            // Evita seguir enlaces hacia archivos fuera de esta carpeta.
            if (file.canonicalFile.parentFile != canonicalDirectory) {
                continue
            }

            val lastModified = file.lastModified()

            // Una fecha desconocida o reciente no permite eliminar el archivo.
            if (lastModified <= 0L || lastModified >= expirationTime) {
                continue
            }

            if (file.delete()) {
                deletedCount++
            } else if (file.exists()) {
                throw IOException(
                    "No se pudo eliminar el temporal: ${file.name}"
                )
            }
        }

        deletedCount
    }

    private fun createUniqueFile(
        directory: File,
        extension: String,
    ): File {
        require(extension.matches(Regex("[a-zA-Z0-9]+"))) {
            "La extensión del archivo no es válida."
        }

        if (!directory.isDirectory &&
            !directory.mkdirs() &&
            !directory.isDirectory
        ) {
            throw IOException("No se pudo crear la carpeta de fotos.")
        }

        val fileName =
            "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"

        val file = File(directory, fileName)

        if (!file.createNewFile()) {
            throw IOException("No se pudo crear un archivo único.")
        }

        return file
    }

    private fun copyToPhotos(
        extension: String,
        openInput: () -> InputStream,
    ): File {
        val destination = createUniqueFile(
            directory = File(context.filesDir, "photos"),
            extension = extension,
        )

        var completed = false

        try {
            val copiedBytes = openInput().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (copiedBytes == 0L) {
                throw IOException("La imagen está vacía.")
            }

            completed = true
            return destination
        } finally {
            if (!completed) {
                destination.delete()
            }
        }
    }

    private suspend fun <T> storageOperation(
        operation: () -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(operation())
        } catch (error: IOException) {
            Result.failure(error)
        } catch (error: SecurityException) {
            Result.failure(error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        }
    }
}