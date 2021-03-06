package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName

data class ReadInfoRequest(
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("chat_id")
    val chatId: Long
)
