package gb.smartchat.ui.create_chat

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.entity.Contact

class ContactsAdapter(
    private val contactClickListener: (Contact) -> Unit
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactItem.Contact -> 1
            is ContactItem.Group -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ContactViewHolder.create(parent, contactClickListener)
            2 -> ContactGroupViewHolder.create(parent)
            else -> throw RuntimeException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactItem.Contact -> (holder as ContactViewHolder).bind(item.contact)
            is ContactItem.Group -> (holder as ContactGroupViewHolder).bind(item.group)
        }
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            if (oldItem is ContactItem.Contact && newItem is ContactItem.Contact) {
                return oldItem.contact.id == newItem.contact.id
            }
            if (oldItem is ContactItem.Group && newItem is ContactItem.Group) {
                return oldItem.group.id == newItem.group.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            if (oldItem is ContactItem.Contact && newItem is ContactItem.Contact) {
                return oldItem.contact == newItem.contact
            }
            if (oldItem is ContactItem.Group && newItem is ContactItem.Group) {
                return oldItem.group == newItem.group
            }
            return false
        }
    }
}
