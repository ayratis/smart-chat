package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.File

data class FileUploadResponse(
    @SerializedName("file") val file: File
)
