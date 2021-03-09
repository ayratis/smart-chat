package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(
    @SerializedName("last_read_message_id")
    val lastReadMessageId: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("id")
    val id: String?
): Serializable
