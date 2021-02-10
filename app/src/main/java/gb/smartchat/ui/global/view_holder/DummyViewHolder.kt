package gb.smartchat.ui.global.view_holder

import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import gb.smartchat.R
import gb.smartchat.databinding.ItemDummyBinding
import gb.smartchat.ui.chat.ChatItem
import gb.smartchat.utils.inflate

class DummyViewHolder private constructor(
    itemView: View,
    onLongClickListener: (ChatItem) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

    private val binding by viewBinding(ItemDummyBinding::bind)
    private lateinit var chatItem: ChatItem

    init {
        binding.root.setOnCreateContextMenuListener(this)
//        binding.root.setOnLongClickListener {
//            onLongClickListener.invoke(chatItem)
//            true
//        }
    }

    fun bind(chatItem: ChatItem) {
        this.chatItem = chatItem
        binding.tvContent.text = when (chatItem) {
            is ChatItem.Outgoing -> {
               val statusString = when (chatItem.status) {
                    ChatItem.OutgoingStatus.SENDING -> "`"
                    ChatItem.OutgoingStatus.SENT -> "+"
                    ChatItem.OutgoingStatus.SENT_2 -> "++"
                    ChatItem.OutgoingStatus.READ -> "r"
                    ChatItem.OutgoingStatus.FAILURE -> "e"
                    ChatItem.OutgoingStatus.EDITING -> "editing"
                    ChatItem.OutgoingStatus.DELETING -> "deleting"
                }
                "${chatItem.message.text} ($statusString)"
            }
            is ChatItem.Incoming -> chatItem.message.text ?: "`null`"
            is ChatItem.System -> chatItem.message.text ?: "`null`"
        }
    }

    companion object {
        fun create(parent: ViewGroup, onLongClickListener: (ChatItem) -> Unit) =
            DummyViewHolder(parent.inflate(R.layout.item_dummy), onLongClickListener)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        TODO("Not yet implemented")
    }
}