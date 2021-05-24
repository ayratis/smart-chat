package gb.smartchat.entity


import android.webkit.MimeTypeMap
import com.google.gson.annotations.SerializedName
import gb.smartchat.data.download.DownloadStatus
import java.io.Serializable

data class File(
    @SerializedName("url")
    val url: String?,
    @SerializedName("size")
    val size: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("type")
    val type: String?,

    val downloadStatus: DownloadStatus = DownloadStatus.Empty
//    @SerializedName("preview")
//    val preview: Any?
) : Serializable {
    fun isImage(): Boolean {
        val mimeType = url?.let {
            val extension = MimeTypeMap.getFileExtensionFromUrl(it)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType?.startsWith("image") == true
    }
}

