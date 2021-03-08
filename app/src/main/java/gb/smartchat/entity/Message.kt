package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import gb.smartchat.BuildConfig
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
    @SerializedName("text")
    val text: String?,
    @SerializedName("type")
    val type: Type?,
    @SerializedName("quoted_message")
    val quotedMessage: QuotedMessage?,
    @SerializedName("time_created")
    val timeCreated: ZonedDateTime,
    @SerializedName("time_updated")
    val timeUpdated: ZonedDateTime?,
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
