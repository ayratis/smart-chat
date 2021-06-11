package gb.smartchat.library.entity.response

import com.google.gson.annotations.SerializedName

data class DeleteRecipientsResponse(
    @SerializedName("deleted_user_ids")
    val deletedUserIds: List<String>,
    @SerializedName("chat_id")
    val chatId: Long
)
