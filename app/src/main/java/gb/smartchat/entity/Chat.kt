package gb.smartchat.entity


import android.util.Log
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
    @SerializedName("last_message")
    val lastMessage: Message?,
    @SerializedName("unread_messages_count")
    val unreadMessagesCount: Int?,
    @SerializedName("is_favorites")
    val isFavorites: Boolean?,
    @SerializedName("is_pinned")
    val isPinned: Boolean?,
    @SerializedName("recipients")
    val users: List<User>
) : Serializable {

    fun getReadInfo(userId: String): ReadInfo {
        val inRead = users.find { it.id == userId }?.lastReadMessageId ?: -1
        val outRead = users.filter { it.id != userId }.maxOf { it.lastReadMessageId ?: -1 }
        val readInfo = ReadInfo(inRead, outRead, unreadMessagesCount ?: 0)
        Log.d("Chat", "getReadInfo: $readInfo")
        return readInfo
    }

    fun hasActualMention(userId: String) : Boolean {
        val user = users.find { it.id == userId } ?: return false
        return (user.lastMentionMessageId ?: 0) > (user.lastReadMessageId ?: 0)
    }
}
