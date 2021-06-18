package gb.smartchat.library.ui._global.view_holder

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.ui._global.viewbinding.ItemChatBinding
import gb.smartchat.library.utils.drawable
import gb.smartchat.library.utils.visible
import java.time.format.DateTimeFormatter

class ChatViewHolder(
    private val binding: ItemChatBinding,
    private val userId: String,
    private val isArchive: Boolean,
    private val clickListener: (Chat) -> Unit,
    private val pinListener: ((Chat, pin: Boolean) -> Unit)?,
    private val archiveListener: ((Chat, archive: Boolean) -> Unit)?
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun create(
            parent: ViewGroup,
            userId: String,
            isArchive: Boolean,
            clickListener: (Chat) -> Unit,
            pinListener: ((Chat, pin: Boolean) -> Unit)?,
            archiveListener: ((Chat, archive: Boolean) -> Unit)?
        ) = ChatViewHolder(
            ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            userId,
            isArchive,
            clickListener,
            pinListener,
            archiveListener
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
        Glide.with(binding.ivAvatar)
            .load(chat.avatar)
            .placeholder(R.drawable.profile_avatar_placeholder)
            .circleCrop()
            .into(binding.ivAvatar)
        
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
        if (pinListener != null) {
            menu.inflate(
                if (chat.isPinned == true) R.menu.unpin
                else R.menu.pin
            )
        }
        if (archiveListener != null) {
            menu.inflate(
                if (isArchive) R.menu.unarhcive
                else R.menu.arhcive
            )
        }
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_pin -> {
                    pinListener?.invoke(chat, true)
                    true
                }
                R.id.action_unpin -> {
                    pinListener?.invoke(chat, false)
                    true
                }
                R.id.action_archive -> {
                    archiveListener?.invoke(chat, true)
                    true
                }
                R.id.action_unarchive -> {
                    archiveListener?.invoke(chat, false)
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
