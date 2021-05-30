package gb.smartchat.ui.chat_list_search

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Chat
import gb.smartchat.entity.Contact
import gb.smartchat.ui._global.view_holder.ChatViewHolder
import gb.smartchat.ui._global.view_holder.ContactViewHolder
import gb.smartchat.ui._global.view_holder.HeaderViewHolder

class SearchResultsAdapter(
    private val userId: String,
    private val onChatClickListener: (Chat) -> Unit,
    private val onContactClickListener: (Contact) -> Unit,
    private val nextPageCallback: () -> Unit
) : ListAdapter<SearchItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    var fullData: Boolean = false

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is SearchItem.Header -> 1
            is SearchItem.Chat -> 2
            is SearchItem.Contact -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            1 -> HeaderViewHolder.create(parent)
            2 -> ChatViewHolder.create(
                parent,
                userId,
                false,
                onChatClickListener,
                null,
                null
            )
            3 -> ContactViewHolder.create(parent, onContactClickListener)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = getItem(position)) {
            is SearchItem.Chat -> (holder as ChatViewHolder).bind(item.chat)
            is SearchItem.Contact -> (holder as ContactViewHolder).bind(item.contact)
            is SearchItem.Header -> (holder as HeaderViewHolder).bind(item.name)
        }
        if (!fullData && position >= itemCount - 5) nextPageCallback.invoke()
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<SearchItem>() {
        override fun areItemsTheSame(oldItem: SearchItem, newItem: SearchItem): Boolean {
            if (oldItem is SearchItem.Header && newItem is SearchItem.Header) {
                return oldItem.name == newItem.name
            }
            if (oldItem is SearchItem.Chat && newItem is SearchItem.Chat) {
                return oldItem.chat.id == newItem.chat.id
            }
            if (oldItem is SearchItem.Contact && newItem is SearchItem.Contact) {
                return oldItem.contact.id == newItem.contact.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: SearchItem, newItem: SearchItem): Boolean {
            if (oldItem is SearchItem.Header && newItem is SearchItem.Header) {
                return oldItem.name == newItem.name
            }
            if (oldItem is SearchItem.Chat && newItem is SearchItem.Chat) {
                return oldItem.chat == newItem.chat
            }
            if (oldItem is SearchItem.Contact && newItem is SearchItem.Contact) {
                return oldItem.contact.name == newItem.contact.name &&
                        oldItem.contact.avatar == newItem.contact.avatar
            }
            return false
        }

    }
}
