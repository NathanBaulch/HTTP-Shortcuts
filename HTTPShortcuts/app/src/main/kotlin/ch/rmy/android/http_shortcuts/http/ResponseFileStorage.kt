package ch.rmy.android.http_shortcuts.http

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import ch.rmy.android.framework.extensions.takeUnlessEmpty
import okhttp3.Response
import java.io.File
import java.io.InputStream
import java.net.SocketTimeoutException

class ResponseFileStorage(
    private val context: Context,
    private val sessionId: String,
    private val storeDirectory: String?,
) {

    fun store(response: Response, finishNormallyOnTimeout: Boolean): DocumentFile {
        val fileName = "response_$sessionId"

        val documentFile = storeDirectory
            ?.let {
                val directory = DocumentFile.fromTreeUri(context, storeDirectory.toUri())
                directory?.createFile(response.getMimeType(), fileName)
            }
            ?: run {
                val file = File(context.cacheDir, fileName)
                DocumentFile.fromFile(file)
            }

        try {
            getStream(response).use { inStream ->
                context.contentResolver.openOutputStream(documentFile.uri, "w")!!.use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        } catch (e: SocketTimeoutException) {
            if (!finishNormallyOnTimeout) {
                throw e
            }
        }
        return documentFile
    }

    private fun getStream(response: Response): InputStream =
        response.body.byteStream()

    companion object {
        internal fun Response.getMimeType(): String =
            header(HttpHeaders.CONTENT_TYPE)
                ?.let { contentType ->
                    contentType.split(';', limit = 2)[0]
                }
                ?.takeUnlessEmpty()
                ?.lowercase()
                ?.trim()
                ?: "application/octet-stream"
    }
}
