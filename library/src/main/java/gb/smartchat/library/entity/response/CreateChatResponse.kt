package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Chat

data class CreateChatResponse(
    @SerializedName("chat")
    val chat: Chat
)
