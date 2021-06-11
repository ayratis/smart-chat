package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName
import gb.smartchat.library.entity.Contact

data class AddRecipientsRequest(
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("contacts")
    val contacts: List<Contact>
)
