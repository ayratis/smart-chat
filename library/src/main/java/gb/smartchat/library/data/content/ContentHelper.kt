package gb.smartchat.library.data.content

import android.net.Uri
import okhttp3.RequestBody
import java.io.InputStream

interface ContentHelper {
    fun nameSize(contentUri: Uri): Pair<String, Long>?
    fun requestBody(contentUri: Uri): RequestBody
    fun inputStream(contentUri: Uri): InputStream?
    fun mimeType(contentUri: Uri): String?
}
