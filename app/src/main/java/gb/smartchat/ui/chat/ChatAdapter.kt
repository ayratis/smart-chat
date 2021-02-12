package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.ui.chat.view_holder.IncomingViewHolder
import gb.smartchat.ui.chat.view_holder.OutgoingViewHolder
import gb.smartchat.ui.chat.view_holder.SystemViewHolder

class ChatAdapter(
    private val onItemBindListener: (ChatItem) -> Unit,
    private val onDeleteListener: (ChatItem) -> Unit,
    private val onEditListener: (ChatItem) -> Unit,
    private val onQuoteListener: (ChatItem) -> Unit
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItem.DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is ChatItem.Outgoing -> R.layout.item_chat_msg_outgoing
            is ChatItem.Incoming -> R.layout.item_chat_msg_incoming
            is ChatItem.System -> R.layout.item_chat_msg_system
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_chat_msg_outgoing ->
                OutgoingViewHolder.create(parent, onDeleteListener, onEditListener, onQuoteListener)
            R.layout.item_chat_msg_system -> SystemViewHolder.create(parent)
            R.layout.item_chat_msg_incoming -> IncomingViewHolder.create(parent, onQuoteListener)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is ChatItem.Outgoing -> (holder as OutgoingViewHolder).bind(item)
            is ChatItem.Incoming -> (holder as IncomingViewHolder).bind(item)
            is ChatItem.System -> (holder as SystemViewHolder).bind(item)
        }
        onItemBindListener.invoke(item)
    }

}