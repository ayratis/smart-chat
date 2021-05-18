package gb.smartchat.ui.chat_profile.members

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Contact
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder

class ChatProfileMembersAdapter(
    private val onContactClickListener: (Contact) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit,
    private val deleteContactListener: ((Contact) -> Unit)?
) : ListAdapter<ChatProfileMembersItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatProfileMembersItem.Data -> 1
            is ChatProfileMembersItem.Error -> 2
            is ChatProfileMembersItem.Progress -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChatProfileMemberViewHolder.create(
                parent,
                onContactClickListener,
                deleteContactListener
            )
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatProfileMembersItem.Data -> (holder as ChatProfileMemberViewHolder).bind(item.contact)
            is ChatProfileMembersItem.Progress -> {
            }
            is ChatProfileMembersItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )

        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ChatProfileMembersItem>() {
        override fun areItemsTheSame(
            oldItem: ChatProfileMembersItem,
            newItem: ChatProfileMembersItem
        ): Boolean {
            if (oldItem is ChatProfileMembersItem.Data && newItem is ChatProfileMembersItem.Data) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is ChatProfileMembersItem.Error && newItem is ChatProfileMembersItem.Error) {
                return true
            }
            if (oldItem is ChatProfileMembersItem.Progress && newItem is ChatProfileMembersItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: ChatProfileMembersItem,
            newItem: ChatProfileMembersItem
        ): Boolean {
            if (oldItem is ChatProfileMembersItem.Data && newItem is ChatProfileMembersItem.Data) {
                return oldItem.contact == newItem.contact
            }
            if (oldItem is ChatProfileMembersItem.Error && newItem is ChatProfileMembersItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is ChatProfileMembersItem.Progress && newItem is ChatProfileMembersItem.Progress) {
                return true
            }
            return false
        }

    }
}
