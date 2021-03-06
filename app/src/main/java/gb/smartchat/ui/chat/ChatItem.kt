package gb.smartchat.ui.chat

import androidx.recyclerview.widget.DiffUtil
import gb.smartchat.entity.Message
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
    }

    enum class OutgoingStatus {
        SENDING,
        SENT,
        SENT_2,
        READ,
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
                return oldItem.message.clientId == newItem.message.clientId
            }
            if (oldItem is Msg.System && newItem is Msg.System) {
                return oldItem.message.id == newItem.message.id
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
    }
}
