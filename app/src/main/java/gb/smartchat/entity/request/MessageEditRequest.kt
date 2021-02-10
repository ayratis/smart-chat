package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName

data class MessageEditRequest (
    @SerializedName("text")
    val text: String,
    @SerializedName("sender_id")
    val messageId: Long,
    @SerializedName("chat_id")
    val chatId: Long,
//    @SerializedName("mentions")
//    val mentions: List<Mention> = emptyList(),
)