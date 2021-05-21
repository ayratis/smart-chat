package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Chat

data class FavoriteChatResponse(
    @SerializedName("chat")
    val chat: Chat
)
