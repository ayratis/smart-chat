package gb.smartchat.library.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Typing(
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("text")
    val text: String?
) : Serializable
