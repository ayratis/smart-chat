package gb.smartchat.ui.chat

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Mention
import gb.smartchat.ui._global.StickyHeaderHelper
import gb.smartchat.ui.chat.view_holder.*
import java.time.ZoneId

class ChatAdapter(
    private val onItemBindListener: (ChatItem) -> Unit,
    private val onDeleteListener: ((ChatItem.Msg) -> Unit)?,
    private val onEditListener: ((ChatItem.Msg) -> Unit)?,
    private val onQuoteListener: ((ChatItem.Msg) -> Unit)?,
    private val nextPageUpCallback: () -> Unit,
    private val nextPageDownCallback: () -> Unit,
    private val onQuotedMsgClickListener: ((ChatItem.Msg) -> Unit)?,
    private val onFileClickListener: (ChatItem.Msg) -> Unit,
    private val onMentionClickListener: ((Mention) -> Unit)?,
    private val onToFavoritesClickListener: ((ChatItem.Msg) -> Unit)?,
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItem.DiffUtilItemCallback()),
    StickyHeaderHelper {

    var fullDataUp = false
    var fullDataDown = false

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatItem.DateHeader -> 1
            is ChatItem.Msg.Incoming -> 2
            is ChatItem.Msg.System -> 3
            is ChatItem.Msg.Outgoing -> 4
            is ChatItem.Msg.Draft -> 5
            is ChatItem.Typing -> 6
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> DateHeaderViewHolder.create(parent)
            2 -> IncomingViewHolder.create(
                parent,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener,
                onMentionClickListener,
                onToFavoritesClickListener
            )
            3 -> SystemViewHolder.create(parent)
            4 -> OutgoingViewHolder.create(
                parent,
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener,
                onMentionClickListener,
                onToFavoritesClickListener
            )
            5 -> DraftViewHolder.create(
                parent,
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener,
                onFileClickListener
            )
            6 -> TypingViewHolder.create(parent)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d("ChatFragment", "onBind: pos: $position")
        val item = getItem(position)
        when (item) {
            is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is ChatItem.Msg.Outgoing -> (holder as OutgoingViewHolder).bind(item)
            is ChatItem.Msg.Incoming -> (holder as IncomingViewHolder).bind(item)
            is ChatItem.Msg.System -> (holder as SystemViewHolder).bind(item)
            is ChatItem.Msg.Draft -> (holder as DraftViewHolder).bind(item)
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

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is ChatItem.DateHeader ->
                item.localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            is ChatItem.Msg.Incoming -> item.message.id
            is ChatItem.Msg.Outgoing -> item.message.id
            is ChatItem.Msg.System -> item.message.id
            is ChatItem.Typing -> -1L
            is ChatItem.Msg.Draft -> item.message.timeCreated.toInstant().toEpochMilli()
        }
    }
}
