package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

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
//    @SerializedName("quoted_message_id")
//    val quotedMessageId: Long,
//    @SerializedName("mentions")
//    val mentions: List<Any>?,
//    @SerializedName("file_ids")
//    val fileIds: List<String>?,
)