package gb.smartchat.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMsgSystemBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate

class SystemViewHolder private constructor(
    itemView: View,
//    private val onMessageClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "DummyViewHolder"

        fun create(parent: ViewGroup, /*onMessageClickListener: (ChatItem) -> Unit*/) =
            SystemViewHolder(parent.inflate(R.layout.item_chat_msg_system), /*onMessageClickListener*/)
    }

    private val binding = ItemChatMsgSystemBinding.bind(itemView)
    private lateinit var chatItem: ChatItem

    init {
//        binding.tvContent.setOnClickListener {
//            onMessageClickListener.invoke(chatItem)
//        }
    }

    fun bind(chatItem: ChatItem.System) {
        this.chatItem = chatItem
        binding.tvContent.text = chatItem.message.text
    }
}
