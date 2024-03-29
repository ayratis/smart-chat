package gb.smartchat.library.entity.request

import com.google.gson.annotations.SerializedName

data class MessageFavoriteRequest(
    @SerializedName("message_ids")
    val messageIds: List<Long>
)
