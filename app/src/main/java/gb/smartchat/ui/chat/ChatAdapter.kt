package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.ui.chat.view_holder.*
import gb.smartchat.ui.custom.StickyHeaderHelper

class ChatAdapter(
    private val onItemBindListener: (ChatItem) -> Unit,
    private val onDeleteListener: (ChatItem.Msg) -> Unit,
    private val onEditListener: (ChatItem.Msg) -> Unit,
    private val onQuoteListener: (ChatItem.Msg) -> Unit,
    private val nextPageUpCallback: () -> Unit,
    private val nextPageDownCallback: () -> Unit,
    private val onQuotedMsgClickListener: (ChatItem.Msg) -> Unit,
    private val onFileClickListener: (ChatItem.Msg) -> Unit
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItem.DiffUtilItemCallback()),
    StickyHeaderHelper {

    var fullDataUp = false
    var fullDataDown = false

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatItem.DateHeader -> R.layout.item_chat_date_header
            is ChatItem.Msg.Outgoing -> R.layout.item_chat_msg_outgoing
            is ChatItem.Msg.Incoming -> R.layout.item_chat_msg_incoming
            is ChatItem.Msg.System -> R.layout.item_chat_msg_system
            is ChatItem.Typing -> R.layout.item_chat_typing
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_chat_date_header -> DateHeaderViewHolder.create(parent)
            R.layout.item_chat_msg_outgoing ->
                OutgoingViewHolder.create(
                    parent,
                    onDeleteListener,
                    onEditListener,
                    onQuoteListener,
                    onQuotedMsgClickListener,
                    onFileClickListener
                )
            R.layout.item_chat_msg_system -> SystemViewHolder.create(parent)
            R.layout.item_chat_msg_incoming -> IncomingViewHolder.create(
                parent,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener
            )
            R.layout.item_chat_typing -> TypingViewHolder.create(parent)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is ChatItem.Msg.Outgoing -> (holder as OutgoingViewHolder).bind(item)
            is ChatItem.Msg.Incoming -> (holder as IncomingViewHolder).bind(item)
            is ChatItem.Msg.System -> (holder as SystemViewHolder).bind(item)
            is ChatItem.Typing -> (holder as TypingViewHolder).bind(item.typingUsers)
        }
        onItemBindListener.invoke(item)
        if (!fullDataUp && position < 10) nextPageUpCallback.invoke()
        if (!fullDataDown && position >= itemCount - 10) nextPageDownCallback.invoke()
    }

    override fun isHeaderItem(position: Int): Boolean {
        if (position >= itemCount) return false
        return getItem(position) is ChatItem.DateHeader
    }
}
