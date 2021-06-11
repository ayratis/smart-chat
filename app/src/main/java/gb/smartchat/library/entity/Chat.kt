package gb.smartchat.library.entity


import android.util.Log
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Chat(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String?,
    @SerializedName("avatar")
    val avatar: String?,
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
) : Serializable, Comparable<Chat> {

    fun getReadInfo(userId: String): ReadInfo {
        val inRead = users.find { it.id == userId }?.lastReadMessageId ?: -1
        val otherUsers = users.filter { it.id != userId }
        val outRead =
            if (otherUsers.isNotEmpty()) otherUsers.maxOf { it.lastReadMessageId ?: -1 }
            else -1
        val readInfo = ReadInfo(inRead, outRead, unreadMessagesCount ?: 0)
        Log.d("Chat", "getReadInfo: $readInfo")
        return readInfo
    }

    fun hasActualMention(userId: String): Boolean {
        val user = users.find { it.id == userId } ?: return false
        return (user.lastMentionMessageId ?: 0) > (user.lastReadMessageId ?: 0)
    }

    override fun compareTo(other: Chat): Int {
        val pinned1 = isPinned ?: false
        val pinned2 = other.isPinned ?: false
        return if (pinned1 != pinned2) {
            if (pinned1) -1 else 1
        } else {
            val first = lastMessage?.timeCreated?.toEpochSecond() ?: -1L
            val second = other.lastMessage?.timeCreated?.toEpochSecond() ?: -1L
            val diff = first - second
            val result = when {
                diff == 0L -> 0
                diff > 0L -> -1
                else -> 1
            }
            result
        }
    }
}
