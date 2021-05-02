package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact

data class ChatSearchResponse(
    @SerializedName("chats")
    val chats: List<Chat>,
    @SerializedName("contacts")
    val contacts: List<Contact>
)
