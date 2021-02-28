package gb.smartchat.entity

import com.google.gson.annotations.SerializedName

data class Typing(
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("text")
    val text: String?
)