package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class MessageCreateRequest(
    @SerializedName("text")
    val text: String?,
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
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("text", text)
        put("sender_id", senderId)
        put("chat_id", chatId)
        put("client_id", clientId)
        put("quoted_message_id", quotedMessageId)
        put("file_ids", fileIds)
    }
}