package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Recipient(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String?,
//    @SerializedName("avatar")
//    val avatar: Any?,
    @SerializedName("online")
    val online: Boolean?
) : Serializable
