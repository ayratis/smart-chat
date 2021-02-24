package gb.smartchat.ui.chat.view_holder

import android.util.Log
import android.view.*
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgOutgoingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate
import gb.smartchat.utils.visible
import java.text.SimpleDateFormat
import java.util.*

class OutgoingViewHolder private constructor(
    itemView: View,
    private val onDeleteListener: (ChatItem) -> Unit,
    private val onEditListener: (ChatItem) -> Unit,
    private val onQuoteListener: (ChatItem) -> Unit,
    private val onQuotedMsgClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    companion object {
        private const val TAG = "DummyViewHolder"

        fun create(
            parent: ViewGroup,
            onDeleteListener: (ChatItem) -> Unit,
            onEditListener: (ChatItem) -> Unit,
            onQuoteListener: (ChatItem) -> Unit,
            onQuotedMsgClickListener: (ChatItem) -> Unit
        ) =
            OutgoingViewHolder(
                parent.inflate(R.layout.item_chat_msg_outgoing),
                onDeleteListener,
                onEditListener,
                onQuoteListener,
                onQuotedMsgClickListener
            )
    }

    private val binding by viewBinding(ItemChatMsgOutgoingBinding::bind)
    private lateinit var chatItem: ChatItem
    private val sdf = SimpleDateFormat("h:mm", Locale.getDefault())

    init {
        binding.root.setOnCreateContextMenuListener(this)
        binding.viewQuotedMessage.setOnClickListener {
            onQuotedMsgClickListener.invoke(chatItem)
        }
    }

    fun bind(chatItem: ChatItem.Outgoing) {
        this.chatItem = chatItem
//        binding.tvContent.text = chatItem.message.id.toString() //debug
//        return
//        val statusString = when (chatItem.status) {
//            ChatItem.OutgoingStatus.SENDING -> "`"
//            ChatItem.OutgoingStatus.SENT -> "+"
//            ChatItem.OutgoingStatus.SENT_2 -> "++"
//            ChatItem.OutgoingStatus.READ -> "+++"
//            ChatItem.OutgoingStatus.FAILURE -> "error"
//            ChatItem.OutgoingStatus.EDITING -> "editing"
//            ChatItem.OutgoingStatus.DELETING -> "deleting"
//            ChatItem.OutgoingStatus.DELETED -> "deleted"
//        }

        binding.viewQuotedMessage.visible(chatItem.message.quotedMessage != null)
        binding.tvQuotedMessage.text = chatItem.message.quotedMessage?.text
        binding.ivStatus.setImageResource(
            when (chatItem.status) {
                ChatItem.OutgoingStatus.SENT_2 -> R.drawable.ic_double_check_12
                ChatItem.OutgoingStatus.READ -> R.drawable.ic_double_check_colored_12
                else -> R.drawable.ic_one_check_12
            }
        )

        binding.tvContent.text = chatItem.message.text
        binding.tvTime.text = chatItem.message.timeCreated?.let { sdf.format(it) }
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