package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.annotation.Keep
import androidx.documentfile.provider.DocumentFile
import ch.rmy.android.framework.extensions.takeUnlessEmpty
import ch.rmy.android.http_shortcuts.data.domains.working_directories.WorkingDirectoryRepository
import ch.rmy.android.http_shortcuts.exceptions.ActionException
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import dagger.hilt.android.qualifiers.ApplicationContext
import org.liquidplayer.javascript.JSObject
import org.liquidplayer.javascript.JSValue
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import javax.inject.Inject

class GetDirectoryAction
@Inject
constructor(
    @ApplicationContext
    private val context: Context,
    private val workingDirectoryRepository: WorkingDirectoryRepository,
) : Action<GetDirectoryAction.Params> {
    override suspend fun Params.execute(executionContext: ExecutionContext): JSObject {
        val workingDirectory = try {
            workingDirectoryRepository.getWorkingDirectoryByNameOrId(directoryNameOrId)
        } catch (e: NoSuchElementException) {
            throw ActionException {
                "Directory \"${directoryNameOrId}\" not found"
            }
        }
        workingDirectoryRepository.touchWorkingDirectory(workingDirectory.id)
        val directory = DocumentFile.fromTreeUri(context, workingDirectory.directoryUri)!!
        if (!directory.isDirectory) {
            throw ActionException {
                "Directory \"${workingDirectory.name}\" is not mounted"
            }
        }

        val contentResolver = context.contentResolver

        return object : JSObject(executionContext.jsContext, DirectoryHandle::class.java), DirectoryHandle {
            override fun readFile(filePath: String, encoding: String?): String {
                val file = directory.findFileFromPath(filePath)
                    ?: executionContext.throwException(
                        ActionException {
                            "File \"$filePath\" not found in directory \"${workingDirectory.name}\""
                        }
                    )
                val charset = encoding?.let {
                    try {
                        Charset.forName(it)
                    } catch (e: IllegalCharsetNameException) {
                        executionContext.throwException(
                            ActionException {
                                "Invalid charset: $it"
                            }
                        )
                    } catch (e: UnsupportedCharsetException) {
                        executionContext.throwException(
                            ActionException {
                                "Unsupported charset: $it"
                            }
                        )
                    }
                } ?: Charsets.UTF_8
                return contentResolver.openInputStream(file.uri)!!
                    .use {
                        it.reader(charset).readText()
                    }
            }

            override fun writeFile(filePath: String, content: JSValue?) {
                content ?: return
                val file = directory.findOrCreateFileFromPath(filePath)
                    ?: executionContext.throwException(
                        ActionException {
                            "File \"$filePath\" not found in directory \"${workingDirectory.name}\""
                        }
                    )
                contentResolver.openOutputStream(file.uri, "wt")!!
                    .use { out ->
                        out.write(content.toByteArray())
                    }
            }

            private fun JSValue.toByteArray(): ByteArray =
                when {
                    isUint8Array || isInt8Array || isArray -> toJSArray().map { it.toString().toByte() }.toByteArray()
                    else -> toString().toByteArray()
                }

            override fun appendFile(filePath: String, content: JSValue?) {
                content ?: return
                val file = directory.findOrCreateFileFromPath(filePath)
                    ?: executionContext.throwException(
                        ActionException {
                            "File \"$filePath\" not found in directory \"${workingDirectory.name}\""
                        }
                    )
                contentResolver.openOutputStream(file.uri, "wa")!!
                    .use { out ->
                        out.write(content.toByteArray())
                    }
            }
        }
    }

    private fun DocumentFile.findFileFromPath(filePath: String): DocumentFile? {
        var fileHandle: DocumentFile = this
        filePath.split('/').forEach { fileName ->
            fileHandle = fileHandle.findFile(fileName)
                ?: return null
        }
        return fileHandle
    }

    private fun DocumentFile.findOrCreateFileFromPath(filePath: String): DocumentFile? {
        var fileHandle: DocumentFile = this
        val parts = filePath.split('/')
        parts.forEachIndexed { index, fileName ->
            if (fileName == "." || fileName == "..") {
                return null
            }
            fileHandle = fileHandle.findFile(fileName)
                ?: (
                    if (index != parts.lastIndex) {
                        fileHandle.createDirectory(fileName)
                    } else {
                        fileHandle.createFile(
                            determineMimeType(fileName),
                            fileName
                        )
                    }
                    )
                ?: return null
        }
        return fileHandle
    }

    private fun determineMimeType(fileName: String): String =
        File(fileName).extension.takeUnlessEmpty()?.let { extension ->
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
            ?: "text/plain"

    interface DirectoryHandle {
        @Keep
        fun readFile(filePath: String, encoding: String?): String

        @Keep
        fun writeFile(filePath: String, content: JSValue?)

        @Keep
        fun appendFile(filePath: String, content: JSValue?)
    }

    data class Params(
        val directoryNameOrId: String,
    )
}
