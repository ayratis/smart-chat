package gb.smartchat.library.ui.group_complete

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.library.entity.Contact

class ContactsAdapter(
    private val onDeleteContactListener: (Contact) -> Unit
) : ListAdapter<Contact, DeletableContactViewHolder>(DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletableContactViewHolder {
        return DeletableContactViewHolder.create(parent, onDeleteContactListener)
    }

    override fun onBindViewHolder(holder: DeletableContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffUtilItemCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
