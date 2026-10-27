package com.eventcounter.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ImageStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "event-images").apply { mkdirs() }

    suspend fun import(uris: List<Uri>): List<String> = withContext(NonCancellable + Dispatchers.IO) {
        uris.mapNotNull { uri -> copyOne(uri) }
    }

    fun delete(path: String) {
        runCatching {
            val file = File(path).canonicalFile
            val root = directory.canonicalFile
            if (file.toPath().startsWith(root.toPath())) {
                file.delete()
            }
        }
    }

    private fun copyOne(uri: Uri): String? {
        val destination = File(directory, UUID.randomUUID().toString())
        return try {
            val input = appContext.contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                destination.outputStream().use { output -> stream.copyTo(output) }
            }
            destination.absolutePath
        } catch (_: Exception) {
            destination.delete()
            null
        }
    }
}
