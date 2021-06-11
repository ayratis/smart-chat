package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Mention

data class MessageEditRequest (
    @SerializedName("text")
    val text: String,
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("mentions")
    val mentions: List<Mention>?,
    @SerializedName("links")
    val links: List<String>?
)
