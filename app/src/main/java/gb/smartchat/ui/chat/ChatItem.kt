package gb.smartchat.ui.chat

import androidx.recyclerview.widget.DiffUtil
import gb.smartchat.entity.Message

sealed class ChatItem {

    data class Incoming(val message: Message) : ChatItem()
    data class Outgoing(val message: Message, val status: OutgoingStatus) : ChatItem()
    data class System(val message: Message) : ChatItem()

    enum class OutgoingStatus {
        SENDING,
        SENT,
        SENT_2,
        READ,
        FAILURE
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is Incoming && newItem is Incoming) {
                return oldItem.message.id == newItem.message.id
            }
            if (oldItem is Outgoing && newItem is Outgoing) {
                return oldItem.message.clientId == newItem.message.clientId &&
                        oldItem.message.text == newItem.message.text //todo нет гарантий
            }
            if (oldItem is System && newItem is System) {
                return oldItem.message.id == newItem.message.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is Incoming && newItem is Incoming) {
                return oldItem.message == newItem.message
            }
            if (oldItem is Outgoing && newItem is Outgoing) {
                return oldItem.message == newItem.message
            }
            if (oldItem is System && newItem is System) {
                return oldItem.message == newItem.message
            }
            return false
        }
    }
}