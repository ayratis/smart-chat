package gb.smartchat.ui.create_chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.ItemContactBinding
import gb.smartchat.entity.Contact

class ContactViewHolder private constructor(
    private val binding: ItemContactBinding,
    private val clickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(parent: ViewGroup, clickListener: (Contact) -> Unit) =
            ContactViewHolder(
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

    fun bind(contact: Contact) {
        this.contact = contact
        Glide.with(binding.ivAvatar)
            .load(contact.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.tvName.text = contact.name
    }
}
