package gb.smartchat.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgIncomingBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate

class IncomingViewHolder private constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "DummyViewHolder"

        fun create(parent: ViewGroup, ) =
            IncomingViewHolder(parent.inflate(R.layout.item_chat_msg_incoming))
    }

    private val binding by viewBinding(ItemChatMsgIncomingBinding::bind)
    private lateinit var chatItem: ChatItem

    fun bind(chatItem: ChatItem.Incoming) {
        this.chatItem = chatItem
        binding.tvContent.text = chatItem.message.text
    }
}