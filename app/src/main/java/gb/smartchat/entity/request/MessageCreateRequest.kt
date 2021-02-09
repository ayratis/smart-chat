package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName

data class MessageCreateRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Int,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("quoted_message_id")
    val quotedMessageId: Long? = null,
//    @SerializedName("mentions")
//    val mentions: List<Mention> = emptyList(),
    @SerializedName("file_ids")
    val fileIds: List<String> = emptyList()
)