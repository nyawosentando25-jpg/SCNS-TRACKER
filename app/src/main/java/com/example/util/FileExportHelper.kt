package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

data class ExportedFileResult(
    val fileName: String,
    val shareUri: Uri,
    val savedToDownloads: Boolean,
    val displayLocation: String
)

object FileExportHelper {

    fun saveAndPrepareShare(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): ExportedFileResult {
        val cacheExportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareFile = File(cacheExportsDir, fileName)
        shareFile.writeBytes(bytes)
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        var savedToDownloads = false
        var displayLocation = "App Share Cache"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SCNS_Records")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                    }
                    savedToDownloads = true
                    displayLocation = "Downloads/SCNS_Records/$fileName"
                }
            } catch (_: Exception) {
                savedToDownloads = false
            }
        }

        if (!savedToDownloads) {
            val appDownloads = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                "exports"
            ).apply { mkdirs() }
            val fallbackFile = File(appDownloads, fileName)
            fallbackFile.writeBytes(bytes)
            displayLocation = fallbackFile.name
            savedToDownloads = true
        }

        return ExportedFileResult(
            fileName = fileName,
            shareUri = shareUri,
            savedToDownloads = savedToDownloads,
            displayLocation = displayLocation
        )
    }

    fun launchShareIntent(
        context: Context,
        shareUri: Uri,
        mimeType: String,
        subject: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via WhatsApp / Apps").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openFileIntent(
        context: Context,
        shareUri: Uri,
        mimeType: String
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }
    }
}
