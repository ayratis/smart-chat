package gb.smartchat.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.entity.Contact

data class AddRecipientsRequest(
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("contacts")
    val contacts: List<Contact>
)
