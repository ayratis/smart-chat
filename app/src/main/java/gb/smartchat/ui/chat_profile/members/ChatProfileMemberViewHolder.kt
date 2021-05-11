package gb.smartchat.ui.chat_profile.members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatMemberBinding
import gb.smartchat.entity.Contact
import gb.smartchat.utils.color
import gb.smartchat.utils.visible

class ChatProfileMemberViewHolder private constructor(
    private val binding: ItemChatMemberBinding,
    onClickListener: (Contact) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup, onClickListener: (Contact) -> Unit) =
            ChatProfileMemberViewHolder(
                ItemChatMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onClickListener
            )
    }

    private lateinit var contact: Contact

    init {
        binding.root.setOnClickListener {
            onClickListener.invoke(contact)
        }
    }

    fun bind(contact: Contact) {
        this.contact = contact
        Glide.with(binding.ivAvatar)
            .load(contact.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.tvCreator.visible(false) //todo
        binding.tvName.text = contact.name
        binding.tvOnline.apply {
            if (contact.online == true) {
                text = itemView.context.getString(R.string.online)
                setTextColor(itemView.context.color(R.color.purple_heart))
            } else {
                text = itemView.context.getString(R.string.offline)
                setTextColor(itemView.context.color(R.color.gray))
            }
        }
    }
}
