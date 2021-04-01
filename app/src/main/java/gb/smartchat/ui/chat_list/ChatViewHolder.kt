package gb.smartchat.ui.chat_list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatBinding
import gb.smartchat.entity.Chat
import gb.smartchat.utils.inflate
import gb.smartchat.utils.visible
import java.time.format.DateTimeFormatter

class ChatViewHolder(
    itemView: View,
    private val userId: String,
    private val clickListener: (Chat) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(
            parent: ViewGroup,
            userId: String,
            clickListener: (Chat) -> Unit
        ) = ChatViewHolder(parent.inflate(R.layout.item_chat), userId, clickListener)
    }

    private val binding: ItemChatBinding = ItemChatBinding.bind(itemView)
    private lateinit var chat: Chat
    private val sdf = DateTimeFormatter.ofPattern("H:mm")

    init {
        itemView.setOnClickListener {
            clickListener.invoke(chat)
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
        binding.tvLastMsgText.text = chat.lastMessage?.text
        binding.tvUnreadCounter.apply {
            text = chat.unreadMessagesCount?.toString()
            visible(chat.unreadMessagesCount ?: 0 > 0)
        }
        binding.tvAgentName.apply {
            text = chat.agentName
            visible(!chat.agentName.isNullOrBlank())
        }
    }
}
