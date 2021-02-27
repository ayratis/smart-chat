package gb.smartchat.ui.chat_list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatBinding
import gb.smartchat.entity.Chat
import gb.smartchat.utils.inflate

class ChatViewHolder(
    itemView: View,
    private val clickListener: (Chat) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(
            parent: ViewGroup,
            clickListener: (Chat) -> Unit
        ) = ChatViewHolder(parent.inflate(R.layout.item_chat), clickListener)
    }

    private val binding: ItemChatBinding = ItemChatBinding.bind(itemView)
    private lateinit var chat: Chat

    init {
        itemView.setOnClickListener {
            clickListener.invoke(chat)
        }
    }

    fun bind(chat: Chat) {
        this.chat = chat
        binding.tvId.text = chat.id.toString()
    }
}
