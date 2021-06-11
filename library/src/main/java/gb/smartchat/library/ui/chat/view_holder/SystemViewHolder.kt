package gb.smartchat.library.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgSystemBinding
import gb.smartchat.library.ui.chat.ChatItem
import gb.smartchat.library.utils.inflate

class SystemViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "SystemViewHolder"

        fun create(parent: ViewGroup) =
            SystemViewHolder(parent.inflate(R.layout.item_chat_msg_system))
    }

    private val binding = ItemChatMsgSystemBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.Msg

    fun bind(chatItem: ChatItem.Msg.System) {
        this.chatItem = chatItem
        binding.root.text = chatItem.message.text
    }
}
