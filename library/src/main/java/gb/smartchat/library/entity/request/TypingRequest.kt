package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class TypingRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Long,
)
