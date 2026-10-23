package com.sge.finca.platform.storage

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileProviderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun createShareIntent(
        file: File,
        mimeType: String,
    ): Intent {
        if (!file.isFile || !file.canRead()) {
            throw FileNotFoundException(
                "El archivo no existe o no se puede leer: ${file.name}"
            )
        }

        val uri = getUriForFile(file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}