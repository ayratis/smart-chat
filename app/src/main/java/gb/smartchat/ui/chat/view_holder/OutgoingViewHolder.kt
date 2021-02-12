package gb.smartchat.ui.chat.view_holder

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgOutgoingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate

class OutgoingViewHolder private constructor(
    itemView: View,
    private val onDeleteListener: (ChatItem) -> Unit,
    private val onEditListener: (ChatItem) -> Unit,
    private val onQuoteListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "DummyViewHolder"

        fun create(
            parent: ViewGroup,
            onDeleteListener: (ChatItem) -> Unit,
            onEditListener: (ChatItem) -> Unit,
            onQuoteListener: (ChatItem) -> Unit
        ) =
            OutgoingViewHolder(
                parent.inflate(R.layout.item_chat_msg_outgoing),
                onDeleteListener,
                onEditListener,
                onQuoteListener
            )
    }

    private val binding by viewBinding(ItemChatMsgOutgoingBinding::bind)
    private lateinit var chatItem: ChatItem

    init {
        binding.root.setOnCreateContextMenuListener(this)
    }

    fun bind(chatItem: ChatItem.Outgoing) {
        this.chatItem = chatItem
        val statusString = when (chatItem.status) {
            ChatItem.OutgoingStatus.SENDING -> "`"
            ChatItem.OutgoingStatus.SENT -> "+"
            ChatItem.OutgoingStatus.SENT_2 -> "++"
            ChatItem.OutgoingStatus.READ -> "+++"
            ChatItem.OutgoingStatus.FAILURE -> "error"
            ChatItem.OutgoingStatus.EDITING -> "editing"
            ChatItem.OutgoingStatus.DELETING -> "deleting"
            ChatItem.OutgoingStatus.DELETED -> "deleted"
        }
        val quotingMessage = chatItem.message.quotedMessageId?.toString()
        if (quotingMessage != null) {
            val text = "$quotingMessage\n\n${chatItem.message.text} ($statusString)"
            val spannable = SpannableStringBuilder(text).apply {
                setSpan(
                    StyleSpan(android.graphics.Typeface.ITALIC),
                    0,
                    quotingMessage.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    0,
                    quotingMessage.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            binding.tvContent.text = spannable
        } else {
            val text = "${chatItem.message.text} ($statusString)"
            binding.tvContent.text = text
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val inflater = MenuInflater(itemView.context)
        inflater.inflate(R.menu.outgoing_message, menu)
        inflater.inflate(R.menu.quote, menu)
        menu?.forEach {
            it.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        Log.d(TAG, "onCreateContextMenu: delete")
                        onDeleteListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_edit -> {
                        Log.d(TAG, "onCreateContextMenu: edit")
                        onEditListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_quote -> {
                        Log.d(TAG, "onCreateContextMenu: quote")
                        onQuoteListener.invoke(chatItem)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
    }
}