package gb.smartchat.ui.chat.view_holder

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgIncomingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate

class IncomingViewHolder private constructor(
    itemView: View,
    private val onQuoteListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "IncomingViewHolder"

        fun create(parent: ViewGroup, onQuoteListener: (ChatItem) -> Unit) =
            IncomingViewHolder(parent.inflate(R.layout.item_chat_msg_incoming), onQuoteListener)
    }

    private val binding by viewBinding(ItemChatMsgIncomingBinding::bind)
    private lateinit var chatItem: ChatItem

    init {
        binding.root.setOnCreateContextMenuListener(this)
    }

    fun bind(chatItem: ChatItem.Incoming) {
        this.chatItem = chatItem
        val quotingMessage = chatItem.message.quotedMessageId?.toString()
        if (quotingMessage != null) {
            val text = "$quotingMessage\n\n${chatItem.message.text}"
            val spannable = SpannableStringBuilder(text).apply {
                setSpan(
                    StyleSpan(Typeface.ITALIC),
                    0,
                    quotingMessage.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    quotingMessage.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            binding.tvContent.text = spannable
        } else {
            val text = chatItem.message.text
            binding.tvContent.text = text
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val inflater = MenuInflater(itemView.context)
        inflater.inflate(R.menu.quote, menu)
        menu?.forEach {
            it.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
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