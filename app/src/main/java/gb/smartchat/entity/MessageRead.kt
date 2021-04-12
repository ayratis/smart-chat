package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class MessageRead(
    @SerializedName("chat_id")
    val chatId: Long?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("message_ids")
    val messageIds: List<Long>
)
