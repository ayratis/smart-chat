package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.ui.global.view_holder.DummyViewHolder

class ChatAdapter(
    private val onItemBindListener: (ChatItem) -> Unit
) : ListAdapter<ChatItem, DummyViewHolder>(ChatItem.DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DummyViewHolder {
        return DummyViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: DummyViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatItem.Outgoing -> {
                val statusText = when(item.status) {
                    ChatItem.OutgoingStatus.SENDING -> "`"
                    ChatItem.OutgoingStatus.SENT -> "+"
                    ChatItem.OutgoingStatus.SENT_2 -> "++"
                    ChatItem.OutgoingStatus.READ -> "r"
                    ChatItem.OutgoingStatus.FAILURE -> "e"
                    ChatItem.OutgoingStatus.EDITING -> "editing"
                    ChatItem.OutgoingStatus.DELETING -> "deleting"
                }
                holder.bind("${item.message.text} ($statusText)")
            }
            is ChatItem.Incoming -> holder.bind(item.message.text ?: "`null`")
            is ChatItem.System -> holder.bind(item.message.text ?: "`null`")
        }
    }

}