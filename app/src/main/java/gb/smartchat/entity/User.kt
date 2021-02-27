package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("last_read_message_id")
    val lastReadMessageId: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("id")
    val id: String?
)