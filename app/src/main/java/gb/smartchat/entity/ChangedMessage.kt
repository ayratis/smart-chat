package gb.smartchat.entity

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class ChangedMessage(
    @SerializedName("id")
    val id: Long,
    @SerializedName("chat_id")
    val chatId: Long?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("time_updated")
    val timeUpdated: ZonedDateTime,
    @SerializedName("mentions")
    val mentions: List<Mention>?,
)
