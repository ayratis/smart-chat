package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class MessageReadRequest(
    @SerializedName("message_ids")
    val messageIds: List<Long>,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("sender_id")
    val senderId: String
)
