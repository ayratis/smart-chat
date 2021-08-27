package gb.smartchat.library.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QuotedMessage(
    @SerializedName("id")
    val messageId: Long,
    @SerializedName("text")
    val text: String?,
    @SerializedName("sender_id")
    val senderId: String?,
    @SerializedName("file")
    val file: File?,

    //custom
    val user: User?,
) : Serializable
