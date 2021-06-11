package gb.smartchat.library.ui.chat_list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.ui._global.view_holder.ChatViewHolder

class ChatListAdapter(
    private val userId: String,
    private val isArchive: Boolean,
    private val clickListener: (Chat) -> Unit,
    private val pinListener: (Chat, pin: Boolean) -> Unit,
    private val archiveListener: (Chat, archive: Boolean) -> Unit,
    private val nextPageCallback: () -> Unit,
) : ListAdapter<Chat, ChatViewHolder>(DiffUtilItemCallback()) {

    var fullData = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder.create(
            parent,
            userId,
            isArchive,
            clickListener,
            pinListener,
            archiveListener
        )
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
