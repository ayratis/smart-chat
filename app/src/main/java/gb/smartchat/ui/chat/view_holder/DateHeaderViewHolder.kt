package gb.smartchat.ui.chat.view_holder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.databinding.ItemChatDateHeaderBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate
import java.time.format.DateTimeFormatter

class DateHeaderViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup) =
            DateHeaderViewHolder(parent.inflate(R.layout.item_chat_date_header))
    }

    private val binding = ItemChatDateHeaderBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.DateHeader
    private val sdf = DateTimeFormatter.ofPattern("d MMMM yyyy")

    fun bind(chatItem: ChatItem.DateHeader) {
        this.chatItem = chatItem
        binding.root.text = sdf.format(chatItem.localDate)
    }
}