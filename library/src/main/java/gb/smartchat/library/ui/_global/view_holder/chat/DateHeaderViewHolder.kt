package gb.smartchat.library.ui._global.view_holder.chat

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.library.ui._global.viewbinding.ItemChatDateHeaderBinding
import gb.smartchat.library.ui.chat.ChatItem
import gb.smartchat.library.utils.inflate
import java.time.format.DateTimeFormatter
import java.util.*

class DateHeaderViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup) =
            DateHeaderViewHolder(parent.inflate(R.layout.item_chat_date_header))
    }

    private val binding = ItemChatDateHeaderBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.DateHeader
    private val sdf = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

    fun bind(chatItem: ChatItem.DateHeader) {
        this.chatItem = chatItem
        binding.tvDate.text = sdf.format(chatItem.localDate)
    }
}
