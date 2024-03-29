package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Contact

data class AddRecipientsResponse(
    @SerializedName("contacts")
    val contacts: List<Contact>,
    @SerializedName("chat_id")
    val chatId: Long
)
