package gb.smartchat.ui.group_complete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.ItemContactBinding
import gb.smartchat.entity.Contact

class DeletableContactViewHolder private constructor(
    private val binding: ItemContactBinding,
    private val deleteClickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(parent: ViewGroup, deleteClickListener: (Contact) -> Unit) =
            DeletableContactViewHolder(
                ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                deleteClickListener
            )
    }

    private lateinit var contact: Contact

    init {
        binding.ivAction.apply {
            setImageResource(R.drawable.ic_close_red_24)
            contentDescription = itemView.context.getString(R.string.delete)
            setOnClickListener {
                deleteClickListener.invoke(contact)
            }
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
