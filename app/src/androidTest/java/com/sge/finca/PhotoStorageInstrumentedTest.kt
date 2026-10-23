package com.sge.finca

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sge.finca.platform.storage.FileProviderHelper
import com.sge.finca.platform.storage.PhotoFileManager
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sge.finca.platform.storage.PhotoCleanupWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.filterNotNull

@RunWith(AndroidJUnit4::class)
class PhotoStorageInstrumentedTest {

    @Test
    fun savesSharesAndCleansPhotos() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val manager = PhotoFileManager(context)
        val provider = FileProviderHelper(context)
        val photoBytes = byteArrayOf(1, 2, 3, 4)

        val temporaryFile = manager
            .createTemporaryCameraFile()
            .getOrThrow()

        var savedPhoto: File? = null

        try {
            temporaryFile.writeBytes(photoBytes)

            savedPhoto = manager
                .persistCameraPhoto(temporaryFile)
                .getOrThrow()

            assertTrue(savedPhoto.isFile)
            assertArrayEquals(photoBytes, savedPhoto.readBytes())

            val uri = provider.getUriForFile(savedPhoto)
            val readBytes = context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }

            assertArrayEquals(photoBytes, readBytes)

            val shareIntent = provider.createShareIntent(
                savedPhoto,
                "image/jpeg",
            )

            assertEquals(Intent.ACTION_SEND, shareIntent.action)
            assertTrue(
                shareIntent.flags and
                        Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
            )

            // Simulamos que el temporal tiene más de 24 horas.
            assertTrue(
                temporaryFile.setLastModified(
                    System.currentTimeMillis() -
                            25L * 60 * 60 * 1000
                )
            )

            val deletedCount = manager
                .cleanupTemporaryCameraFiles()
                .getOrThrow()

            assertTrue(deletedCount >= 1)
            assertFalse(temporaryFile.exists())
            assertTrue(savedPhoto.isFile)
        } finally {
            temporaryFile.delete()
            savedPhoto?.delete()
        }
    }
    @Test
    fun copiesImageUriWithUniqueNames() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val manager = PhotoFileManager(context)
        val provider = FileProviderHelper(context)
        val imageBytes = byteArrayOf(10, 20, 30, 40)

        val source = manager.createTemporaryCameraFile().getOrThrow()
        var firstCopy: File? = null
        var secondCopy: File? = null

        try {
            source.writeBytes(imageBytes)
            val sourceUri = provider.getUriForFile(source)

            firstCopy = manager.copyGalleryPhoto(sourceUri).getOrThrow()
            secondCopy = manager.copyGalleryPhoto(sourceUri).getOrThrow()

            assertArrayEquals(imageBytes, firstCopy.readBytes())
            assertArrayEquals(imageBytes, secondCopy.readBytes())
            assertTrue(firstCopy.name != secondCopy.name)
            assertEquals(
                File(context.filesDir, "photos").canonicalFile,
                firstCopy.parentFile?.canonicalFile,
            )
        } finally {
            source.delete()
            firstCopy?.delete()
            secondCopy?.delete()
        }
    }
    @Test
    fun workManagerExecutesCleanupWorker() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val manager = PhotoFileManager(context)
        val oldTemporaryFile = manager
            .createTemporaryCameraFile()
            .getOrThrow()

        try {
            oldTemporaryFile.writeBytes(byteArrayOf(1, 2, 3))

            assertTrue(
                oldTemporaryFile.setLastModified(
                    System.currentTimeMillis() -
                            25L * 60 * 60 * 1000
                )
            )

            val request = OneTimeWorkRequestBuilder<PhotoCleanupWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(request)

            val workInfo = withTimeout(60_000) {
                WorkManager.getInstance(context)
                    .getWorkInfoByIdFlow(request.id)
                    .filterNotNull()
                    .first { it.state.isFinished }
            }

            assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
            assertFalse(oldTemporaryFile.exists())
        } finally {
            oldTemporaryFile.delete()
        }
    }
}