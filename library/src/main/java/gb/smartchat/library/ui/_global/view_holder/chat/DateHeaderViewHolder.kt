package gb.smartchat.library.ui._global.view_holder.chat

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import gb.smartchat.R
import gb.smartchat.library.ui._global.viewbinding.ItemChatDateHeaderBinding
import gb.smartchat.library.ui.chat.ChatItem
import gb.smartchat.library.utils.inflate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DateHeaderViewHolder private constructor(
    itemView: View,
    private val dateTimeFormatter: DateTimeFormatter
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup, dateTimeFormatter: DateTimeFormatter) =
            DateHeaderViewHolder(parent.inflate(R.layout.item_chat_date_header), dateTimeFormatter)
    }

    private val binding = ItemChatDateHeaderBinding.bind(itemView)
    private lateinit var chatItem: ChatItem.DateHeader

    fun bind(chatItem: ChatItem.DateHeader) {
        this.chatItem = chatItem
        val dayDiff = ChronoUnit.DAYS.between(chatItem.localDate, LocalDate.now())
        binding.tvDate.text = when (dayDiff) {
            0L -> itemView.context.getString(R.string.today)
            1L -> itemView.context.getString(R.string.yesterday)
            else -> dateTimeFormatter.format(chatItem.localDate)
        }
    }
}
