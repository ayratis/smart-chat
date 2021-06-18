package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Group

data class ChatSearchResponse(
    @SerializedName("chats")
    val chats: List<Chat>,
    @SerializedName("contacts")
    val contacts: List<Group>
)
