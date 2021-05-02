package gb.smartchat.ui.chat_list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.entity.Chat
import gb.smartchat.ui._global.view_holder.ChatViewHolder

class ChatListAdapter(
    private val userId: String,
    private val clickListener: (Chat) -> Unit,
    private val pinListener: (Chat, pin: Boolean) -> Unit,
    private val nextPageCallback: () -> Unit,
) : ListAdapter<Chat, ChatViewHolder>(DiffUtilItemCallback()) {

    var fullData = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder.create(parent, userId, clickListener, pinListener)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
        if (!fullData && position >= itemCount - 5) nextPageCallback.invoke()
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Chat, newItem: Chat): Any {
            return Any()
        }
    }
}
