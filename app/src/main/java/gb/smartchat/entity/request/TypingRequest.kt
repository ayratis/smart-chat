package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName

data class TypingRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("chat_id")
    val chatId: Long,
)