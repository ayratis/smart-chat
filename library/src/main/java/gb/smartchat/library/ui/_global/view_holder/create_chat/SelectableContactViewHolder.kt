package gb.smartchat.library.ui._global.view_holder.create_chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.ui._global.viewbinding.ItemContactBinding

class SelectableContactViewHolder private constructor(
    private val binding: ItemContactBinding,
    private val clickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(parent: ViewGroup, clickListener: (Contact) -> Unit) =
            SelectableContactViewHolder(
                ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
            )
    }

    private lateinit var contact: Contact

    init {
        binding.root.setOnClickListener {
            clickListener.invoke(contact)
        }
    }

    fun bind(contact: Contact, isSelected: Boolean) {
        this.contact = contact
        Glide.with(binding.ivAvatar)
            .load(contact.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.tvName.text = contact.name
        binding.ivAction.setImageResource(
            if (isSelected) R.drawable.ic_radio_active_24
            else R.drawable.ic_radio_inactive_24
        )
    }
}
