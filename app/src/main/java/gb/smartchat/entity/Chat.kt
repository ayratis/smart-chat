package gb.smartchat.entity


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Chat(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String?,
    @SerializedName("store_name")
    val storeName: String?,
    @SerializedName("agent_name")
    val agentName: String?,
    @SerializedName("last_message_text")
    val lastMessageText: String?,
    @SerializedName("last_message_date")
    val lastMessageDate: Int?,
    @SerializedName("last_message_sender_id")
    val lastMessageSenderId: String?,
    @SerializedName("unread_messages_count")
    val unreadMessagesCount: Int?,
    @SerializedName("is_favorites")
    val isFavorites: Boolean?,
    @SerializedName("is_pinned")
    val isPinned: Boolean?,
    @SerializedName("users")
    val users: List<User>?
) : Serializable