package gb.smartchat.library.entity


import android.webkit.MimeTypeMap
import com.google.gson.annotations.SerializedName
import gb.smartchat.library.data.download.DownloadStatus
import java.io.Serializable

data class File(
    @SerializedName("url")
    val url: String?,
    @SerializedName("size")
    val size: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("type")
    val type: TYPE?,

    val downloadStatus: DownloadStatus = DownloadStatus.Empty
//    @SerializedName("preview")
//    val preview: Any?
) : Serializable {
    fun isImage(): Boolean {
        if (url != null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return mimeType?.startsWith("image") == true
        }
        return type == TYPE.MEDIA
    }

    enum class TYPE : Serializable {
        @SerializedName("media")
        MEDIA,
        @SerializedName("regular")
        REGULAR
    }
}

