package gb.smartchat.ui.group_profile.members

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Contact
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder

class GroupMembersAdapter(
    private val onContactClickListener: (Contact) -> Unit,
    private val onErrorActionClickListener: (tag: String) -> Unit
) : ListAdapter<GroupMembersItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is GroupMembersItem.Data -> 1
            is GroupMembersItem.Error -> 2
            is GroupMembersItem.Progress -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupMemberViewHolder.create(parent, onContactClickListener)
            2 -> ErrorViewHolder.create(parent, onErrorActionClickListener)
            3 -> ProgressViewHolder.create(parent)
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GroupMembersItem.Data -> (holder as GroupMemberViewHolder).bind(item.contact)
            is GroupMembersItem.Progress -> {
            }
            is GroupMembersItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )

        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<GroupMembersItem>() {
        override fun areItemsTheSame(
            oldItem: GroupMembersItem,
            newItem: GroupMembersItem
        ): Boolean {
            if (oldItem is GroupMembersItem.Data && newItem is GroupMembersItem.Data) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is GroupMembersItem.Error && newItem is GroupMembersItem.Error) {
                return true
            }
            if (oldItem is GroupMembersItem.Progress && newItem is GroupMembersItem.Progress) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: GroupMembersItem,
            newItem: GroupMembersItem
        ): Boolean {
            if (oldItem is GroupMembersItem.Data && newItem is GroupMembersItem.Data) {
                return oldItem.contact == newItem.contact
            }
            if (oldItem is GroupMembersItem.Error && newItem is GroupMembersItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is GroupMembersItem.Progress && newItem is GroupMembersItem.Progress) {
                return true
            }
            return false
        }

    }
}
