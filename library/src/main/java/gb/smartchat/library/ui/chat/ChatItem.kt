package gb.smartchat.library.ui.chat

import androidx.recyclerview.widget.DiffUtil
import gb.smartchat.library.entity.Message
import gb.smartchat.library.entity.User
import gb.smartchat.library.utils.SingleEvent
import java.time.LocalDate

sealed class ChatItem {

    data class DateHeader(val localDate: LocalDate) : ChatItem()

    sealed class Msg(open val message: Message) : ChatItem() {
        data class System(override val message: Message) : Msg(message)
        data class Incoming(override val message: Message) : Msg(message)
        data class Outgoing(
            override val message: Message,
            val status: OutgoingStatus
        ) : Msg(message)

        data class Draft(override val message: Message, val status: DraftStatus) : Msg(message)
    }

    data class Typing(val typingUsers: List<User>) : ChatItem()

    enum class OutgoingStatus {
        SENT,
        RED,
    }

    enum class DraftStatus {
        SENDING,
        SENT,
        FAILURE,
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is DateHeader && newItem is DateHeader) {
                return oldItem.localDate == newItem.localDate
            }
            if (oldItem is Msg.Incoming && newItem is Msg.Incoming) {
                return oldItem.message.id == newItem.message.id
            }
            if (oldItem is Msg.Outgoing && newItem is Msg.Outgoing) {
                return oldItem.message.id == newItem.message.id
            }
            if (oldItem is Msg.System && newItem is Msg.System) {
                return oldItem.message.id == newItem.message.id
            }
            if (oldItem is Msg.Draft && newItem is Msg.Draft) {
                return oldItem.message.clientId == newItem.message.clientId
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is DateHeader && newItem is DateHeader) {
                return oldItem == newItem
            }
            if (oldItem is Msg && newItem is Msg) {
                return oldItem == newItem
            }
            return false
        }

        override fun getChangePayload(oldItem: ChatItem, newItem: ChatItem): Any {
            return Any()
        }
    }
}

data class ChatItemsInfo(
    val messages: List<Message>,
    val draft: List<Message>,
    val readOut: Long,
    val fullDataDown: Boolean,
    val typingSenderIds: List<String>,
    val withScrollTo: SingleEvent<ChatUDF.WithScrollTo>?
)

data class ScrollOptions(val position: Int, val fake: Boolean, val isUp: Boolean)

fun ChatUDF.State.mapIntoChatItemsInfo(): ChatItemsInfo {
    return ChatItemsInfo(
        messages = messages,
        draft = draft,
        readOut = readInfo.readOut,
        fullDataDown = fullDataDown,
        withScrollTo = withScrollTo,
        typingSenderIds = typingSenderIds
    )
}

fun ChatItemsInfo.mapIntoChatItems(
    userId: String,
    users: List<User>
): Pair<List<ChatItem>, ScrollOptions?> {
    if (messages.isEmpty()) return emptyList<ChatItem>() to null
    var scrollPosition = -1
    val list = mutableListOf<ChatItem>()
    var localDate: LocalDate? = null
    val withScrollTo = withScrollTo?.getContentIfNotHandled()
    messages.forEach { message ->
        val msgLocalDate = message.timeCreated.toLocalDate()
        if (msgLocalDate != null && msgLocalDate != localDate) {
            list += ChatItem.DateHeader(msgLocalDate)
            localDate = msgLocalDate
        }
        list += message.mapIntoChatItem(readOut, userId)
        if (withScrollTo != null && withScrollTo.message.id == message.id) {
            scrollPosition = list.lastIndex
        }
    }
    if (fullDataDown) {
        draft.forEach { message ->
            val msgLocalDate = message.timeCreated.toLocalDate()
            if (msgLocalDate != null && msgLocalDate != localDate) {
                list += ChatItem.DateHeader(msgLocalDate)
                localDate = msgLocalDate
            }
            list += ChatItem.Msg.Draft(message, ChatItem.DraftStatus.SENDING)
            if (withScrollTo != null && withScrollTo.message.clientId == message.clientId) {
                scrollPosition = list.lastIndex
            }
        }
    }
    if (scrollPosition == list.lastIndex) {
        scrollPosition++
    }
    val typingUsers = typingSenderIds.mapNotNull { id -> users.find { it.id == id } }
    list += ChatItem.Typing(
        if (fullDataDown) typingUsers
        else emptyList()
    )
    return if (scrollPosition != -1 && withScrollTo != null) {
        list to ScrollOptions(scrollPosition, withScrollTo.fake, withScrollTo.isUp)
    } else {
        list to null
    }
}

private fun Message.mapIntoChatItem(readOut: Long, userId: String): ChatItem.Msg {
    return when {
        senderId == userId -> {
            val status =
                if (id > readOut) ChatItem.OutgoingStatus.SENT
                else ChatItem.OutgoingStatus.RED
            ChatItem.Msg.Outgoing(this, status)
        }
        (type == Message.Type.SYSTEM) -> {
            ChatItem.Msg.System(this)
        }
        else -> {
            ChatItem.Msg.Incoming(this)
        }
    }
}
