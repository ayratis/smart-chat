package gb.smartchat.ui.chat.view_holder

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
import gb.smartchat.utils.visible
import java.text.SimpleDateFormat
import java.util.*

class IncomingViewHolder private constructor(
    itemView: View,
    private val onQuoteListener: (ChatItem) -> Unit,
    private val onQuotedMsgClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "IncomingViewHolder"

        fun create(
            parent: ViewGroup,
            onQuoteListener: (ChatItem) -> Unit,
            onQuotedMsgClickListener: (ChatItem) -> Unit
        ) =
            IncomingViewHolder(
                parent.inflate(R.layout.item_chat_msg_incoming),
                onQuoteListener,
                onQuotedMsgClickListener
            )
    }

    private val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
    private val binding by viewBinding(ItemChatMsgIncomingBinding::bind)
    private lateinit var chatItem: ChatItem

    init {
        binding.root.setOnCreateContextMenuListener(this)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener.invoke(chatItem)
        }
    }

    fun bind(chatItem: ChatItem.Incoming) {
        this.chatItem = chatItem
        //        binding.tvContent.text = chatItem.message.id.toString() //debug
        //        return
        binding.viewQuotedMessage.visible(chatItem.message.quotedMessage != null)
        binding.tvQuotedMessage.text = chatItem.message.quotedMessage?.text
        binding.tvContent.text = chatItem.message.text
        binding.tvTime.text = chatItem.message.timeCreated?.let { sdf.format(it) }
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