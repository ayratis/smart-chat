package gb.smartchat.utils

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.IOException


class ContentUriRequestBody(
    private val contentType: MediaType,
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : RequestBody() {

    override fun contentType(): MediaType {
        return contentType
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        try {
            val source = Okio.source(contentResolver.openInputStream(uri)!!)
            sink.writeAll(source)
        } catch (e: Throwable) {

        }
    }
}