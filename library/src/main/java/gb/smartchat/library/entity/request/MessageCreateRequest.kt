package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Mention

data class MessageCreateRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("quoted_message_id")
    val quotedMessageId: Long?,
    @SerializedName("mentions")
    val mentions: List<Mention>?,
    @SerializedName("links")
    val links: List<String>?,
    @SerializedName("file_url")
    val fileUrl: String?,
    @SerializedName("chat_name")
    val chatName: String?,
    @SerializedName("sender_name")
    val senderName: String?
)
