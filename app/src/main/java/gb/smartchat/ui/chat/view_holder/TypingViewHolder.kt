package gb.smartchat.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatTypingBinding
import gb.smartchat.entity.User
import gb.smartchat.utils.inflate

class TypingViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private const val TAG = "TypingViewHolder"

        fun create(parent: ViewGroup) =
            TypingViewHolder(parent.inflate(R.layout.item_chat_typing), /*onMessageClickListener*/)
    }

    private val binding = ItemChatTypingBinding.bind(itemView)


    fun bind(typingUsers: List<User>) {
        val usersString = typingUsers.mapNotNull { it.name }.joinToString()
        binding.root.text = if (usersString.isNotEmpty()) {
            itemView.resources.getString(R.string.typing_users_s, usersString)
        } else {
            null
        }
    }
}
