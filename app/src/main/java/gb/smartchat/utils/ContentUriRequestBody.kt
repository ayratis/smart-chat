package gb.smartchat.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class ContentUriRequestBody(
    private val context: Context,
    private val uri: Uri
) : RequestBody() {

    private val contentResolver = context.contentResolver

    override fun contentType(): MediaType? {
        contentResolver.getType(uri)?.let { type ->
            return MediaType.parse(type)
        }
        return null
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        try {
            val source = Okio.source(inputStream(uri)!!)
            sink.writeAll(source)
        } catch (e: Throwable) {

        }
    }

    private fun inputStream(contentUri: Uri): InputStream? {
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