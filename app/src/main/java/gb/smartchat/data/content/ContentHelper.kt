package gb.smartchat.data.content

import android.net.Uri
import java.io.InputStream

interface ContentHelper {
    fun nameSize(contentUri: Uri): Pair<String, Long>?
    fun inputStream(contentUri: Uri): InputStream?
    fun mimeType(contentUri: Uri): String?
}
