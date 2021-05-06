package gb.smartchat.ui.create_chat

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Contact
import gb.smartchat.ui._global.view_holder.ErrorViewHolder
import gb.smartchat.ui._global.view_holder.ProgressViewHolder
import gb.smartchat.ui.create_chat.view_holder.ContactGroupViewHolder
import gb.smartchat.ui.create_chat.view_holder.ContactViewHolder
import gb.smartchat.ui.create_chat.view_holder.CreateGroupButtonViewHolder
import gb.smartchat.ui.create_chat.view_holder.SelectableContactViewHolder

class ContactsAdapter(
    private val createGroupClickListener: () -> Unit,
    private val contactClickListener: (Contact) -> Unit,
    private val errorActionClickListener: (tag: String) -> Unit
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactItem.CreateGroupButton -> 1
            is ContactItem.Contact -> 2
            is ContactItem.Group -> 3
            is ContactItem.Error -> 4
            is ContactItem.Loading -> 5
            is ContactItem.SelectableContact -> 6
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> CreateGroupButtonViewHolder.create(parent, createGroupClickListener)
            2 -> ContactViewHolder.create(parent, contactClickListener)
            3 -> ContactGroupViewHolder.create(parent)
            4 -> ErrorViewHolder.create(parent, errorActionClickListener)
            5 -> ProgressViewHolder.create(parent)
            6 -> SelectableContactViewHolder.create(parent, contactClickListener)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactItem.CreateGroupButton -> {
            }
            is ContactItem.Contact -> (holder as ContactViewHolder).bind(item.contact)
            is ContactItem.Group -> (holder as ContactGroupViewHolder).bind(item.group)
            is ContactItem.Error -> (holder as ErrorViewHolder).bind(
                item.message,
                item.action,
                item.tag
            )
            is ContactItem.Loading -> {
            }
            is ContactItem.SelectableContact ->
                (holder as SelectableContactViewHolder).bind(item.contact, item.isSelected)
        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            if (oldItem is ContactItem.CreateGroupButton && newItem is ContactItem.CreateGroupButton) {
                return true
            }
            if (oldItem is ContactItem.Contact && newItem is ContactItem.Contact) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is ContactItem.SelectableContact && newItem is ContactItem.SelectableContact) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is ContactItem.Group && newItem is ContactItem.Group) {
                return oldItem.group.id == newItem.group.id
            }
            if (oldItem is ContactItem.Error && newItem is ContactItem.Error) {
                return true
            }
            if (oldItem is ContactItem.Loading && newItem is ContactItem.Loading) {
                return true
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            if (oldItem is ContactItem.CreateGroupButton && newItem is ContactItem.CreateGroupButton) {
                return true
            }
            if (oldItem is ContactItem.Contact && newItem is ContactItem.Contact) {
                return oldItem.contact == newItem.contact
            }
            if (oldItem is ContactItem.SelectableContact && newItem is ContactItem.SelectableContact) {
                return oldItem == newItem
            }
            if (oldItem is ContactItem.Group && newItem is ContactItem.Group) {
                return oldItem.group == newItem.group
            }
            if (oldItem is ContactItem.Error && newItem is ContactItem.Error) {
                return oldItem == newItem
            }
            if (oldItem is ContactItem.Loading && newItem is ContactItem.Loading) {
                return true
            }
            return false
        }
    }
}
