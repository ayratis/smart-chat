package gb.smartchat.library.entity


import com.google.gson.annotations.SerializedName
import gb.smartchat.BuildConfig
import java.io.Serializable
import java.time.ZonedDateTime

data class Message(
    @SerializedName("id")
    val id: Long,
    @SerializedName("chat_id")
    val chatId: Long?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("client_id")
    val clientId: String?,
    @SerializedName("chat_name")
    val chatName: String?,
    @SerializedName("sender_name")
    val senderName: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("type_")
    val type: Type?,
    @SerializedName("quoted_message")
    val quotedMessage: QuotedMessage?,
    @SerializedName("time_created")
    val timeCreated: ZonedDateTime,
    @SerializedName("time_updated")
    val timeUpdated: ZonedDateTime?,
    @SerializedName("file")
    val file: File?,
    @SerializedName("mentions")
    val mentions: List<Mention>?,

//custom
    val user: User? = null
) : Serializable {
    enum class Type {
        @SerializedName("SYSTEM")
        SYSTEM,

        @SerializedName("DELETED")
        DELETED
    }

    override fun toString(): String {
        if (BuildConfig.DEBUG) {
            return id.toString()
        }
        return super.toString()
    }

    fun isOutgoing(userId: String): Boolean {
        return senderId == userId
    }
}
