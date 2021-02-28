package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import java.util.*

data class Message(
    @SerializedName("id")
    val id: Long,
    @SerializedName("chat_id")
    val chatId: Long?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("client_id")
    val clientId: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("type")
    val type: Type?,
    @SerializedName("readed_ids")
    val readedIds: List<String>?,
    @SerializedName("quoted_message")
    val quotedMessage: QuotedMessage?,
    @SerializedName("time_created")
    val timeCreated: Date?,
    @SerializedName("file")
    val file: File?,
//    @SerializedName("mentions")
//    val mentions: List<Any>?,

//custom
    val user: User? = null
) {
    enum class Type {
        @SerializedName("system")
        SYSTEM,

        @SerializedName("DELETED")
        DELETED
    }
}
