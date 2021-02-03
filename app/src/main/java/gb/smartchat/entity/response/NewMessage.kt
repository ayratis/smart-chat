package gb.smartchat.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Message

data class NewMessage(
    @SerializedName("new_message")
    val message: Message
)