package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class PinChatRequest(
    @SerializedName("chat_id")
    val chatId: Long
)
