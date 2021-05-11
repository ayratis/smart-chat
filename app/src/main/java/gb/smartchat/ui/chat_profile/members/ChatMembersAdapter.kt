package gb.smartchat.ui.chat_profile.members

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Contact
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder

class ChatMembersAdapter(
    private val onContactClickListener: (Contact) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit
) : ListAdapter<ChatMembersItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatMembersItem.Data -> 1
            is ChatMembersItem.Error -> 2
            is ChatMembersItem.Progress -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChatMemberViewHolder.create(parent, onContactClickListener)
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatMembersItem.Data -> (holder as ChatMemberViewHolder).bind(item.contact)
            is ChatMembersItem.Progress -> {
            }
            is ChatMembersItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )

        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatMembersItem>() {
        override fun areItemsTheSame(
            oldItem: ChatMembersItem,
            newItem: ChatMembersItem
        ): Boolean {
            if (oldItem is ChatMembersItem.Data && newItem is ChatMembersItem.Data) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is ChatMembersItem.Error && newItem is ChatMembersItem.Error) {
                return true
            }
            if (oldItem is ChatMembersItem.Progress && newItem is ChatMembersItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: ChatMembersItem,
            newItem: ChatMembersItem
        ): Boolean {
            if (oldItem is ChatMembersItem.Data && newItem is ChatMembersItem.Data) {
                return oldItem.contact == newItem.contact
            }
            if (oldItem is ChatMembersItem.Error && newItem is ChatMembersItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is ChatMembersItem.Progress && newItem is ChatMembersItem.Progress) {
                return true
            }
            return false
        }

    }
}
