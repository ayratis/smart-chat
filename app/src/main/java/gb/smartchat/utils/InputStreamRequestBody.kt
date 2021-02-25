package gb.smartchat.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.InputStream


class InputStreamRequestBody(
    private val inputStream: InputStream,
    private val contentType: MediaType
) : RequestBody() {

    override fun contentType(): MediaType {
        return contentType
    }

    override fun contentLength(): Long {
        return if (inputStream.available() == 0) -1 else inputStream.available().toLong()
    }

    override fun writeTo(sink: BufferedSink) {
        Okio.source(inputStream).use { source ->
            sink.writeAll(source)
        }
    }
}