package gb.smartchat.entity


import com.google.gson.annotations.SerializedName

data class QuotedMessage(
    @SerializedName("id")
    val messageId: Long,
    @SerializedName("text")
    val text: String?,
    @SerializedName("sender_id")
    val senderId: String?
)
