package gb.smartchat.library.data.http.interceptor

import com.google.gson.Gson
import gb.smartchat.library.data.http.ServerException
import gb.smartchat.library.entity.response.ErrorResponse
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
import okio.GzipSource
import java.io.EOFException
import java.nio.charset.Charset

class ErrorResponseInterceptor(private val gson: Gson) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) {
            throw getServerError(response)
        }
        return response
    }

    private fun getServerError(response: Response): ServerException =
        try {
            val errorResponseRaw = getErrorResponseRaw(response)
            val errorResponse = gson.fromJson(errorResponseRaw, ErrorResponse::class.java)
            ServerException(httpCode = response.code(), message = errorResponse.error)
        } catch (e: Throwable) {
            ServerException(httpCode = response.code(), message = null)
        }

    private fun getErrorResponseRaw(response: Response): String? {
        val headers = response.headers()
        val responseBody = response.body()!!
        val source: BufferedSource = responseBody.source()
        val contentLength = responseBody.contentLength()
        source.request(Long.MAX_VALUE) // Buffer the entire body.

        var buffer = source.buffer()

        if ("gzip".equals(headers.get("Content-Encoding"), ignoreCase = true)) {
            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            }
        }

        val charset = responseBody.contentType()?.charset(Charset.forName("UTF-8"))
            ?: Charset.forName("UTF-8")

        if (!isPlaintext(buffer)) {
            return null
        }

        if (contentLength != 0L) {
            return buffer.clone().readString(charset)
        }

        return null
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = if (buffer.size() < 64) buffer.size() else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(
                        codePoint
                    )
                ) {
                    return false
                }
            }
            true
        } catch (e: EOFException) {
            false // Truncated UTF-8 sequence.
        }
    }
}