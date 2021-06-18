package gb.smartchat.library.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(
    @SerializedName("last_read_message_id")
    val lastReadMessageId: Long?,
    @SerializedName("last_mention_message_id")
    val lastMentionMessageId: Long?,
    @SerializedName("role")
    val role: Role?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("id")
    val id: String?
) : Serializable {

    enum class Role : Serializable {
        @SerializedName("USER")
        USER,
        @SerializedName("CREATOR")
        CREATOR
    }
}
