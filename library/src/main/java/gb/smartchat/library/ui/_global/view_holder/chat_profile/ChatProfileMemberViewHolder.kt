package gb.smartchat.library.ui._global.view_holder.chat_profile

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.User
import gb.smartchat.library.ui._global.viewbinding.ItemChatProfileMemberBinding
import gb.smartchat.library.utils.color
import gb.smartchat.library.utils.visible

class ChatProfileMemberViewHolder private constructor(
    private val binding: ItemChatProfileMemberBinding,
    onClickListener: (Contact) -> Unit,
    private val deleteContactListener: ((Contact) -> Unit)?
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(
            parent: ViewGroup,
            onClickListener: (Contact) -> Unit,
            deleteContactListener: ((Contact) -> Unit)?
        ) =
            ChatProfileMemberViewHolder(
                ItemChatProfileMemberBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onClickListener,
                deleteContactListener
            )
    }

    private lateinit var contact: Contact

    init {
        binding.root.setOnClickListener {
            onClickListener.invoke(contact)
        }
        if (deleteContactListener != null) {
            binding.root.setOnLongClickListener {
                showMenu()
                true
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
        binding.tvCreator.visible(contact.role == User.Role.CREATOR)
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

    private fun showMenu() {
        if (contact.role == User.Role.CREATOR) return
        val menu = android.widget.PopupMenu(itemView.context, itemView)
        if (deleteContactListener != null) {
            menu.inflate(R.menu.delete_member)
        }
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete_member -> {
                    deleteContactListener?.invoke(contact)
                    true
                }
                else -> false
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            menu.gravity = Gravity.END
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            menu.setForceShowIcon(true)
        }
        menu.show()
    }
}
