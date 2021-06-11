package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class MessageDeleteRequest(
//    @SerializedName("message_ids")
//    val messageIds: List<Long>,
    @SerializedName("message_ids")
    val messageIds: List<Long>,
    @SerializedName("chat_id")
    val chatId: Long,
//    @SerializedName("user_id")
//    val userId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("for_all")
    val forAll: Boolean = true
)
