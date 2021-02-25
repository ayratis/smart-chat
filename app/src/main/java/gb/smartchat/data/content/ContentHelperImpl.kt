package gb.smartchat.data.content

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class ContentHelperImpl(private val context: Context) : ContentHelper {

    private val contentResolver = context.contentResolver

    override fun nameSize(contentUri: Uri): Pair<String, Long>? {
        contentResolver
            .query(contentUri, null, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                    return displayName to size
                }
            }
        return null
    }

    override fun inputStream(contentUri: Uri): InputStream? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isVirtualFile(contentUri)) {
                getInputStreamForVirtualFile(contentUri)
            } else {
                contentResolver.openInputStream(contentUri)
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun mimeType(contentUri: Uri): String? {
        return contentResolver.getType(contentUri)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun isVirtualFile(uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(context, uri)) {
            return false
        }
        contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null,
            null,
            null
        )?.use { cursor ->
            val flags = if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
            return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
        }

        return false
    }

    @Throws(IOException::class)
    private fun getInputStreamForVirtualFile(uri: Uri): InputStream? {

        val openableMimeTypes: Array<String>? =
            contentResolver.getStreamTypes(uri, "*/*")

        return if (openableMimeTypes?.isNotEmpty() == true) {
            contentResolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                ?.createInputStream()
        } else {
            throw FileNotFoundException()
        }
    }
}