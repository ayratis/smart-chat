package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName

data class FileUploadResponse(
    @SerializedName("file_id") val fileId: Long
)
