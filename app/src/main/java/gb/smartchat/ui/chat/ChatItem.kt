package gb.smartchat.ui.chat

import androidx.recyclerview.widget.DiffUtil
import gb.smartchat.entity.Message

sealed class ChatItem(val message: Message) {

    data class Incoming(val _message: Message) : ChatItem(_message)
    data class Outgoing(val _message: Message, val status: OutgoingStatus) : ChatItem(_message)
    data class System(val _message: Message) : ChatItem(_message)

    enum class OutgoingStatus {
        SENDING,
        SENT,
        SENT_2,
        READ,
        FAILURE,
        EDITING,
        DELETING,
        DELETED
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is Incoming && newItem is Incoming) {
                return oldItem.message.id == newItem.message.id
            }
            if (oldItem is Outgoing && newItem is Outgoing) {
                return oldItem.message.clientId == newItem.message.clientId
            }
            if (oldItem is System && newItem is System) {
                return oldItem.message.id == newItem.message.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            if (oldItem is Incoming && newItem is Incoming) {
                return oldItem == newItem
            }
            if (oldItem is Outgoing && newItem is Outgoing) {
                return oldItem == newItem
            }
            if (oldItem is System && newItem is System) {
                return oldItem == newItem
            }
            return false
        }
    }
}
