package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class Recipient(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String?,
//    @SerializedName("avatar")
//    val avatar: Any?,
    @SerializedName("online")
    val online: Boolean?
)
