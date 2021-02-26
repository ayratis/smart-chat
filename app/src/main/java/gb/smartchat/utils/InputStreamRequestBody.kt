package gb.smartchat.utils

import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.InputStream


class InputStreamRequestBody(
    private val contentType: MediaType?,
    private val inputStream: InputStream,
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentType
    }

    override fun contentLength(): Long {
        return -1
    }

    override fun writeTo(sink: BufferedSink) {
        try {
            val source = Okio.source(inputStream)
            sink.writeAll(source)
        } catch (e: Throwable) {
            Log.d("InputStreamRequestBody", "writeTo: error", e)
        }
    }
}
