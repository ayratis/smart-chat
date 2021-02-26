package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class File(
    @SerializedName("id")
    val id: Long,
    @SerializedName("url")
    val url: String?,
    @SerializedName("size")
    val size: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("type")
    val type: String?,
//    @SerializedName("preview")
//    val preview: Any?
)
