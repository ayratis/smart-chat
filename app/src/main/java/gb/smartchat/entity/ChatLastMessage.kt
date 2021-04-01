package gb.smartchat.entity

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class ChatLastMessage(
    @SerializedName("id")
    val id: Long,
    @SerializedName("text")
    val text: String?,
    @SerializedName("time_created")
    val timeCreated: ZonedDateTime,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("mentions")
    val mentions: List<Mention>?,
    @SerializedName("file")
    val file: Boolean?,
) {
    fun isOutgoing(userId: String): Boolean {
        return senderId == userId
    }
}