package gb.smartchat.ui._global.view_holder

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatBinding
import gb.smartchat.entity.Chat
import gb.smartchat.utils.drawable
import gb.smartchat.utils.visible
import java.time.format.DateTimeFormatter

class ChatViewHolder(
    private val binding: ItemChatBinding,
    private val userId: String,
    private val clickListener: (Chat) -> Unit,
    private val pinListener: (Chat, pin: Boolean) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(
            parent: ViewGroup,
            userId: String,
            clickListener: (Chat) -> Unit,
            pinListener: (Chat, pin: Boolean) -> Unit
        ) = ChatViewHolder(
            ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            userId,
            clickListener,
            pinListener
        )
    }

    private lateinit var chat: Chat
    private val sdf = DateTimeFormatter.ofPattern("H:mm")

    private val imgIcon: Drawable by lazy {
        itemView.context.drawable(R.drawable.ic_img_14).apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }

    private val docIcon: Drawable by lazy {
        itemView.context.drawable(R.drawable.ic_doc_14).apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }

    init {
        itemView.setOnClickListener {
            clickListener.invoke(chat)
        }

        itemView.setOnLongClickListener {
            showMenu()
            true
        }
    }

    fun bind(chat: Chat) {
        this.chat = chat
        if (chat.users.size <= 2) {
            val avatar = chat.users.firstOrNull { it.id != userId }?.avatar
            Glide.with(binding.ivAvatar)
                .load(avatar)
                .placeholder(R.drawable.profile_avatar_placeholder)
                .circleCrop()
                .into(binding.ivAvatar)
        } else {
            binding.ivAvatar.setImageResource(R.drawable.group_avatar_placeholder)
        }
        val icon: Drawable? = when {
            chat.lastMessage?.file == null -> null
            chat.lastMessage.file.isImage() -> imgIcon
            else -> docIcon
        }
        binding.tvLastMsgText.setCompoundDrawables(icon, null, null, null)
        binding.tvLastMsgText.text =
            if (!chat.lastMessage?.text.isNullOrBlank()) chat.lastMessage?.text
            else chat.lastMessage?.file?.name
        binding.tvChatName.text = chat.name
        binding.tvLastMsgDate.text = chat.lastMessage?.timeCreated?.let { sdf.format(it) }
        binding.ivSendStatus.apply {
            if (chat.lastMessage?.isOutgoing(userId) == true) {
                visible(true)
                val readInfo = chat.getReadInfo(userId)
                setImageResource(
                    if (readInfo.readOut < chat.lastMessage.id) R.drawable.ic_double_check_12
                    else R.drawable.ic_double_check_colored_12
                )
            } else {
                visible(false)
                setImageDrawable(null)
            }
        }
        binding.tvLastMsgSenderName.text =
            chat.users.find { it.id == chat.lastMessage?.senderId }?.name
        binding.tvUnreadCounter.apply {
            text = if (chat.hasActualMention(userId)) "@" else chat.unreadMessagesCount?.toString()
            visible(chat.unreadMessagesCount ?: 0 > 0)
        }
        binding.tvAgentName.apply {
            text = chat.agentName
            visible(!chat.agentName.isNullOrBlank())
        }
        binding.ivPin.visible(chat.isPinned == true)
    }

    private fun showMenu() {
        val menu = android.widget.PopupMenu(itemView.context, itemView)
        menu.inflate(
            if (chat.isPinned == true) R.menu.unpin
            else R.menu.pin
        )
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_pin -> {
                    pinListener.invoke(chat, true)
                    return@setOnMenuItemClickListener true
                }
                R.id.action_unpin -> {
                    pinListener.invoke(chat, false)
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            menu.setForceShowIcon(true)
        }
        menu.show()
    }
}
