package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.request.MessageCreateRequest

data class Message(
    @SerializedName("id")
    val id: Long,
    @SerializedName("chat_id")
    val chatId: Int?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("client_id")
    val clientId: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("type")
    val type: Type?,
    @SerializedName("readed_ids")
    val readedIds: List<String>?
//    @SerializedName("quoted_message_id")
//    val quotedMessageId: Long,
//    @SerializedName("mentions")
//    val mentions: List<Any>?,
//    @SerializedName("file_ids")
//    val fileIds: List<String>?,
) {
    enum class Type {
        @SerializedName("system")
        SYSTEM,

        @SerializedName("user")
        USER
    }

    fun toMessageCreateRequestBody(): MessageCreateRequest? {
        return if (text != null && senderId != null && chatId != null && clientId != null) {
            MessageCreateRequest(
                text = text,
                senderId = senderId,
                chatId = chatId,
                clientId = clientId,
            )
        } else {
            null
        }
    }
}