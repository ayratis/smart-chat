package gb.smartchat.library

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.bumptech.glide.load.ImageHeaderParser
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser
import java.io.FileInputStream
import java.lang.Exception

class SmartChatImageFileProvider : FileProvider() {

    private val imageHeaderParser = DefaultImageHeaderParser()

    override fun getType(uri: Uri): String? {
        var type = super.getType(uri)
        if (type != "application/octet-stream") return type
        try {
            openFile(uri, "r")?.use { parcelFileDescriptor ->
                FileInputStream(parcelFileDescriptor.fileDescriptor).use { fileInputStream ->
                    val imageType = imageHeaderParser.getType(fileInputStream)
                    type = getTypeFromImageType(imageType, type)
                }
            }
        } catch (e: Exception) {
            Log.d("ImageFileProvider", "getType", e)
        }
        return type
    }

    private fun getTypeFromImageType(
        imageType: ImageHeaderParser.ImageType,
        defaultType: String?
    ): String? {
        val extension = when(imageType) {
            ImageHeaderParser.ImageType.GIF -> "gif"
            ImageHeaderParser.ImageType.JPEG -> "jpg"
            ImageHeaderParser.ImageType.PNG_A,
            ImageHeaderParser.ImageType.PNG -> "png"
            ImageHeaderParser.ImageType.WEBP_A,
            ImageHeaderParser.ImageType.WEBP -> "webp"
            ImageHeaderParser.ImageType.RAW,
            ImageHeaderParser.ImageType.UNKNOWN -> defaultType
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
