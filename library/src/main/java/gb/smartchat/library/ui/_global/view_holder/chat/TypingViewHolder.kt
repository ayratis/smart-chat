package gb.smartchat.library.ui._global.view_holder.chat

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.library.entity.User
import gb.smartchat.library.ui._global.viewbinding.ItemChatTypingBinding
import gb.smartchat.library.utils.inflate

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
