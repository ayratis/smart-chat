package gb.smartchat.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import gb.smartchat.ui.global.view_holder.DummyViewHolder

class ChatAdapter(
    private val onItemBindListener: (ChatItem) -> Unit,
    private val onLongClickListener: (ChatItem) -> Unit
) : ListAdapter<ChatItem, DummyViewHolder>(ChatItem.DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DummyViewHolder {
        return DummyViewHolder.create(parent, onLongClickListener)
    }

    override fun onBindViewHolder(holder: DummyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        onItemBindListener.invoke(item)
    }

}